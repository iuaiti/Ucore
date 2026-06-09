package com.uitem.command;

import com.uitem.UItemPlugin;
import com.uitem.gui.CraftingGui;
import com.uitem.gui.RecipeListGui;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CraftingCommand implements CommandExecutor {

    private final UItemPlugin plugin;

    public CraftingCommand(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.error(Messages.playersOnly()));
            return true;
        }

        if (!player.hasPermission("uitem.craft")) {
            player.sendMessage(TextUtil.error(Messages.noPermission()));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("craft")) {
            new CraftingGui(plugin, player).open();
        } else {
            new RecipeListGui(plugin, player).open();
        }
        return true;
    }
}
