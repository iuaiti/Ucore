package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractGui implements InventoryHolder {

    protected final UItemPlugin plugin;
    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;

    private Runnable backAction;

    public AbstractGui(UItemPlugin plugin, Player player, int size, String title) {
        this.plugin = plugin;
        this.player = player;
        this.clickHandlers = new HashMap<>();
        this.inventory = Bukkit.createInventory(this, size,
                TextUtil.parse(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        setup();
        player.openInventory(inventory);
    }

    /**
     * Open this GUI as a child of the given parent.
     * The back button will reopen the parent GUI.
     */
    public void openAsChildOf(AbstractGui parent) {
        if (parent != null) {
            this.backAction = () -> parent.reopen();
        }
        open();
    }

    /**
     * Open this GUI as root (no parent, close on back).
     */
    public void openAsRoot() {
        this.backAction = null;
        open();
    }

    public void refresh() {
        clickHandlers.clear();
        inventory.clear();
        setup();
    }

    /**
     * Clear, setup lại, và mở lại inventory.
     * Dùng cho goBack() để parent GUI được hiển thị đúng.
     */
    public void reopen() {
        clickHandlers.clear();
        inventory.clear();
        setup();
        player.openInventory(inventory);
    }

    protected abstract void setup();

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(event);
        }
    }

    public void handleBottomInventoryClick(InventoryClickEvent event) {
        event.setCancelled(false);
    }

    protected void setBackAction(Runnable backAction) {
        this.backAction = backAction;
    }

    protected boolean hasBackAction() {
        return backAction != null;
    }

    public void goBack() {
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        if (backAction != null) {
            backAction.run();
        } else {
            player.closeInventory();
        }
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
    }

    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void fillBorder(int rows) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                setItem(i, border);
            }
        }
    }

    protected ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            if (lore != null) {
                meta.lore(lore.stream().map(TextUtil::parse).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createBackButton() {
        return createItem(Material.ARROW, Messages.guiBack(), List.of(Messages.guiBackDesc()));
    }

    protected ItemStack createPreviousButton() {
        return createItem(Material.ARROW, "<yellow>◄ Previous page");
    }

    protected ItemStack createNextButton() {
        return createItem(Material.ARROW, "<yellow>Next page ►");
    }
}
