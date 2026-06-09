package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.legacy.skill.SkillDataHelper;
import com.ultra.ucore.skill.Skill;
import com.ultra.uskill.effect.PotionEffectHelper;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * GUI để chọn potion preset combos (buff combos, debuff combos, v.v.).
 */
public class PotionPresetComboGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 16;

    public PotionPresetComboGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, "<light_purple><bold>Potion Preset Combos</bold>");
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        int totalPresets = PotionEffectHelper.getTotalPresets();
        int totalPages = (int) Math.ceil((double) totalPresets / ITEMS_PER_PAGE);

        setItem(4, createItem(Material.BREWING_STAND,
                "<light_purple><bold>Potion Combos</bold>",
                List.of(
                        "<gray>Trang: <white>" + (currentPage + 1) + "/" + totalPages,
                        "<gray>Tổng: <white>" + totalPresets + " presets",
                        "",
                        "<yellow>Click để thêm combo"
                )));

        // Display presets in grid
        List<PotionEffectHelper.PotionPreset> allPresets = new ArrayList<>(PotionEffectHelper.getAllPresets().values());
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, allPresets.size());

        int slot = 10;
        int slotsInRow = 0;
        for (int i = startIdx; i < endIdx; i++) {
            if (slotsInRow == 8) {
                slotsInRow = 0;
                slot += 3;
            }

            final PotionEffectHelper.PotionPreset preset = allPresets.get(i);
            ItemStack icon = createPresetIcon(preset);
            
            setItem(slot, icon, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                confirmAddPreset(preset);
            });

            slot++;
            slotsInRow++;
        }

        // Navigation
        if (currentPage > 0) {
            setItem(48, createItem(Material.ARROW, "<gray>< Previous Page"), ev -> {
                currentPage--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        if (endIdx < allPresets.size()) {
            setItem(50, createItem(Material.ARROW, "<gray>Next Page >"), ev -> {
                currentPage++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        // Back
        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack createPresetIcon(PotionEffectHelper.PotionPreset preset) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Preset Combo:");
        for (PotionEffectHelper.PotionEffectCombo combo : preset.effects) {
            String level = combo.amplifier >= 1 ? " " + toRoman(combo.amplifier + 1) : "";
            lore.add("<aqua>  • " + combo.effectType + level + " (" + combo.durationSeconds + "s)");
        }
        lore.add("");
        lore.add("<yellow>Click để thêm");

        return createItem(Material.POTION,
                "<light_purple>" + preset.name,
                lore);
    }

    private void confirmAddPreset(PotionEffectHelper.PotionPreset preset) {
        PotionEffectHelper.addPresetToSkill(skill, preset);
        SkillDataHelper.mergeSkill(itemData, skill);

        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());

        String msg = String.format("<green>✓ Added preset: %s (%d effects)",
            preset.name, preset.effects.size());
        player.sendMessage(TextUtil.success(msg));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        // Quay lại SkillEditorGui (giữ chuỗi parent)
        goBack();
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> String.valueOf(num);
        };
    }
}
