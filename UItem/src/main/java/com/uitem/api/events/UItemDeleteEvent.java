package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired khi một custom item bị xóa khỏi UItem.
 *
 * <p>Cancel event này để ngăn item bị xóa.
 *
 * <pre>{@code
 * @EventHandler
 * public void onDelete(UItemDeleteEvent event) {
 *     if (event.getItemData().getId().equals("legendary_sword")) {
 *         event.setCancelled(true); // bảo vệ item không cho xóa
 *         if (event.getDeleter() != null) {
 *             event.getDeleter().sendMessage("Item này được bảo vệ!");
 *         }
 *     }
 * }
 * }</pre>
 */
public class UItemDeleteEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @Nullable
    private final CommandSender deleter;

    /**
     * @param itemData Dữ liệu item sắp bị xóa
     * @param deleter  Người xóa item (null nếu xóa qua internal API)
     */
    public UItemDeleteEvent(@NotNull UItemData itemData, @Nullable CommandSender deleter) {
        super(itemData);
        this.deleter = deleter;
    }

    /**
     * Ai trigger việc xóa item này.
     * null nếu được xóa bởi plugin hoặc qua {@code UItemAPI}.
     */
    @Nullable
    public CommandSender getDeleter() {
        return deleter;
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
