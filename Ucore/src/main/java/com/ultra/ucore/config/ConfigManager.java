package com.ultra.ucore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages YAML configuration files.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, ConfigFile> cache = new ConcurrentHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigFile load(String name) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File file = new File(plugin.getDataFolder(), name);

        if (!file.exists()) {
            plugin.saveResource(name, false);
        }

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigFile configFile = new ConfigFile(name, file, configuration);

        cache.put(name.toLowerCase(), configFile);

        return configFile;
    }

    public FileConfiguration get(String name) {
        return Optional.ofNullable(cache.get(name.toLowerCase()))
                .map(ConfigFile::configuration)
                .orElseThrow(() -> new IllegalArgumentException("Config not loaded: " + name));
    }

    public void reload(String name) {
        load(name);
    }

    public void reloadAll() {
        cache.keySet().forEach(this::load);
    }

    public Map<String, ConfigFile> loadedConfigs() {
        return Map.copyOf(cache);
    }
}
