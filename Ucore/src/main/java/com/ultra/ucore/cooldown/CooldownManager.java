package com.ultra.ucore.cooldown;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles per-player and global cooldowns.
 */
public final class CooldownManager {

    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();

    public void setCooldown(Player player, String key, Duration duration) {
        playerCooldowns
                .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(normalize(key), System.currentTimeMillis() + duration.toMillis());
    }

    public boolean hasCooldown(Player player, String key) {
        return getRemaining(player, key).toMillis() > 0;
    }

    public Duration getRemaining(Player player, String key) {
        Long expiry = playerCooldowns
                .getOrDefault(player.getUniqueId(), Map.of())
                .get(normalize(key));

        if (expiry == null) {
            return Duration.ZERO;
        }

        long remaining = expiry - System.currentTimeMillis();
        return remaining <= 0 ? Duration.ZERO : Duration.ofMillis(remaining);
    }

    public void setGlobalCooldown(String key, Duration duration) {
        globalCooldowns.put(normalize(key), System.currentTimeMillis() + duration.toMillis());
    }

    public boolean hasGlobalCooldown(String key) {
        Long expiry = globalCooldowns.get(normalize(key));

        if (expiry == null) {
            return false;
        }

        return expiry > System.currentTimeMillis();
    }

    public void clearPlayer(UUID uuid) {
        playerCooldowns.remove(uuid);
    }

    public void cleanup() {
        long now = System.currentTimeMillis();

        playerCooldowns.values().forEach(map ->
                map.entrySet().removeIf(entry -> entry.getValue() <= now)
        );

        globalCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private String normalize(String key) {
        return key.toLowerCase();
    }
}
