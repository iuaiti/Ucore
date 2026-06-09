package com.ultra.umenu.manager;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SettingsManager {

    private final Map<UUID, PlayerSettings> settings = new ConcurrentHashMap<>();

    public SettingsManager() {
    }

    public boolean hasSoundEnabled(Player player) {
        return settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings()).sound();
    }

    public void toggleSound(Player player) {
        PlayerSettings current = settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings());
        settings.put(player.getUniqueId(), new PlayerSettings(
                !current.sound(), current.particles(), current.privateMessages(), current.notifications()
        ));
    }

    public boolean hasParticlesEnabled(Player player) {
        return settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings()).particles();
    }

    public void toggleParticles(Player player) {
        PlayerSettings current = settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings());
        settings.put(player.getUniqueId(), new PlayerSettings(
                current.sound(), !current.particles(), current.privateMessages(), current.notifications()
        ));
    }

    public boolean hasPrivateMessagesEnabled(Player player) {
        return settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings()).privateMessages();
    }

    public void togglePrivateMessages(Player player) {
        PlayerSettings current = settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings());
        settings.put(player.getUniqueId(), new PlayerSettings(
                current.sound(), current.particles(), !current.privateMessages(), current.notifications()
        ));
    }

    public boolean hasNotificationsEnabled(Player player) {
        return settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings()).notifications();
    }

    public void toggleNotifications(Player player) {
        PlayerSettings current = settings.computeIfAbsent(player.getUniqueId(), id -> new PlayerSettings());
        settings.put(player.getUniqueId(), new PlayerSettings(
                current.sound(), current.particles(), current.privateMessages(), !current.notifications()
        ));
    }

    public void clearCache(UUID uuid) {
        settings.remove(uuid);
    }

    public record PlayerSettings(
            boolean sound,
            boolean particles,
            boolean privateMessages,
            boolean notifications
    ) {
        public PlayerSettings() {
            this(true, true, true, true);
        }
    }
}
