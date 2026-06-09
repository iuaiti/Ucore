package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired khi một custom item được cho (give) cho player.
 *
 * <p>Event này có thể modify số lượng hoặc ItemStack trước khi cho.
 * Cancel để ngăn không cho item.
 *
 * <pre>{@code
 * @EventHandler
 * public void onGive(UItemGiveEvent event) {
 *     // VIP nhận gấp đôi
 *     if (event.getRecipient().hasPermission("vip.double_items")) {
 *         event.setAmount(event.getAmount() * 2);
 *     }
 *     // Cấm give trong thế giới nether
 *     if (event.getRecipient().getWorld().getEnvironment() == World.Environment.NETHER) {
 *         event.setCancelled(true);
 *     }
 * }
 * }</pre>
 */
public class UItemGiveEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player recipient;

    @Nullable
    private final CommandSender giver;

    @NotNull
    private ItemStack itemStack;

    private int amount;

    /**
     * @param itemData  Dữ liệu item được give
     * @param recipient Player nhận item
     * @param giver     Người thực hiện lệnh give (null nếu give qua API)
     * @param itemStack ItemStack sắp được cho (clone)
     * @param amount    Số lượng
     */
    public UItemGiveEvent(
            @NotNull UItemData itemData,
            @NotNull Player recipient,
            @Nullable CommandSender giver,
            @NotNull ItemStack itemStack,
            int amount) {
        super(itemData);
        this.recipient = recipient;
        this.giver = giver;
        this.itemStack = itemStack;
        this.amount = Math.max(1, amount);
    }

    /** Player sẽ nhận item. */
    @NotNull
    public Player getRecipient() {
        return recipient;
    }

    /**
     * Ai thực hiện give. null nếu give qua API hoặc tự động.
     */
    @Nullable
    public CommandSender getGiver() {
        return giver;
    }

    /** ItemStack sắp được cho. Có thể thay thế bằng {@link #setItemStack}. */
    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Override ItemStack được cho.
     * Hữu ích khi muốn thêm enchant/lore tùy theo context.
     */
    public void setItemStack(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    /** Số lượng item sẽ được cho. */
    public int getAmount() {
        return amount;
    }

    /**
     * Thay đổi số lượng item sẽ được cho.
     * Giá trị được clamp về [1, 64].
     */
    public void setAmount(int amount) {
        this.amount = Math.max(1, Math.min(64, amount));
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
