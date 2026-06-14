package com.ultra.ucrate.command.sub;

import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.command.UCrateSubCommand;
import com.ultra.ucrate.model.Crate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ListCommand implements UCrateSubCommand {

    @Override
    @NotNull
    public String name() {
        return "list";
    }

    @Override
    @NotNull
    public String permission() {
        return "ucrate.admin";
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        var crates = UCrateProvider.get().getCrateManager().getAllCrates();

        sender.sendMessage(Component.text("── UCrate List ──", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Total: " + crates.size(), NamedTextColor.GRAY));

        for (Crate crate : crates) {
            String loc = crate.location() != null
                    ? crate.location().worldName() + " @ " + (int) crate.location().x() + "," + (int) crate.location().y() + "," + (int) crate.location().z()
                    : "none";
            sender.sendMessage(Component.text("  • " + crate.id(), NamedTextColor.AQUA)
                    .append(Component.text(" (type: " + crate.type().name() + ", rewards: " + crate.rewards().size() + ", loc: " + loc + ")", NamedTextColor.GRAY)));
        }
    }

    @Override
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of();
    }
}
