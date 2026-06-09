package com.ultra.ucore.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Represents a subcommand.
 */
public interface SubCommand {

    String name();

    String permission();

    void execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
