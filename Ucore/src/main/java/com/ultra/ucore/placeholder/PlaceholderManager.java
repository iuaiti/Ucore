package com.ultra.ucore.placeholder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal placeholder registry.
 */
public final class PlaceholderManager {

    private final Map<String, Placeholder> placeholders = new ConcurrentHashMap<>();

    public void register(String key, Placeholder placeholder) {
        placeholders.put(normalize(key), placeholder);
    }

    public void unregister(String key) {
        placeholders.remove(normalize(key));
    }

    public String apply(String input, PlaceholderContext context) {
        String result = input;

        for (Map.Entry<String, Placeholder> entry : placeholders.entrySet()) {
            result = result.replace(
                    "%" + entry.getKey() + "%",
                    entry.getValue().resolve(context)
            );
        }

        return result;
    }

    private String normalize(String key) {
        return key.toLowerCase().replace("%", "");
    }

    public record PlaceholderContext(Player player, CommandSender sender) {
    }
}
