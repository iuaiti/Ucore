package com.uitem.resourcepack;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Resource pack engine cho UItem (thiết kế giống Oraxen / ItemsAdder).
 *
 * - Hỗ trợ user-provided textures + full custom .json models (drop vào models/ folder)
 * - Không xóa file user tự thêm trong resourcepack/
 * - Sử dụng CustomModelData + namespace riêng
 *
 * Cấu trúc generate:
 * - assets/minecraft/models/item/<base>.json (overrides)
 * - assets/uitem/models/item/<id>.json
 * - assets/uitem/textures/item/<id>.png
 *
 * Nguồn user:
 * - plugins/UItem/textures/<id>.png
 * - plugins/UItem/models/<id>.json   (full model, sẽ được copy nguyên)
 */
public class ResourcePackManager {

    private static final int SHA1_LENGTH = 20;

    private final UItemPlugin plugin;
    private final UItemManager itemManager;
    private final File rpDir;
    private final File cmdConfigFile;

    // Mutable: được cập nhật khi reloadFromConfig()
    private File sourceTextureDir;
    private File sourceModelDir;   // NEW: support user-provided model JSONs (like Oraxen/ItemsAdder)
    private String namespace;
    private String modelFolder;
    private String textureFolder;
    private String baseModelFolder;
    private String outputZipName;
    private int packFormat;

    private final Object cmdLock = new Object();
    private org.bukkit.configuration.file.FileConfiguration cmdConfig;
    private final Map<String, Integer> cmdCache = new LinkedHashMap<>();
    private int nextCmd;

    private volatile File lastZipFile;
    private volatile byte[] lastPackHash;

    public ResourcePackManager(UItemPlugin plugin, UItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.rpDir = new File(plugin.getDataFolder(), "resourcepack");
        this.cmdConfigFile = new File(plugin.getDataFolder(),
                plugin.getConfig().getString("resourcepack.cmd.storage-file", "custom_model_data.yml"));
        readConfigFields(plugin.getConfig());
        loadCmdConfig();

        // Ensure source folders exist immediately so users can drop textures/models for copy-paste
        if (sourceTextureDir != null) sourceTextureDir.mkdirs();
        if (sourceModelDir != null) sourceModelDir.mkdirs();
    }

    /**
     * Đọc lại các trường cấu hình từ config (dùng khi reload).
     */
    public void reloadFromConfig(org.bukkit.configuration.file.FileConfiguration config) {
        readConfigFields(config);
        loadCmdConfig();
        plugin.getLogger().info("[UItem] ResourcePackManager reloaded config.");
    }

    private void readConfigFields(org.bukkit.configuration.file.FileConfiguration config) {
        this.sourceTextureDir = new File(plugin.getDataFolder(),
                config.getString("resourcepack.source-texture-folder", "textures"));
        // NEW: models folder next to textures for user custom .json models (Oraxen/ItemsAdder style)
        this.sourceModelDir = new File(plugin.getDataFolder(),
                config.getString("resourcepack.source-model-folder", "models"));
        this.namespace = config.getString("resourcepack.namespace", "uitem");
        this.modelFolder = normalizeFolder(config.getString("resourcepack.model-folder", "assets/uitem/models/item"));
        this.textureFolder = normalizeFolder(config.getString("resourcepack.texture-folder", "assets/uitem/textures/item"));
        this.baseModelFolder = normalizeFolder(config.getString("resourcepack.base-model-folder", "assets/minecraft/models/item"));
        this.outputZipName = config.getString("resourcepack.output-zip", "UItem-ResourcePack.zip");
        this.packFormat = Math.max(1, config.getInt("resourcepack.pack-format", 34));
    }

    public File getResourcePackDirectory() {
        return rpDir;
    }

    public File getCmdConfigFile() {
        return cmdConfigFile;
    }

    public File getLastZipFile() {
        return lastZipFile;
    }

    public byte[] getLastPackHash() {
        return lastPackHash == null ? null : lastPackHash.clone();
    }

    public String getLastPackHashHex() {
        return lastPackHash == null ? null : toHex(lastPackHash);
    }

    public synchronized void reload() {
        loadCmdConfig();
    }

    private void loadCmdConfig() {
        synchronized (cmdLock) {
            if (!cmdConfigFile.exists()) {
                File parent = cmdConfigFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                try {
                    cmdConfigFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not create CMD config file: " + e.getMessage());
                }
            }
            cmdConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(cmdConfigFile);
            nextCmd = Math.max(1001, cmdConfig.getInt("next_cmd", 1001));

            cmdCache.clear();
            var section = cmdConfig.getConfigurationSection("items");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    int value = section.getInt(key, -1);
                    if (value > 0) {
                        cmdCache.put(key.toLowerCase(Locale.ROOT), value);
                    }
                }
            }
        }
    }

    private void saveCmdConfig() {
        synchronized (cmdLock) {
            cmdConfig.set("next_cmd", nextCmd);
            for (Map.Entry<String, Integer> entry : cmdCache.entrySet()) {
                cmdConfig.set("items." + entry.getKey(), entry.getValue());
            }
            try {
                cmdConfig.save(cmdConfigFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save CMD config: " + e.getMessage());
            }
        }
    }

    /**
     * Lấy hoặc cấp CustomModelData cho item, đảm bảo không trùng.
     */
    public int getOrAssignCmd(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return -1;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        synchronized (cmdLock) {
            Integer existing = cmdCache.get(normalized);
            if (existing != null && existing > 0) {
                return existing;
            }
            int cmd = nextCmd++;
            cmdCache.put(normalized, cmd);
            saveCmdConfig();
            return cmd;
        }
    }

    /**
     * Đồng bộ CMD vào UItemData nếu item chưa có CMD hợp lệ.
     */
    public void syncCmdToItem(UItemData data) {
        if (data == null || data.getId() == null || data.getId().isBlank()) {
            return;
        }
        if (data.getCustomModelData() > 0) {
            synchronized (cmdLock) {
                cmdCache.put(data.getId().toLowerCase(Locale.ROOT), data.getCustomModelData());
                saveCmdConfig();
            }
            return;
        }
        int cmd = getOrAssignCmd(data.getId());
        data.setCustomModelData(cmd);
    }

    /**
     * Đồng bộ CMD cache vào các item đang load trong RAM.
     * Dùng trên main thread sau khi load/reload để tránh item stack bị thiếu CMD.
     */
    public void syncCmdsToLiveItems() {
        boolean changed = false;
        synchronized (cmdLock) {
            for (UItemData data : itemManager.getAllItems().values()) {
                if (data == null || data.getId() == null || data.getId().isBlank()) {
                    continue;
                }

                String id = data.getId().toLowerCase(Locale.ROOT);
                int currentCmd = data.getCustomModelData();
                Integer cached = cmdCache.get(id);

                if (currentCmd > 0) {
                    if (cached == null || cached != currentCmd) {
                        cmdCache.put(id, currentCmd);
                        changed = true;
                    }
                    continue;
                }

                if (cached != null && cached > 0) {
                    data.setCustomModelData(cached);
                    continue;
                }

                int assigned = nextCmd++;
                cmdCache.put(id, assigned);
                data.setCustomModelData(assigned);
                changed = true;
            }

            if (changed) {
                saveCmdConfig();
            }
        }

        if (changed) {
            plugin.getStorageManager().saveAllNow(itemManager);
        }
    }

    /**
     * Generate toàn bộ pack đúng cấu trúc.
     */
    public synchronized void generatePack() {
        clearGeneratedPack();
        createDirectories();

        Map<String, List<OverrideEntry>> groupedOverrides = new LinkedHashMap<>();
        Map<String, UItemData> snapshot = itemManager.snapshotAll();
        List<Map.Entry<String, UItemData>> sortedItems = new ArrayList<>(snapshot.entrySet());
        sortedItems.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));

        for (Map.Entry<String, UItemData> entry : sortedItems) {
            String itemId = entry.getKey();
            UItemData data = entry.getValue();
            if (data == null) {
                continue;
            }

            int cmd = data.getCustomModelData();
            if (cmd <= 0) {
                cmd = getOrAssignCmd(itemId);
            }
            if (cmd <= 0) {
                continue;
            }

            Material material = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
            String baseMaterial = material.name().toLowerCase(Locale.ROOT);

            // Support flexible model mapping via "model" in item config (like Oraxen/ItemsAdder)
            // If item has "model: my_cool_model", it will look for models/my_cool_model.json
            // and use uitem:item/my_cool_model in the override (no need for filename == itemId)
            String modelRef = getModelReference(data, itemId);
            groupedOverrides.computeIfAbsent(baseMaterial, k -> new ArrayList<>())
                    .add(new OverrideEntry(cmd, modelRef));

            // Only auto-copy texture for items using default/uitem model (for auto-generated simple models).
            // For external models (e.g. itemsadder:...) the model json handles its own textures; user provides them if needed.
            String modelField = (data != null ? data.getModel() : null);
            if (modelField == null || modelField.isBlank() || modelField.startsWith(namespace + ":") || !modelField.contains(":")) {
                copyOrGenerateTexture(itemId);
            }
        }

        for (Map.Entry<String, List<OverrideEntry>> entry : groupedOverrides.entrySet()) {
            generateBaseModelJson(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, UItemData> entry : sortedItems) {
            copyOrGenerateCustomModel(entry.getKey(), entry.getValue());
        }

        generatePackMcmeta();
        plugin.getLogger().info("[UItem] Resource pack generated at: " + rpDir.getAbsolutePath());
    }

    /**
     * Only clear the parts WE generate. Do NOT delete user files placed in resourcepack/ folder.
     * This makes it possible to add extra assets (like Oraxen/ItemsAdder users do).
     */
    private void clearGeneratedPack() {
        if (!rpDir.exists()) {
            return;
        }

        List<File> toClear = new ArrayList<>();
        toClear.add(new File(rpDir, baseModelFolder));
        toClear.add(new File(rpDir, modelFolder));
        toClear.add(new File(rpDir, textureFolder));
        // NOTE: intentionally not clearing pack.mcmeta here - generatePackMcmeta() will (re)write the correct one anyway.
        // Prevents deleting user-provided root files in resourcepack/ dir.

        for (File target : toClear) {
            if (target.exists()) {
                try {
                    if (target.isDirectory()) {
                        try (Stream<Path> stream = Files.walk(target.toPath())) {
                            stream.sorted(Comparator.reverseOrder())
                                  .forEach(p -> {
                                      try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                                  });
                        }
                    } else {
                        Files.deleteIfExists(target.toPath());
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not clean generated: " + target + " - " + e.getMessage());
                }
            }
        }
    }

    private void createDirectories() {
        new File(rpDir, baseModelFolder).mkdirs();
        new File(rpDir, modelFolder).mkdirs();
        new File(rpDir, textureFolder).mkdirs();
        sourceTextureDir.mkdirs();
        sourceModelDir.mkdirs();  // for user custom models
    }

    /**
     * Tạo file override cho base item.
     * Ví dụ: assets/minecraft/models/item/diamond_sword.json
     */
    private void generateBaseModelJson(String baseMaterial, List<OverrideEntry> overrides) {
        Collections.sort(overrides, Comparator.comparingInt(o -> o.cmd));
        String parent = isHandheld(baseMaterial) ? "minecraft:item/handheld" : "minecraft:item/generated";

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"parent\": \"").append(parent).append("\",\n");
        json.append("  \"textures\": {\n");
        json.append("    \"layer0\": \"minecraft:item/").append(baseMaterial).append("\"\n");
        json.append("  },\n");
        json.append("  \"overrides\": [\n");

        for (int i = 0; i < overrides.size(); i++) {
            OverrideEntry oe = overrides.get(i);
            json.append("    {\n");
            json.append("      \"predicate\": { \"custom_model_data\": ").append(oe.cmd).append(" },\n");
            json.append("      \"model\": \"").append(oe.model).append("\"\n");
            json.append("    }");
            if (i < overrides.size() - 1) {
                json.append(',');
            }
            json.append('\n');
        }

        json.append("  ]\n");
        json.append("}\n");

        File outFile = new File(rpDir, baseModelFolder + "/" + baseMaterial + ".json");
        writeText(outFile, json.toString());
    }

    /**
     * Copy user model or auto-generate simple one.
     * Supports flexible model: via "model:" in item config (e.g. "itemsadder:item/xxx" for dropping IA models).
     * User models copied as-is to correct assets/<ns>/models/... location.
     */
    private void copyOrGenerateCustomModel(String itemId, UItemData data) {
        String modelRef = getModelReference(data, itemId);

        // Compute target file path in the pack based on the model reference (supports "itemsadder:item/xxx" etc.)
        Path targetModelPath = getTargetModelPathFromRef(modelRef);
        File outFile = targetModelPath.toFile();

        // Determine source lookup: support dropping flat or with path structure in models/
        String modelPathPart = modelRef.contains(":") ? modelRef.substring(modelRef.indexOf(':') + 1) : modelRef;
        String lastSegment = modelPathPart.contains("/") ? modelPathPart.substring(modelPathPart.lastIndexOf('/') + 1) : modelPathPart;
        String safeLast = sanitizeFileName(lastSegment);

        // Try flat first (easiest for "ném model của ItemAdder"): models/<lastSegment>.json
        Path userModel = sourceModelDir.toPath().resolve(safeLast + ".json");

        // Also try with the path structure if present: models/<modelPathPart>.json (e.g. models/item/some.json)
        if (!Files.exists(userModel) && modelPathPart.contains("/")) {
            String safePath = sanitizeFileName(modelPathPart);
            Path alt = sourceModelDir.toPath().resolve(safePath + ".json");
            if (Files.exists(alt)) {
                userModel = alt;
            }
        }

        // 1. Try to copy user-provided model JSON (perfect for dropping ItemsAdder / Oraxen models)
        // User drops the .json (with any name) into plugins/UItem/models/
        // The content is copied as-is, preserving internal texture references (e.g. itemsadder:...)
        if (Files.exists(userModel)) {
            try {
                Files.createDirectories(outFile.getParentFile().toPath());
                Files.copy(userModel, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                plugin.getLogger().warning("[UItem] Failed to copy user model for " + itemId + ": " + e.getMessage());
            }
        }

        // 2. No user model provided → auto generate a simple one
        // Texture still based on the UItem ID (user can override by providing full model)
        Material material = data != null && data.getMaterial() != null ? data.getMaterial() : Material.STONE;
        String parent = isHandheld(material.name().toLowerCase(Locale.ROOT)) ? "minecraft:item/handheld" : "minecraft:item/generated";
        String texturePath = namespace + ":item/" + sanitizeFileName(itemId);

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"parent\": \"").append(parent).append("\",\n");
        json.append("  \"textures\": {\n");
        json.append("    \"layer0\": \"").append(texturePath).append("\"\n");
        json.append("  }\n");
        json.append("}\n");

        try {
            Files.createDirectories(outFile.getParentFile().toPath());
            writeText(outFile, json.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("[UItem] Failed to write generated model for " + itemId);
        }
    }

    private Path getTargetModelPathFromRef(String modelRef) {
        if (modelRef == null || modelRef.isBlank()) {
            modelRef = namespace + ":item/unknown";
        }
        String ref = modelRef.contains(":") ? modelRef : (namespace + ":item/" + sanitizeFileName(modelRef));
        String[] split = ref.split(":", 2);
        String ns = split[0];
        String path = split.length > 1 ? split[1] : "item/" + sanitizeFileName(ref);
        // Ensure it ends up as assets/<ns>/models/<path>.json
        String relative = "assets/" + ns + "/models/" + path + ".json";
        return rpDir.toPath().resolve(relative.replace('\\', '/'));
    }

    private void copyOrGenerateTexture(String itemId) {
        String safeId = sanitizeFileName(itemId);
        Path source = sourceTextureDir.toPath().resolve(safeId + ".png");
        Path target = new File(rpDir, textureFolder).toPath().resolve(safeId + ".png");

        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(source)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }

            plugin.getLogger().warning("[UItem] Missing texture for item: " + itemId + " (needed: " + source.toAbsolutePath() + "). Generating placeholder.");
            generatePlaceholderTexture(target, itemId);
        } catch (IOException e) {
            plugin.getLogger().warning("[UItem] Could not copy/generate texture for item " + itemId + ": " + e.getMessage());
        }
    }

    private void generatePlaceholderTexture(Path target, String itemId) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int seed = Math.abs(itemId == null ? 0 : itemId.hashCode());
            Color c1 = new Color((seed >> 16) & 0xFF, (seed >> 8) & 0xFF, seed & 0xFF, 255);
            Color c2 = new Color(40, 40, 40, 255);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean alt = ((x + y) & 1) == 0;
                    g.setColor(alt ? c1 : c2);
                    g.fillRect(x, y, 1, 1);
                }
            }
            g.setColor(new Color(255, 255, 255, 120));
            g.drawRect(0, 0, 15, 15);
        } finally {
            g.dispose();
        }
        Files.createDirectories(target.getParent());
        ImageIO.write(image, "PNG", target.toFile());
    }

    private void generatePackMcmeta() {
        String json = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": " + packFormat + ",\n" +
                "    \"description\": \"UItem Resource Pack\"\n" +
                "  }\n" +
                "}\n";
        writeText(new File(rpDir, "pack.mcmeta"), json);
    }

    /**
     * Xuất toàn bộ pack thành file zip.
     */
    public File exportZip() throws IOException {
        generatePack();

        File zipFile = new File(plugin.getDataFolder(), outputZipName);
        if (zipFile.exists() && !zipFile.delete()) {
            throw new IOException("Could not delete old zip file: " + zipFile.getAbsolutePath());
        }

        Path rpPath = rpDir.toPath();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
             Stream<Path> paths = Files.walk(rpPath)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String entryName = rpPath.relativize(file).toString().replace('\\', '/');
                try {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException io) {
                throw io;
            }
            throw ex;
        }

        lastZipFile = zipFile;
        lastPackHash = calculateSHA1(zipFile);
        plugin.getLogger().info("[UItem] Exported resource pack: " + zipFile.getAbsolutePath());
        return zipFile;
    }

    /**
     * Tính SHA-1 cho file zip.
     */
    public byte[] calculateSHA1(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist to calculate SHA-1");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 not available", e);
        }
    }

    public String sha1Hex(File file) throws IOException {
        return toHex(calculateSHA1(file));
    }

    public byte[] parseHexHash(String hex) {
        if (hex == null) {
            return null;
        }
        String clean = hex.trim().replace(" ", "").toLowerCase(Locale.ROOT);
        if (clean.length() != SHA1_LENGTH * 2 || (clean.length() & 1) != 0) {
            return null;
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            int hi = Character.digit(clean.charAt(i), 16);
            int lo = Character.digit(clean.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }

    /**
     * Gửi resource pack cho player theo URL.
     */
    public void applyResourcePack(Player player, String url) {
        applyResourcePack(player, url, (byte[]) null);
    }

    public void applyResourcePack(Player player, String url, String hashHex) {
        byte[] hash = parseHexHash(hashHex);
        if (hash == null && hashHex != null && !hashHex.isBlank()) {
            plugin.getLogger().warning("[UItem] Invalid hash, will use cached hash or send pack without hash.");
        }
        applyResourcePack(player, url, hash);
    }

    @SuppressWarnings("deprecation")
    public void applyResourcePack(Player player, String url, byte[] hash) {
        if (player == null || url == null || url.isBlank()) {
            return;
        }

        byte[] finalHash = hash;
        if (finalHash == null || finalHash.length != SHA1_LENGTH) {
            if (lastPackHash != null && lastPackHash.length == SHA1_LENGTH) {
                finalHash = lastPackHash.clone();
            }
        }

        byte[] useHash = finalHash == null ? null : finalHash.clone();
        Runnable task = () -> {
            if (useHash != null) {
                player.setResourcePack(url, useHash);
            } else {
                player.setResourcePack(url);
            }
        };

        SchedulerUtil.runSync(plugin, task);
    }

    private boolean isHandheld(String material) {
        String m = material == null ? "" : material.toLowerCase(Locale.ROOT);
        return m.contains("sword") || m.contains("axe") || m.contains("pickaxe") || m.contains("shovel") || m.contains("hoe") || m.contains("rod") || m.contains("trident") || m.contains("mace");
    }

    private void writeText(File file, String content) {
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write file: " + file.getAbsolutePath() + " | " + e.getMessage());
        }
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "";
        }
        String normalized = folder.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    /**
     * Returns the model path to put in the "overrides" (e.g. "uitem:item/my_model").
     * Honors the "model" field in the item config so you don't have to name the model file exactly the same as the item ID.
     * You can put "model: my_shared_model" in several items to reuse one model file (very useful for copy-paste external configs).
     */
    private String getModelReference(UItemData data, String itemId) {
        if (data != null && data.getModel() != null && !data.getModel().isBlank()) {
            String m = data.getModel().trim();
            if (m.contains(":")) {
                return m; // user gave full reference
            }
            return namespace + ":item/" + sanitizeFileName(m);
        }
        return namespace + ":item/" + sanitizeFileName(itemId);
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

    private static final class OverrideEntry {
        final int cmd;
        final String model;

        OverrideEntry(int cmd, String model) {
            this.cmd = cmd;
            this.model = model;
        }
    }
}
