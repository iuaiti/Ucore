package com.ultra.umenu.menu;

import com.ultra.ucore.config.ConfigManager;
import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.model.MenuItemLoader;
import com.ultra.umenu.module.ModuleDetector;
import com.ultra.umenu.module.ModuleType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public abstract class BaseMenu {

    protected final UMenuPlugin plugin;
    protected final MenuManager menuManager;
    protected final ConfigManager configManager;
    protected final MenuItemLoader itemLoader;
    protected final ModuleDetector moduleDetector;
    protected final String configKey;

    protected BaseMenu(UMenuPlugin plugin, MenuManager menuManager, String configKey) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.configManager = plugin.getConfigManager();
        this.itemLoader = menuManager.getItemLoader();
        this.moduleDetector = plugin.getModuleDetector();
        this.configKey = configKey;
    }

    public abstract void open(Player player);

    protected abstract void build(Gui gui, Player player);

    protected Gui createGui(String title, int size) {
        return new Gui(title, size);
    }

    protected String getTitle(String key) {
        return configManager.get("menus.yml").getString(key + ".title", "Menu");
    }

    protected int getSize(String key) {
        return configManager.get("menus.yml").getInt(key + ".size", 54);
    }

    protected void fillBorder(Gui gui, String menuKey) {
        var config = configManager.get("menus.yml");
        String borderKey = menuKey + ".border";
        if (!config.contains(borderKey)) return;

        String materialName = config.getString(borderKey + ".material", "BLACK_STAINED_GLASS_PANE");
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;

        String name = config.getString(borderKey + ".name", " ");
        ItemStack borderItem = ItemBuilder.create(material).name(name).build();

        for (String slotStr : config.getStringList(borderKey + ".slots")) {
            if (slotStr.contains("-")) {
                String[] parts = slotStr.split("-");
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                for (int i = start; i <= end; i++) {
                    gui.setButton(i, new Button(borderItem.clone(), null));
                }
            } else {
                gui.setButton(Integer.parseInt(slotStr.trim()), new Button(borderItem.clone(), null));
            }
        }
    }

    protected void addConfigItems(Gui gui, String menuKey, Player player) {
        var config = configManager.get("menus.yml");
        String itemsKey = menuKey + ".items";
        var section = config.getConfigurationSection(itemsKey);
        if (section == null) return;

        for (String itemKey : section.getKeys(false)) {
            String path = itemsKey + "." + itemKey;
            int slot = config.getInt(path + ".slot", 0);
            String itemRef = config.getString(path + ".item", "stone");
            String action = config.getString(path + ".action", "NONE");
            String target = config.getString(path + ".target", "");

            ItemStack itemStack = itemLoader.build(itemRef, player);
            Consumer<org.bukkit.event.inventory.InventoryClickEvent> clickHandler = switch (action.toUpperCase()) {
                case "OPEN_MENU" -> event -> menuManager.open((Player) event.getWhoClicked(), target);
                case "BACK" -> event -> menuManager.open((Player) event.getWhoClicked(), target);
                case "WARP" -> event -> warp((Player) event.getWhoClicked(), target);
                case "COMMAND" -> event -> runCommand((Player) event.getWhoClicked(), target);
                case "CLOSE" -> event -> event.getWhoClicked().closeInventory();
                default -> null;
            };

            gui.setButton(slot, new Button(itemStack, clickHandler));
        }
    }

    protected void addBackButton(Gui gui, String targetMenu, int slot) {
        ItemStack backItem = itemLoader.build("back", null);
        gui.setButton(slot, new Button(backItem, event ->
                menuManager.open((Player) event.getWhoClicked(), targetMenu)));
    }

    protected void addCloseButton(Gui gui, int slot) {
        ItemStack closeItem = itemLoader.build("close", null);
        gui.setButton(slot, new Button(closeItem, event ->
                event.getWhoClicked().closeInventory()));
    }

    protected void warp(Player player, String warpName) {
        if (warpName == null || warpName.isBlank()) {
            player.sendMessage("§cWarp target is not configured.");
            return;
        }

        player.closeInventory();
        player.performCommand("warp " + warpName.trim());
    }

    protected void runCommand(Player player, String command) {
        if (command == null || command.isBlank()) {
            player.sendMessage("§cCommand target is not configured.");
            return;
        }

        player.closeInventory();
        player.performCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    protected boolean isModuleEnabled(ModuleType type) {
        return moduleDetector.isPresent(type);
    }

    protected String msg(String path) {
        return configManager.get("messages.yml").getString(path, path);
    }
}
