package com.ultra.umenu.command;

import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.menu.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class MenuCommand implements CommandExecutor, TabCompleter {

    private final UMenuPlugin plugin;
    private final MenuManager menuManager;

    public MenuCommand(UMenuPlugin plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("umenu.reload")) {
                sender.sendMessage("§cYou do not have permission to reload UMenu.");
                return true;
            }
            plugin.getConfigManager().reloadAll();
            menuManager.reload();
            plugin.reloadConfig();
            sender.sendMessage("§aUMenu has been reloaded.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("umenu.use")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length > 0) {
            menuManager.open(player, args[0]);
        } else {
            menuManager.openMain(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "main", "profile", "quest", "boss", "crates", "warp", "shop", "collection", "settings", "server-info");
        }
        return List.of();
    }
}
