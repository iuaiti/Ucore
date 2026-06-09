package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI give nhiều item cùng lúc cho một hoặc nhiều người chơi.
 * Tính năng mới giống Oraxen/ItemsAdder nhưng qua GUI.
 */
public class BulkGiveGui extends AbstractGui {

    private int page = 0;
    private final Set<String> selectedItems = new LinkedHashSet<>();
    private int giveAmount = 1;

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.length;

    public BulkGiveGui(UItemPlugin plugin, Player player) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Give Nhiều Item</bold></gradient>");
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.CHEST,
                "<gradient:#ff6b6b:#ffd93d><bold>Bulk Give</bold></gradient>",
                List.of(
                        "<gray>Click item để chọn/bỏ chọn",
                        "<gray>Sau đó chọn người chơi để give",
                        "",
                        "<yellow>Đã chọn: <white>" + selectedItems.size() + " item(s)",
                        "<yellow>Số lượng mỗi item: <white>x" + giveAmount
                )));

        // Items list
        List<UItemData> allItems = new ArrayList<>(plugin.getItemManager().getAllItems().values());
        allItems.sort(Comparator.comparing(UItemData::getId, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex >= allItems.size()) break;

            UItemData data = allItems.get(dataIndex);
            boolean selected = selectedItems.contains(data.getId());

            ItemStack item = plugin.getItemManager().buildItemStack(data);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>──────────────"));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>ID: <white>" + data.getId()));
                if (selected) {
                    lore.add(MiniMessage.miniMessage().deserialize("<green>✔ ĐÃ CHỌN"));
                    lore.add(MiniMessage.miniMessage().deserialize("<yellow>Click để bỏ chọn"));
                } else {
                    lore.add(MiniMessage.miniMessage().deserialize("<gray>Click để chọn"));
                }
                meta.lore(lore);

                // Highlight selected items with enchant glow
                if (selected) {
                    meta.setEnchantmentGlintOverride(true);
                }
                item.setItemMeta(meta);
            }

            setItem(ITEM_SLOTS[i], item, ev -> {
                if (selectedItems.contains(data.getId())) {
                    selectedItems.remove(data.getId());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 0.8f);
                } else {
                    selectedItems.add(data.getId());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
                }
                refresh();
            });
        }

        // Amount selector
        setItem(37, createItem(Material.GOLD_INGOT,
                "<gold><bold>Số Lượng: x" + giveAmount + "</bold>",
                List.of(
                        "<gray>Số lượng mỗi item khi give",
                        "<yellow>Left: +1 | Right: -1",
                        "<green>Shift+Left: +8 | Shift+Right: -8"
                )), ev -> {
            if (ev.getClick() == ClickType.LEFT) giveAmount = Math.min(64, giveAmount + 1);
            else if (ev.getClick() == ClickType.RIGHT) giveAmount = Math.max(1, giveAmount - 1);
            else if (ev.getClick() == ClickType.SHIFT_LEFT) giveAmount = Math.min(64, giveAmount + 8);
            else if (ev.getClick() == ClickType.SHIFT_RIGHT) giveAmount = Math.max(1, giveAmount - 8);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            refresh();
        });

        // Select all / Deselect all
        setItem(38, createItem(Material.LIME_DYE,
                "<green><bold>Chọn Tất Cả</bold>",
                List.of("<gray>Chọn tất cả item trên trang này")),
                ev -> {
                    int start = page * ITEMS_PER_PAGE;
                    for (int i = start; i < Math.min(start + ITEMS_PER_PAGE, allItems.size()); i++) {
                        selectedItems.add(allItems.get(i).getId());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
                    refresh();
                });

        setItem(39, createItem(Material.RED_DYE,
                "<red><bold>Bỏ Chọn Tất Cả</bold>",
                List.of("<gray>Bỏ chọn tất cả item")),
                ev -> {
                    selectedItems.clear();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 0.8f);
                    refresh();
                });

        // Give to self
        setItem(41, createItem(Material.PLAYER_HEAD,
                "<green><bold>Give Cho Bản Thân</bold>",
                List.of(
                        "<gray>Give " + selectedItems.size() + " item(s) cho bạn",
                        "<gray>Mỗi item: x" + giveAmount,
                        "",
                        selectedItems.isEmpty() ? "<red>Chưa chọn item nào!" : "<yellow>Click để give"
                )), ev -> {
            if (selectedItems.isEmpty()) {
                player.sendMessage(TextUtil.error("Chưa chọn item nào!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            giveItemsToPlayer(player, player);
        });

        // Give to another player
        setItem(42, createItem(Material.EMERALD,
                "<aqua><bold>Give Cho Người Chơi Khác</bold>",
                List.of(
                        "<gray>Nhập tên người chơi để give",
                        "<gray>Mỗi item: x" + giveAmount,
                        "",
                        selectedItems.isEmpty() ? "<red>Chưa chọn item nào!" : "<yellow>Click để nhập tên"
                )), ev -> {
            if (selectedItems.isEmpty()) {
                player.sendMessage(TextUtil.error("Chưa chọn item nào!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập tên người chơi để give (hoặc 'cancel' để hủy):",
                    input -> {
                        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("huy")) {
                            new BulkGiveGui(plugin, player).open();
                            return;
                        }
                        Player target = Bukkit.getPlayerExact(input.trim());
                        if (target == null) {
                            player.sendMessage(TextUtil.error("Người chơi '" + input + "' không online!"));
                            new BulkGiveGui(plugin, player).open();
                            return;
                        }
                        giveItemsToPlayer(player, target);
                    });
        });

        // Give to all online players
        setItem(43, createItem(Material.BEACON,
                "<gold><bold>Give Cho Tất Cả Online</bold>",
                List.of(
                        "<gray>Give cho tất cả người chơi đang online",
                        "<gray>Mỗi item: x" + giveAmount,
                        "",
                        selectedItems.isEmpty() ? "<red>Chưa chọn item nào!" : "<yellow>Click để give"
                )), ev -> {
            if (selectedItems.isEmpty()) {
                player.sendMessage(TextUtil.error("Chưa chọn item nào!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            int count = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                giveItemsToPlayer(player, online);
                count++;
            }
            player.sendMessage(TextUtil.success("Đã give " + selectedItems.size() + " item(s) cho " + count + " người chơi!"));
        });

        // Pagination
        if (page > 0) {
            setItem(46, createPreviousButton(), ev -> {
                page--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }
        if (page < totalPages - 1) {
            setItem(52, createNextButton(), ev -> {
                page++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        // Back
        setItem(49, createBackButton(), ev -> goBack());
    }

    private void giveItemsToPlayer(Player sender, Player target) {
        if (!sender.hasPermission("uitem.give")) {
            sender.sendMessage(TextUtil.error("Bạn không có quyền give item!"));
            return;
        }

        int successCount = 0;
        for (String itemId : selectedItems) {
            boolean ok = com.uitem.api.UItemAPI.get().giveItem(target, itemId, giveAmount, sender);
            if (ok) successCount++;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        sender.sendMessage(TextUtil.success("Đã give " + successCount + "/" + selectedItems.size() +
                " item(s) x" + giveAmount + " cho " + target.getName() + "!"));
        if (!sender.equals(target)) {
            target.sendMessage(TextUtil.success("Bạn nhận được " + successCount + " item(s) từ " + sender.getName() + "!"));
        }
    }
}
