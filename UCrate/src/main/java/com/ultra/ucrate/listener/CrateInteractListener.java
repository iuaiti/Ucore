package com.ultra.ucrate.listener;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.model.Crate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CrateInteractListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        var api = UCrateProvider.get();
        Crate crate = api.getCrateManager().getCrateAt(event.getClickedBlock().getLocation());

        if (crate == null) return;

        if (event.getPlayer().hasPermission("ucrate.admin")) return;

        event.setCancelled(true);
        api.openCrate(event.getPlayer(), crate);
    }
}
