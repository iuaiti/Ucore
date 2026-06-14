package com.ultra.ucrate.animation;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MysteryBoxAnimation implements CrateAnimation {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    @NotNull
    public String id() {
        return "mysterybox";
    }

    @Override
    @NotNull
    public String displayName() {
        return "Mystery Box";
    }

    @Override
    public void animate(@NotNull Player player, @NotNull Crate crate, @NotNull Reward reward, @NotNull Runnable onComplete) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 0.8f);

        var scheduler = UCoreAPI.getSchedulerService();

        scheduler.later(() -> {
            if (!player.isOnline()) return;
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);

            scheduler.later(() -> {
                if (!player.isOnline()) return;
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
                onComplete.run();
            }, 20);
        }, 10);
    }
}
