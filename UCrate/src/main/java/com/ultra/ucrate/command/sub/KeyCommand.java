package com.ultra.ucrate.command.sub;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.command.UCrateSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KeyCommand implements UCrateSubCommand {

    private final Map<String, UCrateSubCommand> subCommands = new LinkedHashMap<>();

    public KeyCommand() {
        subCommands.put("give", new KeyGiveCommand());
        subCommands.put("take", new KeyTakeCommand());
        subCommands.put("physical", new KeyPhysicalCommand());
    }

    @Override
    @NotNull
    public String name() {
        return "key";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.admin";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            UCoreAPI.getMessageManager().send(sender, "general.unknown-command");
            return;
        }

        UCrateSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            UCoreAPI.getMessageManager().send(sender, "general.unknown-command");
            return;
        }

        if (!sender.hasPermission(sub.permission())) {
            UCoreAPI.getMessageManager().send(sender, "general.no-permission");
            return;
        }

        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        UCrateSubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) return List.of();
        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
