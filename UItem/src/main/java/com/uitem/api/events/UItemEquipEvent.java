package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired khi player mặc hoặc tháo một UItem khỏi slot trang bị (armor + offhand).
 *
 * <p>Cancel để ngăn thay đổi trang bị.
 *
 * <pre>{@code
 * @EventHandler
 * public void onEquip(UItemEquipEvent event) {
 *     if (!event.getItemData().getId().equals("cursed_armor")) return;
 *     if (!event.isEquipping()) return; // chỉ khi mặc vào
 *
 *     event.setCancelled(true);
 *     event.getPlayer().sendMessage("⚠ Giáp này bị nguyền, không thể mặc!");
 * }
 * }</pre>
 */
public class UItemEquipEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player player;

    @NotNull
    private final EquipmentSlot slot;

    @Nullable
    private final ItemStack oldItem;

    @NotNull
    private final ItemStack newItem;

    /**
     * @param itemData  UItemData của item liên quan (item đang được mặc hoặc tháo)
     * @param player    Player
     * @param slot      Slot trang bị bị thay đổi
     * @param oldItem   Item cũ ở slot đó (null hoặc AIR nếu slot trống)
     * @param newItem   Item mới sẽ vào slot đó (AIR nếu tháo ra)
     */
    public UItemEquipEvent(
            @NotNull UItemData itemData,
            @NotNull Player player,
            @NotNull EquipmentSlot slot,
            @Nullable ItemStack oldItem,
            @NotNull ItemStack newItem) {
        super(itemData);
        this.player = player;
        this.slot = slot;
        this.oldItem = oldItem;
        this.newItem = newItem;
    }

    /** Player đang thay đổi trang bị. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** Slot đang bị thay đổi (HEAD, CHEST, LEGS, FEET, OFF_HAND). */
    @NotNull
    public EquipmentSlot getSlot() {
        return slot;
    }

    /** Item cũ đang ở slot. Null hoặc AIR nếu trống. */
    @Nullable
    public ItemStack getOldItem() {
        return oldItem;
    }

    /** Item mới sẽ vào slot. AIR nếu đang tháo ra. */
    @NotNull
    public ItemStack getNewItem() {
        return newItem;
    }

    /**
     * true nếu player đang MẶC item vào (newItem là UItem),
     * false nếu player đang THÁO item ra (oldItem là UItem).
     */
    public boolean isEquipping() {
        return newItem.getType() != org.bukkit.Material.AIR;
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
