package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base class cho tất cả UItem events.
 *
 * <p>External developer dùng như sau:
 * <pre>{@code
 * @EventHandler
 * public void onUItemGive(UItemGiveEvent event) {
 *     UItemData data = event.getItemData();
 *     // xử lý...
 * }
 * }</pre>
 */
public abstract class UItemEvent extends Event {

    private final UItemData itemData;

    protected UItemEvent(@NotNull UItemData itemData) {
        this.itemData = itemData;
    }

    protected UItemEvent(@NotNull UItemData itemData, boolean async) {
        super(async);
        this.itemData = itemData;
    }

    /**
     * Lấy dữ liệu của custom item liên quan đến event này.
     */
    @NotNull
    public UItemData getItemData() {
        return itemData;
    }
}
