package com.ultra.ucrate.animation;

import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class InstantAnimation implements CrateAnimation {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    @NotNull
    public String id() {
        return "instant";
    }

    @Override
    @NotNull
    public String displayName() {
        return "Instant";
    }

    @Override
    public void animate(@NotNull Player player, @NotNull Crate crate, @NotNull Reward reward, @NotNull Runnable onComplete) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(MM.deserialize(
                "<gradient:#FF6B35:#FFD700>You won: </gradient>" + reward.displayName()
        ));
        onComplete.run();
    }
}
