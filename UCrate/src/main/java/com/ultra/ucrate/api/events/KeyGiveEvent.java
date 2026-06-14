package com.ultra.ucrate.api.events;

import com.ultra.ucrate.model.KeyType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyGiveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player recipient;
    private final String crateId;
    private final KeyType keyType;
    private final CommandSender giver;
    private int amount;
    private boolean cancelled;

    public KeyGiveEvent(
            @NotNull Player recipient,
            @NotNull String crateId,
            @NotNull KeyType keyType,
            int amount,
            @Nullable CommandSender giver
    ) {
        this.recipient = recipient;
        this.crateId = crateId;
        this.keyType = keyType;
        this.amount = Math.max(1, amount);
        this.giver = giver;
    }

    @NotNull
    public Player getRecipient() {
        return recipient;
    }

    @NotNull
    public String getCrateId() {
        return crateId;
    }

    @NotNull
    public KeyType getKeyType() {
        return keyType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    @Nullable
    public CommandSender getGiver() {
        return giver;
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
