package com.uitem.api.events;

import com.uitem.UItemData;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired khi player click (trái hoặc phải) với một UItem trên tay.
 *
 * <p>Dùng để implement custom behavior cho item:
 *
 * <pre>{@code
 * @EventHandler
 * public void onInteract(UItemInteractEvent event) {
 *     if (!event.getItemData().getId().equals("magic_wand")) return;
 *     if (event.getAction() != Action.RIGHT_CLICK_AIR &&
 *         event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
 *
 *     event.setCancelled(true); // ngăn hành động vanilla
 *     event.getPlayer().sendMessage("✨ Magic!");
 * }
 * }</pre>
 */
public class UItemInteractEvent extends UItemCancellableEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    private final Player player;

    @NotNull
    private final Action action;

    @NotNull
    private final ItemStack itemStack;

    @NotNull
    private final EquipmentSlot hand;

    @Nullable
    private final Block clickedBlock;

    @Nullable
    private final BlockFace blockFace;

    public UItemInteractEvent(
            @NotNull UItemData itemData,
            @NotNull Player player,
            @NotNull Action action,
            @NotNull ItemStack itemStack,
            @NotNull EquipmentSlot hand,
            @Nullable Block clickedBlock,
            @Nullable BlockFace blockFace) {
        super(itemData);
        this.player = player;
        this.action = action;
        this.itemStack = itemStack;
        this.hand = hand;
        this.clickedBlock = clickedBlock;
        this.blockFace = blockFace;
    }

    /** Player thực hiện interaction. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** Loại action: RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK, LEFT_CLICK_AIR, LEFT_CLICK_BLOCK. */
    @NotNull
    public Action getAction() {
        return action;
    }

    /** ItemStack đang được cầm (bản copy). */
    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /** Tay nào đang cầm: HAND (main) hoặc OFF_HAND. */
    @NotNull
    public EquipmentSlot getHand() {
        return hand;
    }

    /** Block bị click (null nếu click air). */
    @Nullable
    public Block getClickedBlock() {
        return clickedBlock;
    }

    /** Mặt block bị click (null nếu click air). */
    @Nullable
    public BlockFace getBlockFace() {
        return blockFace;
    }

    /** Convenience: true nếu là right click. */
    public boolean isRightClick() {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    /** Convenience: true nếu là left click. */
    public boolean isLeftClick() {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
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
