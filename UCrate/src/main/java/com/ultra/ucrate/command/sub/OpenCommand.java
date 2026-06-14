package com.ultra.ucrate.command.sub;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.command.UCrateSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OpenCommand implements UCrateSubCommand {

    @Override
    @NotNull
    public String name() {
        return "open";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.open";
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

        String crateId = args[0].toLowerCase();
        var api = UCrateProvider.get();
        api.openCrate(player, crateId);
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return UCrateProvider.get().getCrateManager().getAllCrates().stream()
                    .map(c -> c.id())
                    .filter(id -> id.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
