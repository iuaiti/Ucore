package com.ultra.umenu.manager;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossManager {

    private final Map<String, Boss> bosses = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public BossManager() {
    }

    public void registerBoss(Boss boss) {
        bosses.put(boss.id(), boss);
    }

    public List<Boss> getBosses() {
        return List.copyOf(bosses.values());
    }

    public long getCooldown(Player player, String bossId) {
        return cooldowns
                .getOrDefault(player.getUniqueId(), Map.of())
                .getOrDefault(bossId, 0L);
    }

    public boolean isOnCooldown(Player player, String bossId) {
        return System.currentTimeMillis() < getCooldown(player, bossId);
    }

    public void setCooldown(Player player, String bossId, long durationMs) {
        cooldowns
                .computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .put(bossId, System.currentTimeMillis() + durationMs);
    }

    public record Boss(String id, String name, String world, int x, int y, int z, String reward, long cooldownMs) {
    }
}
