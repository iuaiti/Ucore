package com.ultra.umenu.manager;

import com.ultra.ucore.api.UCoreAPI;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProfileManager {

    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public ProfileManager() {
    }

    public double getBalance(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile()).balance();
    }

    public String getRank(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile()).rank();
    }

    public String getPlaytime(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile()).playtime();
    }

    public int getKills(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile()).kills();
    }

    public int getDeaths(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile()).deaths();
    }

    public int getLevel(Player player) {
        return player.getLevel();
    }

    public PlayerProfile getProfile(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile());
    }

    public void loadProfile(Player player) {
        UCoreAPI.getSchedulerService().async(() -> {
            PlayerProfile profile = new PlayerProfile(
                    0.0, "Player", "0h 0m", 0, 0
            );
            cache.put(player.getUniqueId(), profile);
        });
    }

    public void clearCache(UUID uuid) {
        cache.remove(uuid);
    }

    public record PlayerProfile(
            double balance,
            String rank,
            String playtime,
            int kills,
            int deaths
    ) {
        public PlayerProfile() {
            this(0.0, "Player", "0h 0m", 0, 0);
        }
    }
}
