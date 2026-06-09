package com.uitem.rarity;

import com.uitem.UItemPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RarityConfig {

    private static final Map<String, RarityEntry> rarities = new LinkedHashMap<>();
    private static UItemPlugin plugin;
    private static File rarityFile;

    public static void load(UItemPlugin pluginInstance) {
        plugin = pluginInstance;
        rarities.clear(); // Clear trước khi load: tránh entries cũ tích tụ khi reload

        // Dedicated rarity file (user requested separate file for easy copy-paste)
        rarityFile = new File(plugin.getDataFolder(), "rarity.yml");
        if (!rarityFile.exists()) {
            // Try legacy from config.yml first
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("rarity");
            if (section != null && !section.getKeys(false).isEmpty()) {
                loadFromSection(section);
                saveRarities(); // migrate to new file
                plugin.getLogger().info("[UItem] Migrated rarity config from config.yml to rarity.yml");
                return;
            }
            // No legacy, copy default from resources
            plugin.saveResource("rarity.yml", false);
            plugin.getLogger().info("[UItem] Created default rarity.yml");
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(rarityFile);
        ConfigurationSection section = null;

        // Support both:
        // rarity: { COMMON: {..} }   or flat: COMMON: {..} at root
        if (cfg.isConfigurationSection("rarity")) {
            section = cfg.getConfigurationSection("rarity");
        } else if (cfg.getKeys(false).stream().anyMatch(k -> cfg.isConfigurationSection(k))) {
            section = cfg; // flat mode at root
        }

        if (section == null) {
            createDefaults();
            saveRarities();
            return;
        }
        loadFromSection(section);
    }

    private static void loadFromSection(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection rs = section.getConfigurationSection(key);
            if (rs == null) continue;
            String name = key.toUpperCase(Locale.ROOT);
            String color = rs.getString("color", "<white>");
            String prefix = rs.getString("prefix", "");
            List<String> lore = rs.getStringList("lore");
            rarities.put(name, new RarityEntry(name, color, prefix, lore));
            plugin.getLogger().info("[UItem] Loaded rarity: " + name + " (color=" + color + ", prefix=" + prefix + ")");
        }
    }

    public static void saveRarities() {
        if (plugin == null || rarityFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.options().setHeader(List.of(
            "Rarity definitions for UItem.",
            "You can copy-paste entire rarity blocks easily.",
            "Supports flat style (recommended for copy-paste):",
            "  COMMON:",
            "    color: \"<gray>\"",
            "    prefix: \"\"",
            "    lore: []",
            "",
            "Or wrapped under 'rarity:' section."
        ));

        // Write in flat style at root for easy copy-paste
        for (Map.Entry<String, RarityEntry> entry : rarities.entrySet()) {
            String key = entry.getKey();
            RarityEntry r = entry.getValue();
            cfg.set(key + ".color", r.color());
            cfg.set(key + ".prefix", r.prefix());
            cfg.set(key + ".lore", r.lore());
        }

        try {
            cfg.save(rarityFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[UItem] Could not save rarity.yml: " + e.getMessage());
        }
    }

    private static void createDefaults() {
        rarities.clear();
        rarities.put("COMMON", new RarityEntry("COMMON", "<gray>", "", List.of()));
        rarities.put("UNCOMMON", new RarityEntry("UNCOMMON", "<green>", "<green>", List.of(
                "<dark_gray>──────────────",
                "<green>Uncommon"
        )));
        rarities.put("RARE", new RarityEntry("RARE", "<blue>", "<blue>", List.of(
                "<dark_gray>──────────────",
                "<blue>Rare"
        )));
        rarities.put("EPIC", new RarityEntry("EPIC", "<dark_purple>", "<dark_purple>", List.of(
                "<dark_gray>──────────────",
                "<dark_purple>Epic"
        )));
        rarities.put("LEGENDARY", new RarityEntry("LEGENDARY", "<gold>", "<gold>", List.of(
                "<dark_gray>──────────────",
                "<gold>Legendary"
        )));
        rarities.put("ULTRA", new RarityEntry("ULTRA", "<red>", "<red>", List.of(
                "<dark_gray>──────────────",
                "<red>✦ ULTRA ✦"
        )));
    }

    public static RarityEntry get(String rarity) {
        if (rarity == null) return rarities.get("COMMON");
        return rarities.get(rarity.toUpperCase(Locale.ROOT));
    }

    public static Set<String> getKeys() {
        return rarities.keySet();
    }

    public static List<String> getRarityLore(String rarity) {
        RarityEntry entry = get(rarity);
        return entry != null ? entry.lore() : List.of();
    }

    public static String getRarityColor(String rarity) {
        RarityEntry entry = get(rarity);
        return entry != null ? entry.color() : "<white>";
    }

    public static String getRarityPrefix(String rarity) {
        RarityEntry entry = get(rarity);
        return entry != null ? entry.prefix() : "";
    }

    // === Editing support (for GUI) ===
    public static void setRarity(String name, String color, String prefix, List<String> lore) {
        if (name == null || name.isBlank()) return;
        String key = name.toUpperCase(Locale.ROOT);
        rarities.put(key, new RarityEntry(key, color != null ? color : "<white>", prefix != null ? prefix : "", lore != null ? lore : List.of()));
        saveRarities();
    }

    public static void removeRarity(String name) {
        if (name == null) return;
        rarities.remove(name.toUpperCase(Locale.ROOT));
        saveRarities();
    }

    public static void reloadFromFile() {
        if (plugin != null) {
            load(plugin);
        }
    }

    public static File getRarityFile() {
        return rarityFile;
    }

    public static Map<String, RarityEntry> getAll() {
        return Collections.unmodifiableMap(rarities);
    }

    public record RarityEntry(String name, String color, String prefix, List<String> lore) {
    }
}
