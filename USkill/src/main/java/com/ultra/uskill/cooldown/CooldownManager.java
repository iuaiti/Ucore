package com.ultra.uskill.cooldown;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final NamespacedKey pluginKeyBase;
    private final Map<UUID, Map<String, Long>> lastUsed = new ConcurrentHashMap<>();

    public CooldownManager(Plugin plugin) {
        this.pluginKeyBase = new NamespacedKey(plugin, "uskill_cd");
    }

    private String makeKey(String itemId, String skillId) {
        return (itemId == null ? "null" : itemId.toLowerCase(Locale.ROOT)) + ":" + (skillId == null ? "null" : skillId.toLowerCase(Locale.ROOT));
    }

    private NamespacedKey getPdcKey(String itemId, String skillId) {
        String shortKey = "cd_" + safe(itemId) + "_" + safe(skillId);
        if (shortKey.length() > 80) shortKey = shortKey.substring(0, 80);
        return new NamespacedKey(pluginKeyBase.getNamespace(), shortKey);
    }

    private String safe(String s) {
        return s == null ? "x" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    public boolean isReady(Player player, String itemId, String skillId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return true;
        if (player == null) return true;

        long now = System.currentTimeMillis();
        long cdMillis = (long) (cooldownSeconds * 1000);

        Map<String, Long> playerMap = lastUsed.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        String key = makeKey(itemId, skillId);
        Long last = playerMap.get(key);

        if (last == null) {
            try {
                NamespacedKey pdcKey = getPdcKey(itemId, skillId);
                if (player.getPersistentDataContainer().has(pdcKey, PersistentDataType.LONG)) {
                    last = player.getPersistentDataContainer().get(pdcKey, PersistentDataType.LONG);
                    if (last != null) playerMap.put(key, last);
                }
            } catch (Exception ignored) {}
        }

        if (last == null) return true;
        return (now - last) >= cdMillis;
    }

    public double getRemainingSeconds(Player player, String itemId, String skillId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return 0;
        if (player == null) return 0;

        long now = System.currentTimeMillis();
        long cdMillis = (long) (cooldownSeconds * 1000);

        Map<String, Long> playerMap = lastUsed.get(player.getUniqueId());
        String key = makeKey(itemId, skillId);
        Long last = (playerMap != null) ? playerMap.get(key) : null;

        if (last == null) {
            try {
                NamespacedKey pdcKey = getPdcKey(itemId, skillId);
                if (player.getPersistentDataContainer().has(pdcKey, PersistentDataType.LONG)) {
                    last = player.getPersistentDataContainer().get(pdcKey, PersistentDataType.LONG);
                }
            } catch (Exception ignored) {}
        }

        if (last == null) return 0;
        long elapsed = now - last;
        if (elapsed >= cdMillis) return 0;
        return (cdMillis - elapsed) / 1000.0;
    }

    public void markUsed(Player player, String itemId, String skillId) {
        if (player == null) return;
        long now = System.currentTimeMillis();

        Map<String, Long> playerMap = lastUsed.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        String key = makeKey(itemId, skillId);
        playerMap.put(key, now);

        try {
            NamespacedKey pdcKey = getPdcKey(itemId, skillId);
            player.getPersistentDataContainer().set(pdcKey, PersistentDataType.LONG, now);
        } catch (Exception ignored) {}
    }

    public void reset(Player player, String itemId, String skillId) {
        if (player == null) return;
        Map<String, Long> playerMap = lastUsed.get(player.getUniqueId());
        if (playerMap != null) {
            playerMap.remove(makeKey(itemId, skillId));
        }
        try {
            NamespacedKey pdcKey = getPdcKey(itemId, skillId);
            player.getPersistentDataContainer().remove(pdcKey);
        } catch (Exception ignored) {}
    }

    public void clearPlayer(Player player) {
        if (player == null) return;
        lastUsed.remove(player.getUniqueId());
    }

    public void onPlayerQuit(Player player) {
        clearPlayer(player);
    }
}
