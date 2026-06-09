package com.ultra.umenu.menu;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.manager.SettingsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class SettingsMenu extends BaseMenu {

    private final SettingsManager settingsManager;

    public SettingsMenu(UMenuPlugin plugin, MenuManager menuManager, SettingsManager settingsManager) {
        super(plugin, menuManager, "settings-menu");
        this.settingsManager = settingsManager;
    }

    @Override
    public void open(Player player) {
        String title = getTitle(configKey);
        int size = getSize(configKey);
        Gui gui = createGui(title, size);
        fillBorder(gui, configKey);
        build(gui, player);
        gui.open(player);
    }

    @Override
    protected void build(Gui gui, Player player) {
        boolean sound = settingsManager.hasSoundEnabled(player);
        boolean particles = settingsManager.hasParticlesEnabled(player);
        boolean pm = settingsManager.hasPrivateMessagesEnabled(player);
        boolean notifs = settingsManager.hasNotificationsEnabled(player);

        gui.setButton(11, createToggleItem(
                Material.NOTE_BLOCK,
                "Sound",
                sound,
                event -> {
                    settingsManager.toggleSound(player);
                    open(player);
                    UCoreAPI.getMessageManager().send(player, "settings.sound-toggled");
                }
        ));

        gui.setButton(13, createToggleItem(
                Material.FIREWORK_ROCKET,
                "Particles",
                particles,
                event -> {
                    settingsManager.toggleParticles(player);
                    open(player);
                    UCoreAPI.getMessageManager().send(player, "settings.particles-toggled");
                }
        ));

        gui.setButton(15, createToggleItem(
                Material.PLAYER_HEAD,
                "Private Messages",
                pm,
                event -> {
                    settingsManager.togglePrivateMessages(player);
                    open(player);
                    UCoreAPI.getMessageManager().send(player, "settings.pm-toggled");
                }
        ));

        gui.setButton(20, createToggleItem(
                Material.BELL,
                "Notifications",
                notifs,
                event -> {
                    settingsManager.toggleNotifications(player);
                    open(player);
                    UCoreAPI.getMessageManager().send(player, "settings.notifications-toggled");
                }
        ));

        addBackButton(gui, "main", 40);
    }

    private Button createToggleItem(Material material, String name, boolean enabled, Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        String color = enabled ? "<green>" : "<red>";
        String status = enabled ? "Enabled" : "Disabled";
        ItemStack item = ItemBuilder.create(material)
                .name(color + name + ": " + status + "</" + (enabled ? "green" : "red") + ">")
                .lore(
                        "<gray>Click to toggle</gray>"
                )
                .build();
        return new Button(item, action);
    }
}
