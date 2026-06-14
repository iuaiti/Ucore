package com.ultra.ucrate.api.events;

import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CrateRewardEvent extends UCrateEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Reward reward;
    private boolean cancelled;

    public CrateRewardEvent(@NotNull Crate crate, @NotNull Player player, @NotNull Reward reward) {
        super(crate);
        this.player = player;
        this.reward = reward;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Reward getReward() {
        return reward;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
