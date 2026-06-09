package com.ultra.ucore.command;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers plugin commands.
 */
public final class CommandManager {

    private final JavaPlugin plugin;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(String name, UCoreCommand root) {
        PluginCommand command = plugin.getCommand(name);

        if (command == null) {
            throw new IllegalStateException("Command not defined in plugin.yml: " + name);
        }

        command.setExecutor((sender, cmd, label, args) -> {
            root.execute(sender, args);
            return true;
        });

        command.setTabCompleter((sender, cmd, alias, args) ->
                root.tabComplete(sender, args)
        );
    }
}
