package com.ultra.ucore.command;

import com.ultra.ucore.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root command dispatcher for /ucore.
 */
public final class UCoreCommand {

    private final MessageManager messageManager;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public UCoreCommand(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    public UCoreCommand addSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(), subCommand);
        return this;
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messageManager.send(sender, "command.help");
            return;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());

        if (subCommand == null) {
            messageManager.send(sender, "errors.unknown-command");
            return;
        }

        if (!sender.hasPermission(subCommand.permission())) {
            messageManager.send(sender, "errors.no-permission");
            return;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return subCommands.values().stream()
                    .filter(sub -> sender.hasPermission(sub.permission()))
                    .map(SubCommand::name)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());

        if (subCommand == null) {
            return List.of();
        }

        return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
