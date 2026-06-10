package com.ultra.uskill.command;

import com.ultra.ucore.util.TextUtil;
import com.ultra.uskill.USkillPlugin;
import com.ultra.uskill.tree.SkillTreeManager;
import com.ultra.uskill.tree.gui.SkillTreeGui;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkillTreeCommand implements CommandExecutor, TabCompleter {

    private final USkillPlugin plugin;
    private final SkillTreeManager manager;

    public SkillTreeCommand(USkillPlugin plugin, SkillTreeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can open skill tree!");
                return true;
            }
            new SkillTreeGui(manager).open(player);
            return true;
        }

        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "open" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can open skill tree!");
                    return true;
                }
                Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : player;
                if (target == null) {
                    sender.sendMessage(TextUtil.parse("<red>Player not found!</red>"));
                    return true;
                }
                new SkillTreeGui(manager).open(target);
                if (!target.equals(sender)) {
                    sender.sendMessage(TextUtil.parse("<green>Opened skill tree for " + target.getName() + "</green>"));
                }
            }

            case "points", "sp" -> {
                if (!sender.hasPermission("uskill.admin")) {
                    sender.sendMessage(TextUtil.parse("<red>No permission!</red>"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(TextUtil.parse("<red>Usage: /skilltree points <player> <amount></red>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(TextUtil.parse("<red>Player not found!</red>"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    manager.givePoints(target, amount);
                    sender.sendMessage(TextUtil.parse("<green>Gave " + amount + " SP to " + target.getName() + "</green>"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(TextUtil.parse("<red>Invalid amount!</red>"));
                }
            }

            case "reset" -> {
                if (!sender.hasPermission("uskill.admin")) {
                    sender.sendMessage(TextUtil.parse("<red>No permission!</red>"));
                    return true;
                }
                Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    sender.sendMessage(TextUtil.parse("<red>Player not found!</red>"));
                    return true;
                }
                manager.resetAll(target);
                sender.sendMessage(TextUtil.parse("<green>Reset all skills for " + target.getName() + "</green>"));
            }

            case "reload" -> {
                if (!sender.hasPermission("uskill.admin")) {
                    sender.sendMessage(TextUtil.parse("<red>No permission!</red>"));
                    return true;
                }
                manager.reload();
                sender.sendMessage(TextUtil.parse("<green>Skill tree reloaded!</green>"));
            }

            case "giveall" -> {
                if (!sender.hasPermission("uskill.admin")) {
                    sender.sendMessage(TextUtil.parse("<red>No permission!</red>"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(TextUtil.parse("<red>Usage: /skilltree giveall <amount></red>"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    manager.givePointsAll(amount);
                    sender.sendMessage(TextUtil.parse("<green>Gave " + amount + " SP to all online players!</green>"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(TextUtil.parse("<red>Invalid amount!</red>"));
                }
            }

            default -> {
                sender.sendMessage(TextUtil.parse("<red>Unknown subcommand!</red>"));
                sender.sendMessage(TextUtil.parse("<yellow>/skilltree <open|points|reset|reload|giveall></yellow>"));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("open", "points", "reset", "reload", "giveall")
                    .filter(s -> s.startsWith(args[0].toLowerCase(java.util.Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("points") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("open"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase(java.util.Locale.ROOT).startsWith(args[1].toLowerCase(java.util.Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
