package com.ultra.ucrate.command.sub;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.command.UCrateSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReloadCommand implements UCrateSubCommand {

    @Override
    @NotNull
    public String name() {
        return "reload";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.admin";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        UCrateProvider.get().reload();
        UCoreAPI.getMessageManager().send(sender, "general.reloaded");
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
