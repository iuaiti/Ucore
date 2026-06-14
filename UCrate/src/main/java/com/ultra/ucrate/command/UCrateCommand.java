package com.ultra.ucrate.command;

import com.ultra.ucore.api.UCoreAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UCrateCommand implements CommandExecutor, TabCompleter {

    private final Map<String, UCrateSubCommand> subCommands = new LinkedHashMap<>();

    public UCrateCommand addSubCommand(UCrateSubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(), subCommand);
        return this;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            var api = UCoreAPI.getMessageManager();
            if (sender.hasPermission("ucrate.admin")) {
                api.send(sender, "general.help");
            } else {
                api.send(sender, "general.unknown-command");
            }
            return true;
        }

        UCrateSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            UCoreAPI.getMessageManager().send(sender, "general.unknown-command");
            return true;
        }

        if (!sender.hasPermission(sub.permission())) {
            UCoreAPI.getMessageManager().send(sender, "general.no-permission");
            return true;
        }

        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return subCommands.values().stream()
                    .filter(sub -> sender.hasPermission(sub.permission()))
                    .map(UCrateSubCommand::name)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        UCrateSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) return List.of();

        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
