package com.ultra.uskill.xp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Theo dõi SP đã kiếm mỗi ngày theo từng nguồn.
 * Reset mỗi 24 giờ (hoặc khi server restart).
 */
public class DailyCapTracker {

    // playerId → (source → SP earned today)
    private final Map<UUID, Map<String, Integer>> earned = new ConcurrentHashMap<>();

    /**
     * Thử kiếm {@code amount} SP từ nguồn {@code source}.
     * Tự động cắt bớt nếu vượt daily cap.
     *
     * @param dailyCap giới hạn SP/ngày (<=0 = không giới hạn)
     * @return SP thực sự được trao (0 nếu đã đạt cap)
     */
    public int tryEarn(UUID playerId, String source, int amount, int dailyCap) {
        if (amount <= 0) return 0;
        if (dailyCap <= 0) return amount; // không giới hạn

        Map<String, Integer> playerMap = earned.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        int current = playerMap.getOrDefault(source, 0);
        if (current >= dailyCap) return 0;

        int actual = Math.min(amount, dailyCap - current);
        playerMap.put(source, current + actual);
        return actual;
    }

    /** SP đã kiếm hôm nay từ một nguồn cụ thể. */
    public int getEarned(UUID playerId, String source) {
        return earned.getOrDefault(playerId, Map.of()).getOrDefault(source, 0);
    }

    /** Tổng SP đã kiếm hôm nay từ tất cả nguồn. */
    public int getTotalEarned(UUID playerId) {
        return earned.getOrDefault(playerId, Map.of())
                .values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Reset tất cả player (chạy mỗi 24h). */
    public void resetAll() {
        earned.clear();
    }
}
