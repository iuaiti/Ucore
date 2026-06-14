package com.ultra.ucrate.animation;

import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface CrateAnimation {

    @NotNull
    String id();

    @NotNull
    String displayName();

    void animate(@NotNull Player player, @NotNull Crate crate, @NotNull Reward reward, @NotNull Runnable onComplete);
}
