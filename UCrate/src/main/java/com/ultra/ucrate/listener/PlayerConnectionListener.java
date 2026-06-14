package com.ultra.ucrate.listener;

import com.ultra.ucrate.UCrateProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var provider = UCrateProvider.getInstance();
        if (provider == null) return;

        var storage = provider.getStorageProvider();
        if (storage instanceof com.ultra.ucrate.storage.YamlStorageProvider yaml) {
            yaml.unloadPlayer(event.getPlayer().getUniqueId());
        }

        provider.getCacheManager().removePlayer(event.getPlayer().getUniqueId());
    }
}
