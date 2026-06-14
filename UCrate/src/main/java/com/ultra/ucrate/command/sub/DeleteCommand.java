package com.ultra.ucrate.command.sub;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.command.UCrateSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DeleteCommand implements UCrateSubCommand {

    @Override
    @NotNull
    public String name() {
        return "delete";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.admin";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            UCoreAPI.getMessageManager().send(sender, "crate.not-found");
            return;
        }

        String id = args[0].toLowerCase();
        var api = UCrateProvider.get();
        var crateManager = api.getCrateManager();

        if (!crateManager.crateExists(id)) {
            UCoreAPI.getMessageManager().send(sender, "crate.not-found");
            return;
        }

        crateManager.removeCrate(id);
        UCoreAPI.getMessageManager().send(sender, "crate.deleted");
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
