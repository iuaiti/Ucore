package com.ultra.ucrate.command.sub;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.command.UCrateSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class KeyGiveCommand implements UCrateSubCommand {

    @Override
    @NotNull
    public String name() {
        return "give";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.admin";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            UCoreAPI.getMessageManager().send(sender, "general.unknown-command");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            UCoreAPI.getMessageManager().send(sender, "general.not-found");
            return;
        }

        String crateId = args[1].toLowerCase();
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                UCoreAPI.getMessageManager().send(sender, "general.invalid-number");
                return;
            }
        }

        var api = UCrateProvider.get();
        if (api.getCrateManager().getCrate(crateId) == null) {
            UCoreAPI.getMessageManager().send(sender, "crate.not-found");
            return;
        }

        boolean success = api.getKeyManager().giveVirtualKeys(target, crateId, amount, sender);
        if (success) {
            UCoreAPI.getMessageManager().send(sender, "key.given");
            UCoreAPI.getMessageManager().send(target, "key.received");
        }
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return null;
        }
        if (args.length == 2) {
            return UCrateProvider.get().getCrateManager().getAllCrates().stream()
                    .map(c -> c.id())
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 3) {
            return List.of("1", "10", "64");
        }
        return List.of();
    }
}
