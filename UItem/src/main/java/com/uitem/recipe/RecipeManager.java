package com.uitem.recipe;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Quản lý đọc/ghi recipe config.
 *
 * <h3>Cấu trúc thư mục được hỗ trợ:</h3>
 * <pre>
 * plugins/UItem/
 * ├── recipes.yml            ← file đơn (backward-compat)
 * └── recipes/               ← thư mục ưu tiên hơn nếu tồn tại
 *     ├── boss_recipes.yml
 *     ├── weapons.yml
 *     └── special/
 *         └── events.yml
 * </pre>
 *
 * <p><b>Quy tắc giống StorageManager:</b>
 * <ul>
 *   <li>Nếu thư mục {@code recipes/} tồn tại → quét TẤT CẢ file .yml (đệ quy).</li>
 *   <li>Nếu không → đọc {@code recipes.yml}.</li>
 *   <li>Reload đọc lại từ disk, không cache.</li>
 *   <li>Save từ GUI luôn ghi vào {@code recipes.yml}.</li>
 *   <li>Không ghi đè file người dùng. Không crash khi config sai.</li>
 *   <li>Mọi lỗi đều được log rõ ràng.</li>
 * </ul>
 */
public class RecipeManager {

    private static final String LEGACY_FILE     = "recipes.yml";
    private static final String RECIPES_DIR     = "recipes";
    private static final String RECIPES_SECTION = "recipes";

    private final UItemPlugin  plugin;
    private final UItemManager itemManager;

    /** Map chính: itemId → recipe. */
    private final Map<String, UItemRecipe> recipes = new LinkedHashMap<>();

    private final File legacyFile;
    private final File recipesDir;
    private final File recipesFile;

    public RecipeManager(UItemPlugin plugin, UItemManager itemManager) {
        this.plugin      = plugin;
        this.itemManager = itemManager;
        this.legacyFile  = new File(plugin.getDataFolder(), LEGACY_FILE);
        this.recipesDir  = new File(plugin.getDataFolder(), RECIPES_DIR);
        this.recipesDir.mkdirs();
        this.recipesFile = new File(this.recipesDir, "recipe.yml");
        loadRecipes();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — truy vấn
    // ═════════════════════════════════════════════════════════════════════════

    public Map<String, UItemRecipe> getAllRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    public UItemRecipe getRecipe(String itemId) {
        return recipes.get(normalise(itemId));
    }

    public boolean hasRecipe(String itemId) {
        return recipes.containsKey(normalise(itemId));
    }

    public void setRecipe(String itemId, UItemRecipe recipe) {
        if (recipe != null) recipes.put(normalise(itemId), recipe);
    }

    public void removeRecipe(String itemId) {
        recipes.remove(normalise(itemId));
    }

    public ItemStack matchRecipe(ItemStack[] matrix) {
        for (UItemRecipe recipe : recipes.values()) {
            if (recipe.matches(matrix)) {
                UItemData data = itemManager.getItem(recipe.getResultItemId());
                if (data != null) {
                    ItemStack result = itemManager.buildItemStack(data);
                    result.setAmount(recipe.getResultAmount());
                    return result;
                }
            }
        }
        return null;
    }

    /** @deprecated Dùng {@link #matchRecipe(ItemStack[])} — consumption được xử lý trong CraftingGui. */
    @Deprecated
    public ItemStack matchRecipeAndConsume(ItemStack[] matrix) {
        return matchRecipe(matrix);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — load
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Load/reload tất cả recipes từ disk.
     * Gọi lại khi /uitem reload.
     */
    public void loadRecipes() {
        recipes.clear();

        if (recipesDir.exists() && recipesDir.isDirectory()) {
            loadFromDirectory();
        } else if (legacyFile.exists()) {
            loadFromLegacyFile();
        }
        // Không có file nào → 0 recipes, bình thường
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Load thư mục recipes/
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFromDirectory() {
        List<File> yamls = collectYamlFiles(recipesDir);
        if (yamls.isEmpty()) {
            plugin.getLogger().info("[UItem] recipes/ folder is empty — no recipes loaded.");
            return;
        }
        plugin.getLogger().info("[UItem] Loading recipes from " + yamls.size() + " file(s) in recipes/ ...");

        int total = 0, skipped = 0;
        for (File f : yamls) {
            String label = relativeToDataFolder(f);
            int[] r = loadSingleFile(f, label);
            total   += r[0];
            skipped += r[1];
        }
        plugin.getLogger().info(String.format(
                "[UItem] Recipes done — %d loaded, %d skipped.", total, skipped));
    }

    private List<File> collectYamlFiles(File dir) {
        List<File> out = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) return out;
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File f : children) {
            if (f.isDirectory()) out.addAll(collectYamlFiles(f));
            else if (f.isFile() && f.getName().endsWith(".yml")) out.add(f);
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Load file recipes.yml đơn (legacy)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadFromLegacyFile() {
        int[] r = loadSingleFile(legacyFile, LEGACY_FILE);
        plugin.getLogger().info(String.format(
                "[UItem] %s loaded: %d recipe(s) OK, %d skipped.", LEGACY_FILE, r[0], r[1]));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Đọc một file .yml  → int[]{loaded, skipped}
    // ─────────────────────────────────────────────────────────────────────────

    private int[] loadSingleFile(File file, String label) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(file);
        } catch (IOException e) {
            logError("[%s] Cannot read file: %s", label, e.getMessage());
            return new int[]{0, 0};
        } catch (InvalidConfigurationException e) {
            logError("[%s] YAML syntax error — %s", label, yamlErrorDetail(e));
            logError("[%s] Fix the error and run /uitem reload.", label);
            return new int[]{0, 0};
        }

        // Support both wrapped "recipes:" and flat copy-paste style (single recipe blocks at root)
        // Makes it easy to drop external / copy-pasted recipe configs into recipes/ folder.
        ConfigurationSection section = null;
        boolean flatMode = false;

        if (cfg.isConfigurationSection(RECIPES_SECTION)) {
            section = cfg.getConfigurationSection(RECIPES_SECTION);
        } else {
            section = cfg;
            flatMode = true;
        }

        if (section == null) {
            plugin.getLogger().warning("[UItem] [" + label + "] No '" + RECIPES_SECTION + "' section or flat recipes found — skipping.");
            return new int[]{0, 0};
        }

        int loaded = 0, skipped = 0;
        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                if (!flatMode) {
                    logError("[%s] Recipe '%s' is not a YAML mapping — skipped.", label, key);
                    skipped++;
                }
                continue;
            }
            ConfigurationSection recipeSection = section.getConfigurationSection(key);
            if (recipeSection == null) { skipped++; continue; }

            // Trùng ID
            if (recipes.containsKey(normalise(key))) {
                logError("[%s] Duplicate recipe id '%s' — already registered from another file. Skipped.",
                        label, key);
                skipped++;
                continue;
            }

            try {
                UItemRecipe recipe = UItemRecipe.load(recipeSection);
                if (recipe == null) {
                    logError("[%s] Recipe '%s': failed to parse (missing 'result-item'?).", label, key);
                    skipped++;
                } else {
                    // Validate result-item tồn tại (warn nhẹ — item có thể load sau)
                    if (!itemManager.hasItem(recipe.getResultItemId())) {
                        plugin.getLogger().warning("[UItem] [" + label + "] Recipe '" + key
                                + "': result-item '" + recipe.getResultItemId()
                                + "' not found in loaded items — recipe registered anyway.");
                    }
                    recipes.put(normalise(key), recipe);
                    loaded++;
                }
            } catch (Exception e) {
                logError("[%s] Recipe '%s': unexpected error — %s", label, key, e.getMessage());
                skipped++;
            }
        }
        return new int[]{loaded, skipped};
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC — save  (GUI → recipes.yml)
    // ═════════════════════════════════════════════════════════════════════════

    public void saveRecipes() {
        doSave();
    }

    public void saveRecipeIndividual(String resultItemId, UItemRecipe recipe) {
        if (resultItemId == null || recipe == null) return;
        File target = new File(recipesDir, resultItemId + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.options().setHeader(List.of(
            "Individual recipe file (copy-paste friendly).",
            "Full shaped/shapeless + ingredients (uitem or material) are supported."
        ));

        // Use the existing save logic from UItemRecipe for complete fidelity
        ConfigurationSection recipeSection = cfg.createSection(resultItemId);
        recipe.save(recipeSection);

        try {
            cfg.save(target);
            recipes.put(normalise(resultItemId), recipe);
        } catch (IOException e) {
            plugin.getLogger().warning("[UItem] Failed to save individual recipe file: " + e.getMessage());
        }
    }

    public void saveRecipesAsync() {
        boolean useConsolidated = plugin.getConfig().getBoolean("storage.use-consolidated-files", false);
        if (!useConsolidated) {
            // Prefer individual files
            for (String id : new ArrayList<>(recipes.keySet())) {
                UItemRecipe r = recipes.get(id);
                if (r != null) saveRecipeIndividual(id, r);
            }
            return;
        }
        YamlConfiguration cfg = buildConfig();
        SchedulerUtil.runAsync(plugin, () -> {
            try { cfg.save(recipesFile); }
            catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[UItem] Could not save " + recipesFile.getName(), e);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────

    private YamlConfiguration buildConfig() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, UItemRecipe> e : recipes.entrySet()) {
            ConfigurationSection s = cfg.createSection(RECIPES_SECTION + "." + e.getKey());
            e.getValue().save(s);
        }
        return cfg;
    }

    private void doSave() {
        boolean useConsolidated = plugin.getConfig().getBoolean("storage.use-consolidated-files", false);
        if (!useConsolidated) {
            for (String id : new ArrayList<>(recipes.keySet())) {
                UItemRecipe r = recipes.get(id);
                if (r != null) saveRecipeIndividual(id, r);
            }
            return;
        }
        try { buildConfig().save(recipesFile); }
        catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[UItem] Could not save " + recipesFile.getName(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private static String normalise(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
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
}
