package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.legacy.skill.Skill;
import com.uitem.legacy.skill.SkillAction;
import com.uitem.util.PotionEffectRegistry;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * GUI để chọn nhanh potion effect và cấu hình duration/amplifier.
 */
public class PotionPresetGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 16; // 4x4 grid (slot 10-26, skip borders)

    public PotionPresetGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, "<light_purple><bold>Chọn Potion Effect</bold>");
        this.itemData = itemData;
        this.skill = skill;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        int totalEffects = PotionEffectRegistry.getTotalEffects();
        int totalPages = (int) Math.ceil((double) totalEffects / ITEMS_PER_PAGE);
        
        setItem(4, createItem(Material.POTION,
                "<light_purple><bold>Potion Effects</bold>",
                List.of(
                        "<gray>Trang: <white>" + (currentPage + 1) + "/" + totalPages,
                        "<gray>Tổng: <white>" + totalEffects + " effects",
                        "",
                        "<yellow>Click để thêm potion"
                )));

        // Display potion effects in grid
        List<PotionEffectRegistry.PotionInfo> allEffects = new ArrayList<>(PotionEffectRegistry.getAllEffects().values());
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, allEffects.size());

        int slot = 10;
        int slotsInRow = 0;
        for (int i = startIdx; i < endIdx; i++) {
            if (slotsInRow == 8) {
                slotsInRow = 0;
                slot += 3; // skip to next row (9-1=8, then +3 = +2 for border)
            }

            final PotionEffectRegistry.PotionInfo info = allEffects.get(i);
            ItemStack icon = createPotionIcon(info);
            final int idx = i;
            
            setItem(slot, icon, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                showPotionDurationPrompt(info);
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

        if (endIdx < allEffects.size()) {
            setItem(50, createItem(Material.ARROW, "<gray>Next Page >"), ev -> {
                currentPage++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                refresh();
            });
        }

        // Back
        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack createPotionIcon(PotionEffectRegistry.PotionInfo info) {
        return createItem(Material.POTION,
                "<aqua>" + info.displayName,
                List.of(
                        "<gray>Effect: <white>" + info.key,
                        "<gray>Desc: <white>" + info.description,
                        "",
                        "<yellow>Click để chọn"
                ));
    }

    private void showPotionDurationPrompt(PotionEffectRegistry.PotionInfo info) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Nhập duration (giây, ví dụ: 10):",
                input -> {
                    try {
                        int duration = Integer.parseInt(input.trim());
                        showPotionAmplifierPrompt(info, duration);
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.error("Invalid number. Using default 10 seconds."));
                        showPotionAmplifierPrompt(info, 10);
                    }
                });
    }

    private void showPotionAmplifierPrompt(PotionEffectRegistry.PotionInfo info, int duration) {
        plugin.getChatInputManager().requestInput(player,
                "<yellow>Nhập amplifier (0-5, ví dụ: 0 cho cấp 1):",
                input -> {
                    try {
                        int amplifier = Math.max(0, Math.min(5, Integer.parseInt(input.trim())));
                        addPotionAction(info, duration, amplifier);
                    } catch (NumberFormatException e) {
                        player.sendMessage(TextUtil.error("Invalid number. Using default 0."));
                        addPotionAction(info, duration, 0);
                    }
                });
    }

    private void addPotionAction(PotionEffectRegistry.PotionInfo info, int duration, int amplifier) {
        SkillAction action = SkillAction.givePotion(info.key, duration, amplifier);
        skill.addAction(action);

        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());

        String msg = String.format("<green>✓ Added potion: %s (%ds, Amplifier %d)", 
            info.displayName, duration, amplifier + 1);
        player.sendMessage(TextUtil.success(msg));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        // Quay lại SkillEditorGui (dùng goBack để giữ chuỗi parent)
        goBack();
    }
}
