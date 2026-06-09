package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired khi player nhặt một UItem từ dưới đất.
 *
 * <p>Cancel để ngăn player nhặt item (item vẫn còn trên đất).
 *
 * <pre>{@code
 * @EventHandler
 * public void onPickup(UItemPickupEvent event) {
 *     // Chỉ owner mới nhặt được item drop của họ
 *     if (!canPickup(event.getPlayer(), event.getItemData())) {
 *         event.setCancelled(true);
 *     }
 * }
 * }</pre>
 */
public class UItemPickupEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player player;

    @NotNull
    private final Item itemEntity;

    public UItemPickupEvent(@NotNull UItemData itemData, @NotNull Player player, @NotNull Item itemEntity) {
        super(itemData);
        this.player = player;
        this.itemEntity = itemEntity;
    }

    /** Player đang nhặt item. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** Item entity dưới đất. */
    @NotNull
    public Item getItemEntity() {
        return itemEntity;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
