package com.ultra.ucrate.command.sub;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.command.UCrateSubCommand;
import com.ultra.ucrate.gui.CrateListMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class EditorCommand implements UCrateSubCommand {

    @Override
    @NotNull
    public String name() {
        return "editor";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.admin";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            UCoreAPI.getMessageManager().send(sender, "general.player-only");
            return;
        }

        new CrateListMenu().open(player);
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
