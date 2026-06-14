package com.ultra.ucrate.manager;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheManager {

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean hasCooldown(@NotNull Player player, @NotNull String crateId) {
        Long expiry = cooldowns.get(key(player, crateId));
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public long getCooldownSeconds(@NotNull Player player, @NotNull String crateId) {
        Long expiry = cooldowns.get(key(player, crateId));
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public void setCooldown(@NotNull Player player, @NotNull String crateId, int seconds) {
        if (seconds <= 0) return;
        cooldowns.put(key(player, crateId), System.currentTimeMillis() + (seconds * 1000L));
    }

    public void removeCooldown(@NotNull Player player, @NotNull String crateId) {
        cooldowns.remove(key(player, crateId));
    }

    public void removePlayer(@NotNull UUID playerId) {
        cooldowns.keySet().removeIf(k -> k.startsWith(playerId.toString() + ":"));
    }

    private static String key(@NotNull Player player, @NotNull String crateId) {
        return player.getUniqueId() + ":" + crateId.toLowerCase();
    }
}
