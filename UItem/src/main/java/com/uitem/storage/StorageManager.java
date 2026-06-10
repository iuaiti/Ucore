package com.uitem.storage;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Quản lý đọc/ghi item config.
 *
 * <h3>Cấu trúc thư mục được hỗ trợ:</h3>
 * <pre>
 * plugins/UItem/
 * ├── items.yml              ← file đơn (backward-compat)
 * └── items/                 ← thư mục ưu tiên hơn nếu tồn tại
 *     ├── weapons.yml
 *     ├── armor.yml
 *     └── boss/
 *         └── mythic.yml     ← lồng thư mục tùy ý
 * </pre>
 *
 * <p><b>Quy tắc:</b>
 * <ul>
 *   <li>Nếu thư mục {@code items/} tồn tại → quét TẤT CẢ file .yml bên trong (đệ quy).</li>
 *   <li>Nếu không → đọc {@code items.yml} như cũ.</li>
 *   <li>Reload đọc lại từ disk, không dùng cache.</li>
 *   <li>Save từ GUI/lệnh chỉ ghi vào {@code items.yml} (hoặc file gốc của item đó nếu được track).</li>
 *   <li>Không ghi đè file người dùng khi reload.</li>
 *   <li>Mọi lỗi config đều được log rõ ràng — không bao giờ im lặng.</li>
 * </ul>
 */
public class StorageManager {

    // ── Tên file / thư mục ───────────────────────────────────────────────────
    private static final String LEGACY_FILE   = "items.yml";
    private static final String ITEMS_DIR     = "items";
    private static final String ITEMS_SECTION = "items";

    // ── Async-save constants ──────────────────────────────────────────────────
    private static final long DEBOUNCE_MS = 2000;

    private final UItemPlugin plugin;

    /** File chính để GUI save vào (trong items/item.yml - external folder, user có thể edit thủ công). */
    private final File itemsFile;

    /** Legacy items.yml in root for fallback load. */
    private final File legacyFile;

    /** Thư mục items/ (có thể chưa tồn tại). */
    private final File itemsDir;

    // ── Async-save state ─────────────────────────────────────────────────────
    private final Object           fileLock              = new Object();
    private final AtomicBoolean    asyncWorkerRunning    = new AtomicBoolean(false);
    private final AtomicLong       requestCounter        = new AtomicLong();
    private final AtomicLong       minAllowedWriteId     = new AtomicLong();
    private final AtomicReference<PendingSave> pendingRef = new AtomicReference<>();
    private final AtomicLong       lastDebounceSchedule  = new AtomicLong(0);

    public StorageManager(UItemPlugin plugin) {
        this.plugin    = plugin;
        this.legacyFile = new File(plugin.getDataFolder(), LEGACY_FILE);
        this.itemsDir   = new File(plugin.getDataFolder(), ITEMS_DIR);
        this.itemsDir.mkdirs();
        this.itemsFile = new File(this.itemsDir, "item.yml");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — LOAD
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Load tất cả items từ disk vào manager.
     * Ưu tiên thư mục {@code items/}; fallback về {@code items.yml}.
     * Đọc lại từ disk mỗi lần — không dùng cache.
     */
    public void loadAll(UItemManager manager) {
        // Make on-disk files (including split items/*.yml) the source of truth.
        // This fixes "GUI edits disappear after /uitem reload" — previous in-memory only items or stale deleted items are cleared.
        manager.clearAll();

        // Huỷ pending-save cũ để tránh ghi đè lên dữ liệu sắp load
        minAllowedWriteId.set(requestCounter.incrementAndGet());
        pendingRef.set(null);

        if (itemsDir.exists() && itemsDir.isDirectory()) {
            loadFromDirectory(manager);
        } else {
            loadFromLegacyFile(manager);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Load: thư mục items/
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFromDirectory(UItemManager manager) {
        List<File> yamlFiles = collectYamlFiles(itemsDir);
        if (yamlFiles.isEmpty()) {
            plugin.getLogger().info("[UItem] items/ folder is empty — no items loaded.");
            return;
        }

        plugin.getLogger().info("[UItem] Loading items from " + yamlFiles.size()
                + " file(s) in items/ ...");

        int totalLoaded = 0;
        int totalSkipped = 0;

        for (File file : yamlFiles) {
            String label = relativeToDataFolder(file); // ví dụ: "items/weapons.yml"
            FileLoadResult r = loadSingleYamlFile(file, label, manager);
            totalLoaded  += r.loaded();
            totalSkipped += r.skipped();
        }

        plugin.getLogger().info(String.format(
                "[UItem] Done — %d item(s) loaded, %d skipped.", totalLoaded, totalSkipped));
    }

    /** Thu thập đệ quy tất cả .yml trong thư mục. */
    private List<File> collectYamlFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) return result;
        Arrays.sort(children, Comparator.comparing(File::getName)); // thứ tự ổn định
        for (File f : children) {
            if (f.isDirectory()) {
                result.addAll(collectYamlFiles(f));
            } else if (f.isFile() && f.getName().endsWith(".yml")) {
                result.add(f);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Load: items.yml (legacy / fallback)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFromLegacyFile(UItemManager manager) {
        if (!legacyFile.exists()) {
            createDefaultLegacyFile();
        }
        FileLoadResult r = loadSingleYamlFile(legacyFile, LEGACY_FILE, manager);
        plugin.getLogger().info(String.format(
                "[UItem] %s loaded: %d item(s) OK, %d skipped.",
                LEGACY_FILE, r.loaded(), r.skipped()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Đọc một file .yml
    // ─────────────────────────────────────────────────────────────────────────

    private FileLoadResult loadSingleYamlFile(File file, String label, UItemManager manager) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException e) {
            logError("[%s] Cannot read file: %s", label, e.getMessage());
            return FileLoadResult.EMPTY;
        } catch (InvalidConfigurationException e) {
            logError("[%s] YAML syntax error — %s", label, yamlErrorDetail(e));
            logError("[%s] Fix the error and run /uitem reload.", label);
            return FileLoadResult.EMPTY;
        }

        // Support both:
        //  - Wrapped: items: { id: {...} }   (standard + legacy)
        //  - Flat (copy-paste friendly for external configs): id: { material: ..., ... } directly at root of the yml file
        // This makes it easy for users to drop single-item configs copied from elsewhere into items/ folder.
        ConfigurationSection itemsSection = null;
        boolean flatMode = false;

        if (config.isConfigurationSection(ITEMS_SECTION)) {
            itemsSection = config.getConfigurationSection(ITEMS_SECTION);
        } else {
            // Flat mode: scan root keys, only treat keys that look like item definitions (have 'material')
            itemsSection = config;
            flatMode = true;
        }

        if (itemsSection == null) {
            plugin.getLogger().warning("[UItem] [" + label + "] No item definitions found (neither '" + ITEMS_SECTION + "' section nor flat item entries).");
            return FileLoadResult.EMPTY;
        }

        int loaded = 0, skipped = 0;
        for (String id : itemsSection.getKeys(false)) {
            // In flat mode, skip keys that are clearly not items (e.g. comments or other top level)
            if (flatMode) {
                ConfigurationSection candidate = itemsSection.getConfigurationSection(id);
                if (candidate == null || !candidate.contains("material")) {
                    continue; // not an item definition
                }
            }

            LoadResult r = parseItem(id, itemsSection, label);
            if (r.data() != null) {
                manager.registerItem(r.data());
                loaded++;
            } else {
                skipped++;
            }
        }
        return new FileLoadResult(loaded, skipped);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Parse + validate từng item
    // ═════════════════════════════════════════════════════════════════════════

    private LoadResult parseItem(String id, ConfigurationSection itemsSection, String fileName) {

        // ── ID không rỗng ─────────────────────────────────────────────────────
        if (id == null || id.isBlank()) {
            logError("[%s] Blank item key — skipped.", fileName);
            return LoadResult.SKIP;
        }

        // ── Section phải là mapping ───────────────────────────────────────────
        if (!itemsSection.isConfigurationSection(id)) {
            logError("[%s] Invalid item '%s': not a YAML mapping (check indentation).", fileName, id);
            return LoadResult.SKIP;
        }

        ConfigurationSection s = itemsSection.getConfigurationSection(id);
        if (s == null) {
            logError("[%s] Invalid item '%s': null section.", fileName, id);
            return LoadResult.SKIP;
        }

        UItemData data = new UItemData(id);
        boolean hasWarning = false;

        // ── material ──────────────────────────────────────────────────────────
        String matStr = s.getString("material");
        if (matStr == null || matStr.isBlank()) {
            logError("[%s] Invalid item '%s' in items.yml: missing 'material' field.", fileName, id);
            data.setMaterial(Material.STONE);
            hasWarning = true;
        } else {
            Material mat = parseMaterial(matStr);
            if (mat == null) {
                logError("[%s] Invalid item '%s' in items.yml: material '%s' not found.", fileName, id, matStr);
                return LoadResult.SKIP;
            }
            if (!mat.isItem()) {
                logError("[%s] Invalid item '%s' in items.yml: material '%s' is not an item (e.g. AIR, WATER).",
                        fileName, id, matStr);
                return LoadResult.SKIP;
            }
            data.setMaterial(mat);
        }

        // ── display-name ──────────────────────────────────────────────────────
        data.setDisplayName(s.getString("display-name", "<white>" + id));

        // ── lore ──────────────────────────────────────────────────────────────
        data.setLore(s.getStringList("lore"));

        // ── enchantments ──────────────────────────────────────────────────────
        ConfigurationSection enchSection = s.getConfigurationSection("enchantments");
        if (enchSection != null) {
            Map<String, Integer> enchants = new LinkedHashMap<>();
            for (String enchKey : enchSection.getKeys(false)) {
                if (!enchSection.isInt(enchKey) && !enchSection.isLong(enchKey)) {
                    logError("[%s] Item '%s': enchantment '%s' has non-integer level — skipped.",
                            fileName, id, enchKey);
                    hasWarning = true;
                    continue;
                }
                int lvl = enchSection.getInt(enchKey);
                if (lvl < 1) {
                    logError("[%s] Item '%s': enchantment '%s' level must be >= 1 (got %d) — skipped.",
                            fileName, id, enchKey, lvl);
                    hasWarning = true;
                    continue;
                }
                enchants.put(enchKey.toLowerCase(Locale.ROOT), lvl);
            }
            data.setEnchantments(enchants);
        }

        // ── flags ─────────────────────────────────────────────────────────────
        Set<String> validFlags = new LinkedHashSet<>();
        for (String flagName : s.getStringList("flags")) {
            try {
                org.bukkit.inventory.ItemFlag.valueOf(flagName.toUpperCase(Locale.ROOT));
                validFlags.add(flagName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logError("[%s] Item '%s': unknown ItemFlag '%s' — skipped.", fileName, id, flagName);
                hasWarning = true;
            }
        }
        data.setFlags(validFlags);

        // ── custom-model-data ─────────────────────────────────────────────────
        if (s.contains("custom-model-data") && !s.isInt("custom-model-data")) {
            logError("[%s] Item '%s': 'custom-model-data' must be an integer — defaulting to -1.", fileName, id);
            hasWarning = true;
            data.setCustomModelData(-1);
        } else {
            data.setCustomModelData(s.getInt("custom-model-data", -1));
        }

        // ── unbreakable / glowing ─────────────────────────────────────────────
        data.setUnbreakable(s.getBoolean("unbreakable", false));
        data.setGlowing(s.getBoolean("glowing", false));

        // ── max-durability ────────────────────────────────────────────────────
        if (s.contains("max-durability") && !s.isInt("max-durability")) {
            logError("[%s] Item '%s': 'max-durability' must be an integer — defaulting to -1.", fileName, id);
            hasWarning = true;
            data.setMaxDurability(-1);
        } else {
            data.setMaxDurability(s.getInt("max-durability", -1));
        }

        // ── attributes ────────────────────────────────────────────────────────
        ConfigurationSection attrSection = s.getConfigurationSection("attributes");
        if (attrSection != null) {
            List<UItemData.AttributeData> attrs = new ArrayList<>();
            for (String attrKey : attrSection.getKeys(false)) {
                ConfigurationSection a = attrSection.getConfigurationSection(attrKey);
                if (a == null) {
                    logError("[%s] Item '%s': attribute '%s' is not a mapping — skipped.", fileName, id, attrKey);
                    hasWarning = true;
                    continue;
                }
                String attrName = a.getString("attribute");
                if (attrName == null || attrName.isBlank()) {
                    logError("[%s] Item '%s': attribute '%s' missing 'attribute' field — skipped.",
                            fileName, id, attrKey);
                    hasWarning = true;
                    continue;
                }
                String op = a.getString("operation", "ADD_NUMBER");
                if (!isValidOperation(op)) {
                    logError("[%s] Item '%s': attribute '%s' invalid operation '%s' " +
                                    "(valid: ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1) — skipped.",
                            fileName, id, attrKey, op);
                    hasWarning = true;
                    continue;
                }
                attrs.add(new UItemData.AttributeData(attrName, a.getDouble("amount", 0),
                        op, a.getString("slot", "ANY")));
            }
            data.setAttributes(attrs);
        }

        // ── rarity ────────────────────────────────────────────────────────────
        String rarity = s.getString("rarity", null);
        if (rarity != null && !rarity.isBlank()) {
            String up = rarity.toUpperCase(Locale.ROOT);
            if (!RarityConfig.getKeys().contains(up)) {
                logError("[%s] Item '%s': unknown rarity '%s' (known: %s) — rarity cleared.",
                        fileName, id, rarity, String.join(", ", RarityConfig.getKeys()));
                hasWarning = true;
            } else {
                data.setRarity(up);
            }
        }

        // ── integration IDs ───────────────────────────────────────────────────
        data.setOraxenId(s.getString("oraxen-id", null));
        data.setItemsAdderId(s.getString("itemsadder-id", null));
        data.setExecutableItemsId(s.getString("executableitems-id", null));

        // ── model (custom model for resource pack, Oraxen/ItemsAdder style) ──
        String modelRef = s.getString("model");
        if (modelRef != null && !modelRef.isBlank()) {
            data.setModel(modelRef);
        }

        // effects: general effect system (HIT, DAMAGE, DIE, EQUIP, FULL_SET etc) - REFLECT/EXTRA_DAMAGE only
        ConfigurationSection effectsSec = s.getConfigurationSection("effects");
        if (effectsSec != null) {
            List<UItemData.ItemEffect> effs = new ArrayList<>();
            for (String k : effectsSec.getKeys(false)) {
                ConfigurationSection es = effectsSec.getConfigurationSection(k);
                if (es == null) continue;
                UItemData.ItemEffect eff = new UItemData.ItemEffect();
                eff.setTrigger(es.getString("trigger", "HIT"));
                eff.setType(es.getString("type", "REFLECT"));
                eff.setChance(es.getDouble("chance", 100.0));
                eff.setValue(es.getDouble("value", 0.0));
                eff.setApplyToTarget(es.getBoolean("apply-to-target", false));
                effs.add(eff);
            }
            data.setEffects(effs);
        }

        // ── categories ────────────────────────────────────────────────────────
        List<String> cats = s.getStringList("categories");
        if (cats != null && !cats.isEmpty()) {
            data.setCategories(new HashSet<>(cats));
        }

        // ── required-skill-node ──────────────────────────────────────────────
        String skillNode = s.getString("required-skill-node");
        if (skillNode != null && !skillNode.isBlank()) {
            data.setRequiredSkillNode(skillNode);
            data.setRequiredSkillLevel(s.getInt("required-skill-level", 1));
        }

        // skills: stored raw — parsed by USkillAPI when needed
        ConfigurationSection skillsSec = s.getConfigurationSection("skills");
        if (skillsSec != null) {
            data.setSkillsRaw(sectionToMap(skillsSec));
        }

        if (hasWarning) {
            plugin.getLogger().warning(
                    "[UItem] Item '" + id + "' loaded with warnings (see [ERROR] lines above).");
        }

        return new LoadResult(data);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — SAVE  (GUI / lệnh → luôn ghi vào items.yml)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Save async, gom nhiều lần gọi thành snapshot mới nhất.
     * Luôn ghi vào {@code items.yml} — không đụng file người dùng trong {@code items/}.
     */
    public void saveAll(UItemManager manager) {
        long id = requestCounter.incrementAndGet();
        pendingRef.set(new PendingSave(manager.snapshotAll(), id));
        if (asyncWorkerRunning.compareAndSet(false, true)) {
            SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
        }
    }

    /** Debounced save — dành cho UI-driven changes để tránh I/O spam. */
    public void saveAllDebounced(UItemManager manager) {
        long now = System.currentTimeMillis();
        lastDebounceSchedule.set(now);
        saveAll(manager);
        SchedulerUtil.runDelayedAsync(plugin, () -> {
            if (lastDebounceSchedule.get() == now) {
                if (pendingRef.get() != null && asyncWorkerRunning.compareAndSet(false, true)) {
                    SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
                }
            }
        }, DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /** Ghi ngay lập tức — dùng trước khi reload hoặc khi disable. */
    public void saveAllNow(UItemManager manager) {
        long syncId = requestCounter.incrementAndGet();
        minAllowedWriteId.set(syncId);
        pendingRef.set(null);
        writeSnapshot(manager.snapshotAll(), syncId);
    }

    public void saveItem(UItemManager manager, UItemData data) {
        // Preferred: save as individual file for easy copy-paste and separate management
        saveItemIndividual(data);
        // Also keep in-memory
        manager.registerItem(data);
    }

    /**
     * Delete the on-disk file(s) for an item id.
     * Supports the default save location (items/&lt;id&gt;.yml) and any split files under items/ (including subdirs).
     * Called on removeItem so that deleted items do not re-appear after /reload when using split files.
     */
    public void deleteItemFile(String id) {
        if (id == null || id.isBlank()) return;
        // Primary location used by saveItemIndividual
        File primary = new File(itemsDir, id + ".yml");
        if (primary.exists() && primary.delete()) {
            plugin.getLogger().info("[UItem] Deleted item file for '" + id + "': " + primary.getName());
        }
        // Also clean any copies that may exist in subfolders (user-organized split files)
        deleteItemFilesRecursive(itemsDir, id);
    }

    private void deleteItemFilesRecursive(File dir, String targetId) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                deleteItemFilesRecursive(f, targetId);
            } else if (f.isFile() && f.getName().endsWith(".yml")) {
                // Peek to see if this file defines the target item id (flat or under "items:")
                try {
                    YamlConfiguration peek = new YamlConfiguration();
                    peek.load(f);
                    ConfigurationSection root = peek.isConfigurationSection(ITEMS_SECTION)
                            ? peek.getConfigurationSection(ITEMS_SECTION)
                            : peek;
                    if (root != null && root.isConfigurationSection(targetId)) {
                        ConfigurationSection itemSec = root.getConfigurationSection(targetId);
                        if (itemSec != null && itemSec.contains("material")) {
                            if (f.delete()) {
                                plugin.getLogger().info("[UItem] Deleted split item file for '" + targetId + "': " + relativeToDataFolder(f));
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // ignore bad yml during delete scan
                }
            }
        }
    }

    /**
     * Save a single item as its own file: items/<id>.yml (flat style, copy-paste friendly).
     * This avoids constantly recreating/overwriting a big item.yml.
     */
    public void saveItemIndividual(UItemData d) {
        if (d == null) return;
        String id = d.getId();
        File target = new File(itemsDir, id + ".yml");

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.options().setHeader(List.of(
            "Single UItem definition (copy-paste friendly).",
            "You can move this file anywhere inside the items/ folder (including subfolders).",
            "Flat style is supported."
        ));

        Material mat = d.getMaterial() != null ? d.getMaterial() : Material.STONE;
        cfg.set(id + ".material", mat.name());
        if (d.getDisplayName() != null) cfg.set(id + ".display-name", d.getDisplayName());
        if (d.getLore() != null && !d.getLore().isEmpty()) cfg.set(id + ".lore", d.getLore());

        if (d.getEnchantments() != null && !d.getEnchantments().isEmpty()) {
            d.getEnchantments().forEach((k, v) -> cfg.set(id + ".enchantments." + k, v));
        }
        cfg.set(id + ".flags", d.getFlags() != null ? new ArrayList<>(d.getFlags()) : List.of());
        cfg.set(id + ".custom-model-data", d.getCustomModelData());
        cfg.set(id + ".unbreakable", d.isUnbreakable());
        cfg.set(id + ".glowing", d.isGlowing());
        cfg.set(id + ".max-durability", d.getMaxDurability());

        if (d.getAttributes() != null && !d.getAttributes().isEmpty()) {
            for (int i = 0; i < d.getAttributes().size(); i++) {
                UItemData.AttributeData a = d.getAttributes().get(i);
                String ap = id + ".attributes.attr_" + i;
                cfg.set(ap + ".attribute", a.getAttribute());
                cfg.set(ap + ".amount", a.getAmount());
                cfg.set(ap + ".operation", a.getOperation());
                cfg.set(ap + ".slot", a.getSlot());
            }
        }

        // Skills — stored/loaded as raw data (no USkill model dependency)
        if (d.getSkillsRaw() != null && !d.getSkillsRaw().isEmpty()) {
            cfg.set(id + ".skills", d.getSkillsRaw());
        }

        // Categories
        if (d.getCategories() != null && !d.getCategories().isEmpty()) {
            cfg.set(id + ".categories", new ArrayList<>(d.getCategories()));
        }

        // Skill requirement
        if (d.getRequiredSkillNode() != null) {
            cfg.set(id + ".required-skill-node", d.getRequiredSkillNode());
            cfg.set(id + ".required-skill-level", d.getRequiredSkillLevel());
        }

        // Effects, integrations, model, rarity...
        if (d.getRarity() != null) cfg.set(id + ".rarity", d.getRarity());
        if (d.getModel() != null) cfg.set(id + ".model", d.getModel());
        if (d.getOraxenId() != null) cfg.set(id + ".oraxen-id", d.getOraxenId());
        if (d.getItemsAdderId() != null) cfg.set(id + ".itemsadder-id", d.getItemsAdderId());
        if (d.getExecutableItemsId() != null) cfg.set(id + ".executableitems-id", d.getExecutableItemsId());

        // Effects
        if (d.getEffects() != null && !d.getEffects().isEmpty()) {
            for (int i = 0; i < d.getEffects().size(); i++) {
                UItemData.ItemEffect e = d.getEffects().get(i);
                String ep = id + ".effects.e" + i;
                if (e.getTrigger() != null) cfg.set(ep + ".trigger", e.getTrigger());
                if (e.getType() != null) cfg.set(ep + ".type", e.getType());
                cfg.set(ep + ".chance", e.getChance());
                if (e.getValue() != 0) cfg.set(ep + ".value", e.getValue());
                if (e.isApplyToTarget()) cfg.set(ep + ".apply-to-target", true);
            }
        }

        synchronized (fileLock) {
            try {
                cfg.save(target);
            } catch (IOException e) {
                plugin.getLogger().warning("[UItem] Failed to save individual item file " + target.getName() + ": " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal flush / write
    // ─────────────────────────────────────────────────────────────────────────

    private void flushPendingSaves() {
        try {
            while (true) {
                PendingSave p = pendingRef.get();
                if (p == null) break;
                if (!pendingRef.compareAndSet(p, null)) continue;
                writeSnapshot(p.snapshot(), p.requestId());
            }
        } finally {
            asyncWorkerRunning.set(false);
            if (pendingRef.get() != null && asyncWorkerRunning.compareAndSet(false, true)) {
                SchedulerUtil.runAsync(plugin, this::flushPendingSaves);
            }
        }
    }

    private void writeSnapshot(Map<String, UItemData> snapshot, long writeId) {
        if (snapshot == null || writeId < minAllowedWriteId.get()) return;

        boolean useConsolidated = plugin.getConfig().getBoolean("storage.use-consolidated-files", false);
        if (!useConsolidated) {
            // User prefers individual files only (items/<id>.yml)
            // Still write individuals for safety
            for (UItemData d : snapshot.values()) {
                saveItemIndividual(d);
            }
            return;
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, UItemData> entry : snapshot.entrySet()) {
            String id   = entry.getKey();
            UItemData d = entry.getValue();
            String path = ITEMS_SECTION + "." + id;

            Material mat = d.getMaterial() != null ? d.getMaterial() : Material.STONE;
            config.set(path + ".material", mat.name());
            config.set(path + ".display-name", d.getDisplayName());
            config.set(path + ".lore", d.getLore());

            if (d.getEnchantments() != null && !d.getEnchantments().isEmpty()) {
                d.getEnchantments().forEach((k, v) ->
                        config.set(path + ".enchantments." + k, v));
            }

            config.set(path + ".flags",
                    d.getFlags() != null ? new ArrayList<>(d.getFlags()) : List.of());
            config.set(path + ".custom-model-data", d.getCustomModelData());
            config.set(path + ".unbreakable", d.isUnbreakable());
            config.set(path + ".glowing", d.isGlowing());
            config.set(path + ".max-durability", d.getMaxDurability());

            if (d.getAttributes() != null && !d.getAttributes().isEmpty()) {
                for (int i = 0; i < d.getAttributes().size(); i++) {
                    UItemData.AttributeData a = d.getAttributes().get(i);
                    String ap = path + ".attributes.attr_" + i;
                    config.set(ap + ".attribute", a.getAttribute());
                    config.set(ap + ".amount",    a.getAmount());
                    config.set(ap + ".operation", a.getOperation());
                    config.set(ap + ".slot",      a.getSlot());
                }
            }

            if (d.getOraxenId()          != null) config.set(path + ".oraxen-id",           d.getOraxenId());
            if (d.getItemsAdderId()       != null) config.set(path + ".itemsadder-id",        d.getItemsAdderId());
            if (d.getExecutableItemsId()  != null) config.set(path + ".executableitems-id",   d.getExecutableItemsId());
            if (d.getRarity()             != null) config.set(path + ".rarity",               d.getRarity());
            if (d.getModel()              != null) config.set(path + ".model",                d.getModel());

            // Effects (attachable reflect/extra damage + actions)
            if (d.getEffects() != null && !d.getEffects().isEmpty()) {
                for (int i = 0; i < d.getEffects().size(); i++) {
                    UItemData.ItemEffect e = d.getEffects().get(i);
                    String ep = path + ".effects.e" + i;
                    if (e.getTrigger() != null) config.set(ep + ".trigger", e.getTrigger());
                    if (e.getType() != null) config.set(ep + ".type", e.getType());
                    config.set(ep + ".chance", e.getChance());
                    if (e.getValue() != 0) config.set(ep + ".value", e.getValue());
                    if (e.isApplyToTarget()) config.set(ep + ".apply-to-target", true);
                }
            }

            // Categories
            if (d.getCategories() != null && !d.getCategories().isEmpty()) {
                config.set(path + ".categories", new ArrayList<>(d.getCategories()));
            }

            // Skills — stored/loaded as raw data
            if (d.getSkillsRaw() != null && !d.getSkillsRaw().isEmpty()) {
                config.set(path + ".skills", d.getSkillsRaw());
            }
        }

        synchronized (fileLock) {
            if (writeId < minAllowedWriteId.get()) return;
            try {
                config.save(itemsFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[UItem] Could not save to " + itemsFile.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Default file (chỉ tạo khi chưa tồn tại)
    // ═════════════════════════════════════════════════════════════════════════

    private void createDefaultLegacyFile() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.options().setHeader(java.util.Arrays.asList(
                "╔══════════════════════════════════════════╗",
                "║         UItem - items.yml (legacy)       ║",
                "║                                          ║",
                "║  RECOMMENDED: Use external folder config ║",
                "║  for easy copy-paste of items!           ║",
                "║                                          ║",
                "║  Drop your item configs into:            ║",
                "║    plugins/UItem/items/                  ║",
                "║                                          ║",
                "║  You can use either style:               ║",
                "║    - Wrapped: items: { id: {..} }        ║",
                "║    - Flat (easy copy-paste):             ║",
                "║        my_item:                          ║",
                "║          material: DIAMOND_SWORD         ║",
                "║          ...                             ║",
                "║                                          ║",
                "║  Multiple files supported (weapons.yml,  ║",
                "║  armor.yml, boss/mythic.yml ...).        ║",
                "╚══════════════════════════════════════════╝"
        ));

        cfg.set("items.ruby_sword.material", "DIAMOND_SWORD");
        cfg.set("items.ruby_sword.display-name",
                "<gradient:#ff0000:#ff6600><bold>Ruby Sword</bold></gradient>");
        cfg.set("items.ruby_sword.lore", List.of(
                "<gray>Một thanh kiếm huyền thoại",
                "<gray>được rèn từ <red>Ruby <gray>nguyên chất"));
        cfg.set("items.ruby_sword.enchantments.sharpness",  5);
        cfg.set("items.ruby_sword.enchantments.unbreaking", 3);
        cfg.set("items.ruby_sword.flags", List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        cfg.set("items.ruby_sword.custom-model-data", 10001);
        cfg.set("items.ruby_sword.unbreakable", true);
        cfg.set("items.ruby_sword.glowing", false);

        cfg.set("items.magic_apple.material", "GOLDEN_APPLE");
        cfg.set("items.magic_apple.display-name",
                "<light_purple><bold>Magic Apple</bold></light_purple>");
        cfg.set("items.magic_apple.lore", List.of("<gray>Một quả táo ma thuật",
                "<green>Hồi phục tất cả HP"));
        cfg.set("items.magic_apple.unbreakable", false);
        cfg.set("items.magic_apple.glowing", true);

        synchronized (fileLock) {
            try {
                cfg.save(legacyFile);
                plugin.getLogger().info("[UItem] Created sample " + LEGACY_FILE + ".");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[UItem] Could not create " + LEGACY_FILE + ": " + e.getMessage(), e);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private static Material parseMaterial(String name) {
        try { return Material.valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static boolean isValidOperation(String op) {
        if (op == null) return false;
        return switch (op.toUpperCase(Locale.ROOT)) {
            case "ADD_NUMBER", "ADD_SCALAR", "MULTIPLY_SCALAR_1" -> true;
            default -> false;
        };
    }

    private static String yamlErrorDetail(InvalidConfigurationException e) {
        String m = e.getMessage();
        if (m == null && e.getCause() != null) m = e.getCause().getMessage();
        return m != null ? m.trim() : "unknown YAML error";
    }

    private String relativeToDataFolder(File file) {
        String base = plugin.getDataFolder().getAbsolutePath();
        String full = file.getAbsolutePath();
        return full.startsWith(base) ? full.substring(base.length() + 1) : full;
    }

    private void logError(String fmt, Object... args) {
        plugin.getLogger().warning("[ERROR] " + String.format(fmt, args));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════

    /** Recursively convert a ConfigurationSection to a plain Map<String, Object>. */
    private static Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nested) {
                result.put(key, sectionToMap(nested));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Records
    // ═════════════════════════════════════════════════════════════════════════

    private record PendingSave(Map<String, UItemData> snapshot, long requestId) {}

    private record LoadResult(UItemData data) {
        static final LoadResult SKIP = new LoadResult(null);
    }

    private record FileLoadResult(int loaded, int skipped) {
        static final FileLoadResult EMPTY = new FileLoadResult(0, 0);
    }
}
