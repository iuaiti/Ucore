package com.ultra.umenu.manager;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CollectionManager {

    private final Map<UUID, PlayerCollectionData> cache = new ConcurrentHashMap<>();
    private final List<CollectionCategory> categories = new CopyOnWriteArrayList<>();

    public CollectionManager() {
    }

    public void registerCategory(CollectionCategory category) {
        categories.add(category);
    }

    public List<CollectionCategory> getCategories() {
        return List.copyOf(categories);
    }

    public long getValue(Player player, String categoryId) {
        return cache
                .computeIfAbsent(player.getUniqueId(), id -> new PlayerCollectionData())
                .values().getOrDefault(categoryId, 0L);
    }

    public void setValue(Player player, String categoryId, long value) {
        cache
                .computeIfAbsent(player.getUniqueId(), id -> new PlayerCollectionData())
                .values().put(categoryId, value);
    }

    public void clearCache(UUID uuid) {
        cache.remove(uuid);
    }

    public record CollectionCategory(String id, String name, String icon, String unit) {
    }

    public record PlayerCollectionData(Map<String, Long> values) {
        public PlayerCollectionData() {
            this(new ConcurrentHashMap<>());
        }
    }
}
