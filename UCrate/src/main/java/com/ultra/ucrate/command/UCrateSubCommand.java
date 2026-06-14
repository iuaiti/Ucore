package com.ultra.ucrate.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface UCrateSubCommand {

    @NotNull
    String name();

    @NotNull
    String permission();

    void execute(@NotNull CommandSender sender, @NotNull String[] args);

    @NotNull
    default List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
