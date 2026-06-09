package com.ultra.umenu.menu;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ServerInfoMenu extends BaseMenu {

    public ServerInfoMenu(UMenuPlugin plugin, MenuManager menuManager) {
        super(plugin, menuManager, "server-info-menu");
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
        long online = Bukkit.getOnlinePlayers().size();
        long max = Bukkit.getMaxPlayers();

        ItemStack infoItem = ItemBuilder.create(Material.KNOWLEDGE_BOOK)
                .name("<gradient:#00AAFF:#0055FF>Server Information</gradient>")
                .lore(
                        "<gray>Server: <yellow>Ultra SMP</yellow></gray>",
                        "<gray>Version: <yellow>" + plugin.getServer().getVersion() + "</yellow></gray>",
                        "<gray>Players Online: <yellow>" + online + "/" + max + "</yellow></gray>",
                        "<gray>TPS: <yellow>" + String.format("%.1f", plugin.getServer().getTPS()[0]) + "</yellow></gray>",
                        "",
                        "<gray>Modules:</gray>" +
                                (plugin.getModuleDetector().anyPresent() ? "" : " <dark_gray>None</dark_gray>"),
                        "<dark_gray>" + buildModuleList() + "</dark_gray>"
                )
                .build();
        gui.setButton(22, new Button(infoItem, null));
        addBackButton(gui, "main", 40);
    }

    private String buildModuleList() {
        var md = plugin.getModuleDetector();
        StringBuilder sb = new StringBuilder();
        for (var type : com.ultra.umenu.module.ModuleType.values()) {
            if (md.isPresent(type)) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append("<green>").append(type.pluginName()).append("</green>");
            }
        }
        return sb.toString();
    }
}
