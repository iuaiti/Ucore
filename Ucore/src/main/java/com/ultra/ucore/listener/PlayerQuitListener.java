package com.ultra.ucore.listener;

import com.ultra.ucore.cooldown.CooldownManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans player-scoped runtime data.
 */
public final class PlayerQuitListener implements Listener {

    private final CooldownManager cooldownManager;

    public PlayerQuitListener(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldownManager.clearPlayer(event.getPlayer().getUniqueId());
    }
}
