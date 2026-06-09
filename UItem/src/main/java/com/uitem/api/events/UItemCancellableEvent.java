package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Base class cho các UItem events có thể bị cancel.
 * Cancel event sẽ ngăn hành động tương ứng xảy ra.
 */
public abstract class UItemCancellableEvent extends UItemEvent implements Cancellable {

    private boolean cancelled;

    protected UItemCancellableEvent(@NotNull UItemData itemData) {
        super(itemData);
        this.cancelled = false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
