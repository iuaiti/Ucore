package com.ultra.umenu.module;

import com.ultra.umenu.UMenuPlugin;
import org.bukkit.plugin.PluginManager;

import java.util.EnumMap;
import java.util.Map;

public final class ModuleDetector {

    private final UMenuPlugin plugin;
    private final Map<ModuleType, Boolean> detected = new EnumMap<>(ModuleType.class);

    public ModuleDetector(UMenuPlugin plugin) {
        this.plugin = plugin;
    }

    public void detect() {
        PluginManager pm = plugin.getServer().getPluginManager();

        for (ModuleType type : ModuleType.values()) {
            boolean enabled = pm.isPluginEnabled(type.pluginName());
            detected.put(type, enabled);
            if (enabled) {
                plugin.getLogger().info("Detected module: " + type.pluginName());
            }
        }
    }

    public boolean isPresent(ModuleType type) {
        return detected.getOrDefault(type, false);
    }

    public boolean anyPresent() {
        return detected.values().stream().anyMatch(v -> v);
    }
}
