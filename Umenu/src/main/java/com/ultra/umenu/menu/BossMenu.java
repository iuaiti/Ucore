package com.ultra.umenu.menu;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.manager.BossManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BossMenu extends BaseMenu {

    private final BossManager bossManager;

    public BossMenu(UMenuPlugin plugin, MenuManager menuManager, BossManager bossManager) {
        super(plugin, menuManager, "boss-menu");
        this.bossManager = bossManager;
    }

    @Override
    public void open(Player player) {
        String title = getTitle(configKey);
        int size = getSize(configKey);
        Gui gui = createGui(title, size);
        fillBorder(gui, configKey);

        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int slot = 0;

        List<BossManager.Boss> bosses = bossManager.getBosses();
        if (bosses.isEmpty()) {
            ItemStack empty = ItemBuilder.create(Material.BARRIER)
                    .name("<red>No bosses available</red>")
                    .lore("<gray>Bosses will appear here when configured.</gray>")
                    .build();
            gui.setButton(22, new Button(empty, null));
        } else {
            for (BossManager.Boss boss : bosses) {
                if (slot >= contentSlots.length) break;
                boolean cooldown = bossManager.isOnCooldown(player, boss.id());
                String status;

                if (cooldown) {
                    long remaining = bossManager.getCooldown(player, boss.id()) - System.currentTimeMillis();
                    status = "<red>Cooldown: " + (remaining / 1000) + "s</red>";
                } else {
                    status = "<green>Ready!</green>";
                }

                ItemStack item = ItemBuilder.create(Material.ZOMBIE_HEAD)
                        .name("<red>" + boss.name() + "</red>")
                        .lore(
                                "<gray>Location: " + boss.world() + " (" + boss.x() + ", " + boss.y() + ", " + boss.z() + ")</gray>",
                                "<gray>Reward: " + boss.reward() + "</gray>",
                                "",
                                status
                        )
                        .build();
                gui.setButton(contentSlots[slot++], new Button(item, null));
            }
        }

        addConfigItems(gui, configKey, player);
        gui.open(player);
    }

    @Override
    protected void build(Gui gui, Player player) {
    }
}
