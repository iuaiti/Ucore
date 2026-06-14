package com.ultra.ucrate.manager;

import com.ultra.ucrate.api.events.CrateRewardEvent;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public final class RewardManager {

    private final Random random = new Random();

    @Nullable
    public Reward rollReward(@NotNull Crate crate) {
        double total = crate.totalChance();
        if (total <= 0 || crate.rewards().isEmpty()) return null;

        double roll = random.nextDouble() * total;
        double cumulative = 0;

        for (Reward reward : crate.rewards()) {
            cumulative += reward.chance();
            if (roll <= cumulative) {
                return reward;
            }
        }

        return crate.rewards().getLast();
    }

    public boolean executeReward(@NotNull Player player, @NotNull Crate crate, @NotNull Reward reward) {
        CrateRewardEvent event = new CrateRewardEvent(crate, player, reward);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        for (String cmd : reward.commands()) {
            String parsed = cmd
                    .replace("%player%", player.getName())
                    .replace("%crate%", crate.id())
                    .replace("%reward%", reward.displayName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }

        if (reward.item() != null) {
            var leftovers = player.getInventory().addItem(reward.item().clone());
            for (var left : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }

        if (reward.money() > 0) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                var economy = Bukkit.getServicesManager().load(economyClass);
                if (economy != null) {
                    economy.getClass().getMethod("depositPlayer", Player.class, double.class)
                            .invoke(economy, player, reward.money());
                }
            } catch (Exception ignored) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "eco give " + player.getName() + " " + String.format("%.2f", reward.money()));
            }
        }

        for (String perm : reward.permissions()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    perm.replace("%player%", player.getName())
                            .replace("%crate%", crate.id())
                            .replace("%reward%", reward.displayName()));
        }

        var mm = MiniMessage.miniMessage();
        for (String msg : reward.messages()) {
            String parsed = msg.replace("%player%", player.getName())
                    .replace("%crate%", crate.id())
                    .replace("%reward%", reward.displayName());
            player.sendMessage(mm.deserialize(parsed));
        }

        for (String bc : reward.broadcasts()) {
            String parsed = bc
                    .replace("%player%", player.getName())
                    .replace("%crate%", crate.id())
                    .replace("%reward%", reward.displayName());
            Bukkit.broadcast(mm.deserialize(parsed));
        }

        return true;
    }
}
