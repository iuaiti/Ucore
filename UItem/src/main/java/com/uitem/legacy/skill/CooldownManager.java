package com.uitem.legacy.skill;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý cooldown cho skill của UItem.
 * 
 * - Dùng memory map nhanh cho runtime.
 * - Lưu last-used time vào player PDC để persist qua logout/restart (ổn định hơn nhiều plugin khác).
 * - Key trong PDC: uitem_cd_<itemId>_<skillId>
 */
public final class CooldownManager {

    private final NamespacedKey pluginKeyBase;

    // memory cache: playerUUID -> ( "itemId:skillId" -> lastUsedMillis )
    private final Map<UUID, Map<String, Long>> lastUsed = new ConcurrentHashMap<>();

    public CooldownManager(org.bukkit.plugin.Plugin plugin) {
        this.pluginKeyBase = new NamespacedKey(plugin, "uitem_cd");
    }

    private String makeKey(String itemId, String skillId) {
        return (itemId == null ? "null" : itemId.toLowerCase(Locale.ROOT)) + ":" + (skillId == null ? "null" : skillId.toLowerCase(Locale.ROOT));
    }

    private NamespacedKey getPdcKey(String itemId, String skillId) {
        // Giới hạn độ dài key (PDC key max ~256)
        String shortKey = "cd_" + safe(itemId) + "_" + safe(skillId);
        if (shortKey.length() > 80) shortKey = shortKey.substring(0, 80);
        return new NamespacedKey(pluginKeyBase.getNamespace(), shortKey);
    }

    private String safe(String s) {
        return s == null ? "x" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    /**
     * Kiểm tra xem skill có sẵn sàng (hết cooldown) không.
     * Trả về true nếu được dùng.
     */
    public boolean isReady(Player player, String itemId, String skillId, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return true;
        if (player == null) return true;

        long now = System.currentTimeMillis();
        long cdMillis = (long) (cooldownSeconds * 1000);

        Map<String, Long> playerMap = lastUsed.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        String key = makeKey(itemId, skillId);
        Long last = playerMap.get(key);

        // Nếu chưa có trong memory → thử load từ PDC (lần đầu join hoặc restart)
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

    /**
     * Trả về thời gian còn lại (giây), 0 nếu sẵn sàng.
     */
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

    /**
     * Đánh dấu skill vừa được dùng (bắt đầu cooldown).
     */
    public void markUsed(Player player, String itemId, String skillId) {
        if (player == null) return;
        long now = System.currentTimeMillis();

        Map<String, Long> playerMap = lastUsed.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        String key = makeKey(itemId, skillId);
        playerMap.put(key, now);

        // Lưu vào PDC để persist
        try {
            NamespacedKey pdcKey = getPdcKey(itemId, skillId);
            player.getPersistentDataContainer().set(pdcKey, PersistentDataType.LONG, now);
        } catch (Exception ignored) {}
    }

    /**
     * Xóa cooldown cho 1 skill (reset).
     */
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

    /**
     * Xóa toàn bộ cooldown của player (khi logout hoặc admin reset).
     */
    public void clearPlayer(Player player) {
        if (player == null) return;
        lastUsed.remove(player.getUniqueId());
        // Không xóa hết PDC (quá nặng + không cần thiết). PDC sẽ tự hết hạn theo thời gian.
    }

    /**
     * Dọn dẹp khi player quit.
     */
    public void onPlayerQuit(Player player) {
        clearPlayer(player);
    }
}
