package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * GUI chọn preset thuộc tính (Attribute) thay vì gõ tay.
 * Giống Oraxen/ItemsAdder nhưng hoàn toàn qua GUI.
 *
 * Người chơi chọn loại thuộc tính → chọn slot → nhập giá trị.
 */
public class AttributePresetGui extends AbstractGui {

    private final UItemData data;

    // Preset: name, attribute key, default value, operation, description
    private record AttrPreset(String name, String key, double defaultValue, String operation, String slot, String desc, Material icon) {}

    private static final List<AttrPreset> PRESETS = List.of(
            // Combat
            new AttrPreset("⚔ Sát Thương Tay Chính", "generic_attack_damage", 5.0, "ADD_NUMBER", "HAND",
                    "Tăng sát thương khi cầm trên tay", Material.DIAMOND_SWORD),
            new AttrPreset("⚔ Tốc Độ Đánh", "generic_attack_speed", 1.0, "ADD_NUMBER", "HAND",
                    "Tăng tốc độ đánh (mặc định 4.0)", Material.GOLDEN_SWORD),
            new AttrPreset("🏹 Sát Thương Tay Phụ", "generic_attack_damage", 3.0, "ADD_NUMBER", "OFF_HAND",
                    "Sát thương khi cầm tay phụ", Material.IRON_SWORD),
            new AttrPreset("💥 Tầm Đánh", "generic_attack_knockback", 1.0, "ADD_NUMBER", "HAND",
                    "Tăng lực đẩy khi đánh", Material.STICK),

            // Defense
            new AttrPreset("🛡 Giáp (Armor)", "generic_armor", 4.0, "ADD_NUMBER", "CHEST",
                    "Tăng điểm giáp (mặc định 0)", Material.DIAMOND_CHESTPLATE),
            new AttrPreset("💎 Độ Cứng Giáp", "generic_armor_toughness", 2.0, "ADD_NUMBER", "CHEST",
                    "Giảm sát thương từ đòn mạnh", Material.NETHERITE_CHESTPLATE),
            new AttrPreset("🔰 Kháng Knock", "generic_knockback_resistance", 0.5, "ADD_NUMBER", "CHEST",
                    "Kháng lực đẩy (0-1, 1=miễn dịch)", Material.SHIELD),

            // Movement
            new AttrPreset("🏃 Tốc Độ Di Chuyển", "generic_movement_speed", 0.02, "ADD_NUMBER", "FEET",
                    "Tăng tốc độ di chuyển (mặc định 0.1)", Material.DIAMOND_BOOTS),
            new AttrPreset("🌊 Tốc Độ Bơi", "generic_water_movement_efficiency", 0.1, "ADD_NUMBER", "FEET",
                    "Tăng tốc độ bơi", Material.TURTLE_HELMET),

            // Health
            new AttrPreset("❤ Máu Tối Đa", "generic_max_health", 4.0, "ADD_NUMBER", "ANY",
                    "Tăng máu tối đa (+4 = +2 tim)", Material.GOLDEN_APPLE),
            new AttrPreset("🔄 Hồi Máu", "generic_max_absorption", 4.0, "ADD_NUMBER", "ANY",
                    "Tăng máu hấp thụ tối đa", Material.ENCHANTED_GOLDEN_APPLE),

            // Luck
            new AttrPreset("🍀 May Mắn", "generic_luck", 5.0, "ADD_NUMBER", "ANY",
                    "Tăng may mắn (ảnh hưởng loot table)", Material.RABBIT_FOOT),

            // Gravity
            new AttrPreset("🪂 Trọng Lực", "generic_gravity", -0.04, "ADD_NUMBER", "ANY",
                    "Thay đổi trọng lực (âm = bay lên)", Material.FEATHER),

            // Scale
            new AttrPreset("📏 Kích Thước", "generic_scale", 0.5, "ADD_NUMBER", "ANY",
                    "Thay đổi kích thước nhân vật", Material.SLIME_BALL)
    );

    public AttributePresetGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, "<gold><bold>Chọn Thuộc Tính (Attribute)</bold>");
        this.data = data;
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
        setItem(4, createItem(Material.IRON_SWORD,
                "<gold><bold>Preset Thuộc Tính</bold>",
                List.of(
                        "<gray>Chọn thuộc tính để thêm vào item",
                        "<gray>Bạn sẽ được nhập giá trị sau",
                        "",
                        "<yellow>Thuộc tính hiện tại: <white>" + data.getAttributes().size()
                )));

        // Display current attributes
        if (!data.getAttributes().isEmpty()) {
            setItem(8, createItem(Material.BOOK,
                    "<aqua><bold>Thuộc Tính Hiện Tại</bold>",
                    data.getAttributes().stream()
                            .map(a -> "<gray>• <white>" + a.getAttribute() + " <yellow>+" + a.getAmount() + " <gray>(" + a.getSlot() + ")")
                            .toList()));
        }

        // Clear all button
        setItem(0, createItem(Material.TNT,
                "<red><bold>Xóa Tất Cả</bold>",
                List.of("<gray>Xóa mọi thuộc tính đã gắn", "<red>Không thể hoàn tác!")),
                ev -> {
                    data.getAttributes().clear();
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    player.sendMessage(TextUtil.success("Đã xóa tất cả thuộc tính!"));
                    refresh();
                });

        // Preset buttons
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(PRESETS.size(), slots.length); i++) {
            AttrPreset preset = PRESETS.get(i);
            setItem(slots[i], createItem(preset.icon(),
                    "<gold>" + preset.name(),
                    List.of(
                            "<gray>" + preset.desc(),
                            "",
                            "<gray>Key: <white>" + preset.key(),
                            "<gray>Mặc định: <white>+" + preset.defaultValue(),
                            "<gray>Slot: <white>" + preset.slot(),
                            "",
                            "<yellow>Click để thêm"
                    )), ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                promptForValue(preset);
            });
        }

        // Custom attribute button
        setItem(31, createItem(Material.COMMAND_BLOCK,
                "<light_purple><bold>⚙ Thuộc Tính Tùy Chỉnh</bold>",
                List.of(
                        "<gray>Nhập thuộc tính thủ công",
                        "<gray>Định dạng: attribute amount operation slot",
                        "<gray>Ví dụ: generic_attack_damage 15 ADD_NUMBER HAND",
                        "",
                        "<yellow>Click để nhập"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập thuộc tính <gray>(attribute amount operation slot)\n" +
                    "<gray>Ví dụ: generic_attack_damage 15 ADD_NUMBER HAND\n" +
                    "<gray>Gõ 'cancel' để hủy:",
                    input -> {
                        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("huy")) {
                            new AttributePresetGui(plugin, player, data).openAsChildOf(null);
                            return;
                        }
                        String[] parts = input.trim().split("\\s+");
                        if (parts.length < 4) {
                            player.sendMessage(TextUtil.error("Định dạng: attribute amount operation slot"));
                            new AttributePresetGui(plugin, player, data).open();
                            return;
                        }
                        try {
                            String attr = parts[0];
                            double amount = Double.parseDouble(parts[1]);
                            String operation = parts[2].toUpperCase();
                            String slot = parts[3].toUpperCase();
                            data.getAttributes().add(new UItemData.AttributeData(attr, amount, operation, slot));
                            plugin.getItemManager().registerItem(data);
                            plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                            player.sendMessage(TextUtil.success("Đã thêm thuộc tính: " + attr + " +" + amount));
                        } catch (Exception e) {
                            player.sendMessage(TextUtil.error("Lỗi: " + e.getMessage()));
                        }
                        new AttributePresetGui(plugin, player, data).open();
                    });
        });

        // Back
        setItem(49, createBackButton(), ev -> goBack());
    }

    private void promptForValue(AttrPreset preset) {
        // Ask for slot first if it's ANY
        if ("ANY".equals(preset.slot())) {
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Chọn slot cho <gold>" + preset.name() + "\n" +
                    "<gray>Nhập: HAND / OFF_HAND / HEAD / CHEST / LEGS / FEET / ANY\n" +
                    "<gray>Hoặc Enter để dùng ANY:",
                    slotInput -> {
                        String slot = slotInput.trim().isEmpty() ? "ANY" : slotInput.trim().toUpperCase();
                        promptForAmount(preset, slot);
                    });
        } else {
            promptForAmount(preset, preset.slot());
        }
    }

    private void promptForAmount(AttrPreset preset, String slot) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Nhập giá trị cho <gold>" + preset.name() + "\n" +
                "<gray>Mặc định: <white>" + preset.defaultValue() + "\n" +
                "<gray>Nhập số hoặc Enter để dùng mặc định:",
                input -> {
                    double amount = preset.defaultValue();
                    if (!input.trim().isEmpty()) {
                        try {
                            amount = Double.parseDouble(input.trim());
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Số không hợp lệ, dùng giá trị mặc định: " + preset.defaultValue()));
                        }
                    }
                    data.getAttributes().add(new UItemData.AttributeData(preset.key(), amount, preset.operation(), slot));
                    plugin.getItemManager().registerItem(data);
                    plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                    player.sendMessage(TextUtil.success("Đã thêm: " + preset.name() + " +" + amount + " (" + slot + ")"));
                    new AttributePresetGui(plugin, player, data).open();
                });
    }
}
