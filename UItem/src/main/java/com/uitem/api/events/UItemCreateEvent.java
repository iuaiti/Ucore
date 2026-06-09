package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired khi một custom item mới được tạo trong UItem.
 *
 * <p>Cancel event này để ngăn item được đăng ký vào hệ thống.
 *
 * <pre>{@code
 * @EventHandler
 * public void onCreate(UItemCreateEvent event) {
 *     if (event.getItemData().getId().startsWith("test_")) {
 *         event.setCancelled(true); // chặn tạo item bắt đầu bằng "test_"
 *     }
 * }
 * }</pre>
 */
public class UItemCreateEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @Nullable
    private final CommandSender creator;

    /**
     * @param itemData Dữ liệu item mới sắp được tạo
     * @param creator  Người tạo item (null nếu tạo qua internal API)
     */
    public UItemCreateEvent(@NotNull UItemData itemData, @Nullable CommandSender creator) {
        super(itemData);
        this.creator = creator;
    }

    /**
     * Ai trigger việc tạo item này.
     * null nếu được tạo bởi plugin hoặc qua {@code UItemAPI} mà không có sender.
     */
    @Nullable
    public CommandSender getCreator() {
        return creator;
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
