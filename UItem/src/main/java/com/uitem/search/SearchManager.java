package com.uitem.search;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per‑player search queries for the item list GUI.
 * Uses a thread‑safe map because chat input callbacks may be executed
 * asynchronously. All methods are lightweight and safe to call from any
 * thread.
 */
public class SearchManager {
    private final Map<UUID, String> queries = new ConcurrentHashMap<>();

    /** Set the current search query for a player. */
    public void setQuery(Player player, String query) {
        if (query == null || query.isBlank()) {
            queries.remove(player.getUniqueId());
        } else {
            queries.put(player.getUniqueId(), query.toLowerCase());
        }
    }

    /** Clear the stored query for a player. */
    public void clearQuery(Player player) {
        queries.remove(player.getUniqueId());
    }

    /** Get the current query for a player, or null if none. */
    public String getQuery(Player player) {
        return queries.get(player.getUniqueId());
    }
}
