package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI editor for attaching effects (reflect, extra damage on triggers).
 * Supports selecting trigger (incl. DIE, HIT for target), type (REFLECT/EXTRA_DAMAGE), chance, value.
 */
public class EffectEditorGui extends AbstractGui {

    private static final List<String> TRIGGERS = List.of("HIT", "DAMAGE", "DIE", "EQUIP", "FULL_SET");
    private static final List<String> TYPES = List.of("REFLECT", "EXTRA_DAMAGE");

    private final UItemData data;
    private UItemData.ItemEffect pending;

    public EffectEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        this(plugin, player, data, null);
    }

    public EffectEditorGui(UItemPlugin plugin, Player player, UItemData data, UItemData.ItemEffect initialPending) {
        super(plugin, player, 54, "<light_purple><bold>Effect Editor</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
        if (initialPending != null) {
            this.pending = initialPending.copy();
        } else {
            this.pending = new UItemData.ItemEffect();
            // sensible defaults
            this.pending.setTrigger("HIT");
            this.pending.setType("REFLECT");
            this.pending.setChance(100.0);
            this.pending.setValue(20.0);
        }
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
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.BOOK,
                "<light_purple><bold>Hiệu Ứng cho " + data.getId() + "</bold>",
                List.of(
                        "<gray>Kích hoạt khi hành động → áp dụng hiệu ứng chiến đấu (phản đòn/sát thương thêm)",
                        "<yellow>Chọn trigger + loại + tỷ lệ + giá trị",
                        "<red>QUAN TRỌNG: Sau khi lưu, dùng /uitem give <id> hoặc craft để lấy item đã cập nhật!",
                        "<gray>HIT target: hiệu ứng có thể áp dụng cho nạn nhân (nếu loại hỗ trợ)"
                )));

        // === Danh sách hiệu ứng hiện tại (click để xóa) - chỉ hàng đầu để chừa chỗ ===
        setItem(9, createItem(Material.WRITABLE_BOOK, "<gold><bold>Hiệu Ứng Hiện Tại</bold>",
                List.of("<gray>Click hiệu ứng để xóa", "<gray>Khuyến nghị tối đa: 4-6 mỗi item")));

        List<UItemData.ItemEffect> current = data.getEffects() != null ? new ArrayList<>(data.getEffects()) : new ArrayList<>();
        int[] currentSlots = {10, 11, 12, 13, 14, 15, 16, 17};
        for (int i = 0; i < Math.min(current.size(), currentSlots.length); i++) {
            final int idx = i;
            UItemData.ItemEffect e = current.get(i);
            ItemStack icon = buildEffectIcon(e);
            setItem(currentSlots[i], icon, ev -> {
                if (idx < data.getEffects().size()) {
                    data.getEffects().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    refresh();
                }
            });
        }
        if (current.isEmpty()) {
            setItem(10, createItem(Material.BARRIER, "<gray><italic>Chưa có hiệu ứng</italic>",
                    List.of("<yellow>Dùng trình tạo bên dưới để thêm")));
        }

        // Nút xóa tất cả
        setItem(18, createItem(Material.TNT, "<red><bold>Xóa Tất Cả Hiệu Ứng</bold>",
                List.of("<gray>Xóa mọi hiệu ứng đã gắn")), ev -> {
            if (!data.getEffects().isEmpty()) {
                data.getEffects().clear();
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.2f);
                plugin.getItemManager().registerItem(data);
                plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                refresh();
            }
        });

        // === BUILDER (uses lower area) ===
        setItem(25, createItem(Material.ANVIL, "<gold><bold>TRÌNH TẠO HIỆU ỨNG MỚI</bold>",
                List.of("<gray>Chọn trigger → loại (REFLECT/EXTRA) → tỷ lệ + giá trị rồi Add")));

        // Trigger buttons
        int tStart = 28;
        for (int ti = 0; ti < TRIGGERS.size(); ti++) {
            final String trig = TRIGGERS.get(ti);
            boolean sel = trig.equalsIgnoreCase(pending.getTrigger());
            Material mat = sel ? Material.LIME_STAINED_GLASS_PANE : triggerMaterial(trig);
            setItem(tStart + ti, createItem(mat,
                    (sel ? "<green>✔ " : "<gold>") + trig,
                    List.of("<gray>Trigger: " + trig,
                            sel ? "<green>Đã chọn" : "<yellow>Click để chọn")),
                    ev -> {
                        pending.setTrigger(trig);
                        if (!"HIT".equals(trig)) {
                            pending.setApplyToTarget(false);
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
                        refresh();
                    });
        }

        // Affects: Bản thân / Mục tiêu - luôn hiển thị trong builder (chỉ có hiệu lực với trigger HIT)
        boolean toTarget = pending.isApplyToTarget();
        setItem(33, createItem(!toTarget ? Material.EMERALD : Material.GRAY_STAINED_GLASS_PANE,
                (!toTarget ? "<green><bold>BẢN THÂN (người cầm/mặc)</bold>" : "<gray>BẢN THÂN (người cầm/mặc)"),
                List.of(
                        "<gray>Ai nhận hiệu ứng?",
                        "<green>Bản thân = người chơi cầm hoặc mặc item",
                        "<yellow>Click để áp dụng cho bản thân"
                )),
                ev -> {
                    pending.setApplyToTarget(false);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                    refresh();
                });

        // Nút Mục tiêu (slot 34 - chỉ set một lần ở đây)
        setItem(34, createItem(toTarget ? Material.REDSTONE : Material.GRAY_STAINED_GLASS_PANE,
                (toTarget ? "<red><bold>MỤC TIÊU (nạn nhân)</bold>" : "<gray>MỤC TIÊU (nạn nhân)"),
                List.of(
                        "<gray>Chỉ hữu ích với trigger HIT:",
                        "<red>Mục tiêu = mob/người chơi bị đánh",
                        "<yellow>Click để áp dụng cho kẻ bị đánh"
                )),
                ev -> {
                    pending.setApplyToTarget(true);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                    refresh();
                });

        // Nhãn nhỏ cho cặp
        setItem(35, createItem(Material.PAPER, "<gray>Áp dụng cho", List.of()));

        // Nút Type (sau phần affects self/target ở 33/34/35)
        int typeStart = 36;
        for (int ty = 0; ty < TYPES.size(); ty++) {
            final String typ = TYPES.get(ty);
            boolean sel = typ.equalsIgnoreCase(pending.getType());
            Material mat = sel ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(typeStart + ty, createItem(mat,
                    (sel ? "<aqua>✔ " : "<gray>") + typ,
                    List.of("<gray>Loại hiệu ứng: " + typ, sel ? "<green>Đã chọn" : "<yellow>Click để chọn")),
                    ev -> {
                        pending.setType(typ);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
                        refresh();
                    });
        }

        // Preset Tỷ lệ (sau value ở 39)
        double ch = pending.getChance();
        setItem(40, createItem(Material.GHAST_TEAR, "<gold><bold>Tỷ lệ: <white>" + (int)ch + "%</bold>", List.of()));
        int[] chanceSlots = {41, 42, 43, 44};
        double[] chances = {100, 75, 50, 25};
        for (int ci = 0; ci < chances.length; ci++) {
            final double c = chances[ci];
            boolean sel = Math.abs(ch - c) < 0.1;
            setItem(chanceSlots[ci], createItem(sel ? Material.LIME_DYE : Material.GRAY_DYE,
                    (sel ? "<green>" : "<gray>") + (int) c + "%",
                    List.of("<yellow>Đặt tỷ lệ kích hoạt thành " + (int) c + "%")), ev -> {
                pending.setChance(c);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        // Điều khiển Giá trị (cho REFLECT / EXTRA_DAMAGE) - ở 39
        setItem(39, createItem(Material.DIAMOND_SWORD, "<gold>Giá trị <white>" + pending.getValue(),
                List.of("<gray>REFLECT = % sát thương phản đòn", "<gray>EXTRA_DAMAGE = lượng sát thương thêm phẳng", "<yellow>Click nhãn để nhập giá trị chính xác qua chat")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>Nhập giá trị (ví dụ: 25 cho 25% hoặc lượng):", input -> {
                try {
                    double v = Double.parseDouble(input.trim());
                    pending.setValue(Math.max(0, v));
                } catch (Exception ignored) {}
                refresh();
            });
        });

        // Nút THÊM
        String targetInfo = "";
        if ("HIT".equalsIgnoreCase(pending.getTrigger())) {
            targetInfo = pending.isApplyToTarget() ? " <red>(cho MỤC TIÊU)" : " <green>(cho BẢN THÂN)";
        }
        setItem(45, createItem(Material.EMERALD_BLOCK, "<green><bold>✔ THÊM HIỆU ỨNG NÀY</bold>",
                List.of(
                        "<gray>Trigger: <white>" + safe(pending.getTrigger()) + targetInfo,
                        "<gray>Loại: <white>" + safe(pending.getType()),
                        "<gray>Tỷ lệ: <white>" + (int) pending.getChance() + "%  Giá trị: <white>" + pending.getValue(),
                        "<yellow>Click để gắn vào item"
                )), ev -> {
            if (pending.getTrigger() == null || pending.getType() == null) {
                player.sendMessage(TextUtil.error("Cần ít nhất Trigger + Loại"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            data.getEffects().add(pending.copy());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.4f);
            player.sendMessage(TextUtil.success("Đã thêm hiệu ứng: " + pending.getTrigger() + " / " + pending.getType() + (pending.isApplyToTarget() ? " (mục tiêu)" : "")));

            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveItem(plugin.getItemManager(), data);

            // fresh pending for next
            pending = new UItemData.ItemEffect();
            pending.setTrigger("HIT");
            pending.setType("REFLECT");
            pending.setChance(100.0);
            pending.setValue(20.0);

            refresh();
        });

        // Nút Quay lại ở dưới
        setItem(53, createBackButton(), ev -> goBack());
    }

    private Material triggerMaterial(String trig) {
        return switch (trig) {
            case "HIT"      -> Material.IRON_SWORD;
            case "DAMAGE"   -> Material.SHIELD;
            case "DIE"      -> Material.SKELETON_SKULL;
            case "EQUIP"    -> Material.LEATHER_CHESTPLATE;
            case "FULL_SET" -> Material.DIAMOND_CHESTPLATE;
            default         -> Material.GRAY_DYE;
        };
    }

    private String safe(String s) {
        return s == null ? "—" : s;
    }

    private ItemStack buildEffectIcon(UItemData.ItemEffect e) {
        Material mat = Material.IRON_SWORD;
        String trig = e.getTrigger() != null ? e.getTrigger() : "?";
        if ("HIT".equals(trig) || "DAMAGE".equals(trig)) mat = Material.IRON_SWORD;
        else if ("DIE".equals(trig)) mat = Material.SKELETON_SKULL;
        else if ("EQUIP".equals(trig) || "FULL_SET".equals(trig)) mat = Material.DIAMOND_CHESTPLATE;

        String targetNote = "";
        if ("HIT".equals(trig) && e.isApplyToTarget()) {
            targetNote = " <red>→ TARGET";
        } else if ("HIT".equals(trig)) {
            targetNote = " <green>→ SELF";
        }

        String name = "<light_purple>" + trig + targetNote + " <gray>→ <aqua>" + (e.getType() != null ? e.getType() : "");
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Chance: <white>" + (int) e.getChance() + "%");
        if (e.getValue() != 0) lore.add("<gray>Value: <white>" + e.getValue());
        if ("HIT".equals(trig)) {
            lore.add("<gray>Áp dụng cho: <white>" + (e.isApplyToTarget() ? "Mục tiêu (nạn nhân)" : "Bản thân (người cầm/mặc)"));
        }
        lore.add("");
        lore.add("<red>Click để xóa");
        return createItem(mat, name, lore);
    }

}
