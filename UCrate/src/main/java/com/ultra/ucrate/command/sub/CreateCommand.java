package com.ultra.ucrate.command.sub;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.command.UCrateSubCommand;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.CrateLocation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CreateCommand implements UCrateSubCommand {

    @Override
    @NotNull
    public String name() {
        return "create";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.admin";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            UCoreAPI.getMessageManager().send(sender, "general.player-only");
            return;
        }

        if (args.length == 0) {
            UCoreAPI.getMessageManager().send(sender, "general.unknown-command");
            return;
        }

        String id = args[0].toLowerCase();
        var api = UCrateProvider.get();
        var crateManager = api.getCrateManager();

        if (crateManager.crateExists(id)) {
            UCoreAPI.getMessageManager().send(sender, "crate.exists");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.isPassable()) {
            UCoreAPI.getMessageManager().send(sender, "crate.no-location");
            return;
        }

        Block placeBlock = targetBlock.getRelative(player.getFacing());
        if (!placeBlock.isPassable() || placeBlock.getType() != Material.AIR) {
            UCoreAPI.getMessageManager().send(sender, "crate.no-location");
            return;
        }

        Material crateMaterial = Material.valueOf(
                api.getPlugin().getConfig().getString("settings.crate-block-material", "ENDER_CHEST")
        );
        placeBlock.setType(crateMaterial);

        Crate crate = new Crate(id);
        crate.location(new CrateLocation(placeBlock.getLocation()));
        crate.displayItem(new org.bukkit.inventory.ItemStack(crateMaterial));
        crateManager.addCrate(crate);

        UCoreAPI.getMessageManager().send(sender, "crate.created");
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of("<crate_id>");
    }
}
