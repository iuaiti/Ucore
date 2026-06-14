package com.ultra.ucrate.animation;

import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import com.ultra.ucore.api.UCoreAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RouletteAnimation implements CrateAnimation {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    @NotNull
    public String id() {
        return "roulette";
    }

    @Override
    @NotNull
    public String displayName() {
        return "Roulette";
    }

    @Override
    public void animate(@NotNull Player player, @NotNull Crate crate, @NotNull Reward reward, @NotNull Runnable onComplete) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        var scheduler = UCoreAPI.getSchedulerService();

        scheduler.later(() -> {
            if (!player.isOnline()) return;
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.sendMessage(MM.deserialize(
                    "<gradient:#FF6B35:#FFD700> You won: </gradient>" + reward.displayName()
            ));
            onComplete.run();
        }, 15);
    }
}
