package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Confirm delete item GUI — fully bilingual.
 */
public class ConfirmDeleteGui extends AbstractGui {

    private final UItemData data;
    private final int returnPage;

    public ConfirmDeleteGui(UItemPlugin plugin, Player player, UItemData data, int returnPage) {
        super(plugin, player, 27, Messages.confirmDeleteTitle());
        this.data = data;
        this.returnPage = returnPage;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        // Fill tất cả với glass pane đen
        for (int i = 0; i < 27; i++) {
            setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        // Hiển thị item cần xóa ở giữa
        setItem(13, plugin.getItemManager().buildItemStack(data));

        // Confirm button (green)
        setItem(11, createItem(Material.LIME_WOOL,
                Messages.confirmDeleteYes(),
                List.of(
                        Messages.confirmDeleteDesc() + "<white>" + data.getId(),
                        "",
                        Messages.confirmDeleteWarn()
                )), event -> {
            // Dùng API để fire UItemDeleteEvent — plugin khác có thể cancel
            boolean deleted = com.uitem.api.UItemAPI.get()
                    .removeItem(data.getId(), player);
            if (!deleted) {
                player.sendMessage(com.uitem.util.TextUtil.error(
                        Messages.cannotDeleteBlocked(data.getId())));
                goBackOrList();
                return;
            }
            player.sendMessage(com.uitem.util.TextUtil.success(
                    Messages.deletedItem(data.getId())));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
            goBackOrList();
        });

        // Cancel button (red) — quay lại GUI trước đó
        setItem(15, createItem(Material.RED_WOOL,
                Messages.confirmDeleteNo(),
                List.of(
                        Messages.confirmDeleteBack()
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            goBack();
        });
    }

    private void goBackOrList() {
        if (hasBackAction()) {
            goBack();
        } else {
            new ItemListGui(plugin, player, returnPage).open();
        }
    }
}
