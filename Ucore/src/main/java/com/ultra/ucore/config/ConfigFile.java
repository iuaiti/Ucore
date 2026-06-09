package com.ultra.ucore.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * Represents a loaded YAML configuration file.
 */
public record ConfigFile(
        String name,
        File file,
        FileConfiguration configuration
) {
}
