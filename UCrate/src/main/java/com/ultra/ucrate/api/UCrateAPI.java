package com.ultra.ucrate.api;

import com.ultra.ucrate.animation.CrateAnimation;
import com.ultra.ucrate.manager.AnimationManager;
import com.ultra.ucrate.manager.CrateManager;
import com.ultra.ucrate.manager.KeyManager;
import com.ultra.ucrate.manager.RewardManager;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface UCrateAPI {

    @NotNull
    CrateManager getCrateManager();

    @NotNull
    KeyManager getKeyManager();

    @NotNull
    RewardManager getRewardManager();

    @NotNull
    AnimationManager getAnimationManager();

    void openCrate(@NotNull Player player, @NotNull Crate crate);

    void openCrate(@NotNull Player player, @NotNull String crateId);

    void registerAnimation(@NotNull CrateAnimation animation);

    boolean giveKey(@NotNull Player player, @NotNull String crateId, int amount);

    boolean takeKey(@NotNull Player player, @NotNull String crateId, int amount);

    int getKeyCount(@NotNull Player player, @NotNull String crateId);

    @NotNull
    Optional<Crate> getCrate(@NotNull String crateId);

    @Nullable
    Reward rollReward(@NotNull Crate crate);
}
