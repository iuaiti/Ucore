package com.ultra.umenu.manager;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestManager {

    private final Map<UUID, PlayerQuestData> cache = new ConcurrentHashMap<>();

    public QuestManager() {
    }

    public List<Quest> getDailyQuests(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerQuestData()).dailyQuests();
    }

    public List<Quest> getWeeklyQuests(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerQuestData()).weeklyQuests();
    }

    public double getProgress(Player player, Quest quest) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerQuestData())
                .progress().getOrDefault(quest.id(), 0.0);
    }

    public void clearCache(UUID uuid) {
        cache.remove(uuid);
    }

    public record Quest(String id, String name, String description, double target, String reward) {
    }

    public record PlayerQuestData(
            List<Quest> dailyQuests,
            List<Quest> weeklyQuests,
            Map<String, Double> progress
    ) {
        public PlayerQuestData() {
            this(List.of(), List.of(), new ConcurrentHashMap<>());
        }
    }
}
