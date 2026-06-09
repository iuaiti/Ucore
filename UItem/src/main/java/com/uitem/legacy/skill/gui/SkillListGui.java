package com.uitem.legacy.skill.gui;

import com.uitem.UItemData;
import com.uitem.gui.AbstractGui;
import com.uitem.UItemPlugin;
import com.uitem.legacy.skill.SkillDataHelper;
import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillTrigger;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách skill/passive của item.
 * Cho phép thêm, xóa, chỉnh sửa skill.
 */
public class SkillListGui extends AbstractGui {

    private final UItemData data;

    public SkillListGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, "<light_purple><bold>Skills & Passives</bold> <dark_gray>- " + data.getId());
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

        setItem(4, createItem(Material.ENCHANTED_BOOK,
                Messages.skillListHeader(data.getId()),
                List.of(
                        Messages.skillListDesc(),
                        "<yellow>Easy & Stable • Folia-safe",
                        Messages.skillListClickEdit() + " | " + Messages.skillListClickDelete()
                )));

        // Current skills — parse from raw data
        List<Skill> skills = SkillDataHelper.getSkills(data);
        int[] slots = {10,11,12,13,14,15,16,17, 19,20,21,22,23,24,25,26};

        for (int i = 0; i < Math.min(skills.size(), slots.length); i++) {
            final int idx = i;
            Skill sk = skills.get(i);
            ItemStack icon = buildSkillIcon(sk);
            setItem(slots[i], icon, ev -> {
                if (ev.isShiftClick() && ev.isRightClick()) {
                    // Shift + Right Click = Xóa
                    if (idx < skills.size()) {
                        skills.remove(idx);
                        SkillDataHelper.setSkills(data, skills);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.0f);
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                        refresh();
                    }
                } else {
                    // Left Click = Edit
                    if (idx < skills.size()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                        new SkillEditorGui(plugin, player, data, skills.get(idx)).openAsChildOf(this);
                    }
                }
            });
        }

        if (skills.isEmpty()) {
            setItem(10, createItem(Material.BARRIER, "<gray><italic>" + Messages.skillEditorNoActions().replace("action", "skill") + "</italic>",
                    List.of("<yellow>Click button below to add new skill")));
        }

        // Add new skill
        setItem(40, createItem(Material.EMERALD_BLOCK, Messages.skillListAdd(),
                List.of(
                        Messages.skillListAddDesc(),
                        "<yellow>Click to create"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            // Tạo skill trống với ID ngẫu nhiên
            String newId = "skill_" + (skills.size() + 1);
            Skill newSkill = new Skill(newId);
            newSkill.setTrigger(SkillTrigger.RIGHT_CLICK);
            newSkill.setCooldown(3.0);
            newSkill.setCancelEvent(true);
            skills.add(newSkill);
            SkillDataHelper.setSkills(data, skills);

            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());

            new SkillEditorGui(plugin, player, data, newSkill).openAsChildOf(this);
        });

        // Quick add presets
        setItem(42, createItem(Material.FIRE_CHARGE, Messages.skillListPresetFireball(),
                List.of(Messages.skillListPresetFireballDesc())), ev -> {
            addPresetFireball();
            refresh();
        });

        setItem(43, createItem(Material.GOLDEN_APPLE, Messages.skillListPresetLifesteal(),
                List.of(Messages.skillListPresetLifestealDesc())), ev -> {
            addPresetLifesteal();
            refresh();
        });

        setItem(44, createItem(Material.CLOCK, Messages.skillListPresetRegen(),
                List.of(Messages.skillListPresetRegenDesc())), ev -> {
            addPresetRegenAura();
            refresh();
        });

        // Back
        setItem(53, createBackButton(), ev -> goBack());
    }

    private ItemStack buildSkillIcon(Skill sk) {
        Material mat = switch (sk.getTrigger()) {
            case RIGHT_CLICK, SHIFT_RIGHT_CLICK -> Material.DIAMOND_SWORD;
            case LEFT_CLICK, SHIFT_LEFT_CLICK -> Material.IRON_SWORD;
            case HIT -> Material.IRON_AXE;
            case TAKE_DAMAGE -> Material.SHIELD;
            case EQUIP, WHILE_WEARING -> Material.DIAMOND_CHESTPLATE;
            case PERIODIC, WHILE_HOLDING -> Material.CLOCK;
            case DROP -> Material.DROPPER;
            default -> Material.ENCHANTED_BOOK;
        };

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Trigger: <white>" + sk.getTrigger().getDisplayName());
        lore.add("<gray>Cooldown: <white>" + sk.getCooldown() + "s");
        if (sk.getChance() < 100) lore.add("<gray>Chance: <white>" + (int)sk.getChance() + "%");
        lore.add("<gray>Actions: <white>" + sk.getActions().size());
        lore.add("");
        lore.add(Messages.skillListClickEdit());
        lore.add("<red>Shift + Right-click để xóa");
        return createItem(mat, "<light_purple>" + sk.getId() + " <dark_gray>→ <white>" + sk.getTrigger().name(), lore);
    }

    private void addPresetFireball() {
        Skill sk = new Skill("fireball");
        sk.setDisplayName("<red>Fireball");
        sk.setTrigger(SkillTrigger.RIGHT_CLICK);
        sk.setCooldown(6.0);
        sk.setCancelEvent(true);
        sk.addAction(com.ultra.ucore.skill.SkillAction.playSound("ENTITY_BLAZE_SHOOT", 1.0f, 1.0f));
        sk.addAction(com.ultra.ucore.skill.SkillAction.shootProjectile("SMALL_FIREBALL", 1.6, 7.0));
        sk.addAction(        com.ultra.ucore.skill.SkillAction.actionBar("<red>Fireball!"));
        List<Skill> allSk = SkillDataHelper.getSkills(data);
        allSk.add(sk);
        SkillDataHelper.setSkills(data, allSk);

        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
        player.sendMessage(TextUtil.success(Messages.skillPresetAdded("Fireball")));
    }

    private void addPresetLifesteal() {
        Skill sk = new Skill("lifesteal");
        sk.setDisplayName("<red>Lifesteal");
        sk.setTrigger(SkillTrigger.HIT);
        sk.setCooldown(0);
        sk.setChance(35);
        sk.addAction(com.ultra.ucore.skill.SkillAction.heal(2.5));
        sk.addAction(com.ultra.ucore.skill.SkillAction.actionBar("<red>+2.5 HP"));
        List<Skill> allSk = SkillDataHelper.getSkills(data);
        allSk.add(sk);
        SkillDataHelper.setSkills(data, allSk);

        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
        player.sendMessage(TextUtil.success(Messages.skillPresetAdded("Lifesteal")));
    }

    private void addPresetRegenAura() {
        Skill sk = new Skill("regen_aura");
        sk.setDisplayName("<aqua>Regen Aura");
        sk.setTrigger(SkillTrigger.WHILE_HOLDING);
        sk.setCooldown(4.0); // interval
        sk.setPeriodTicks(20);
        sk.addAction(com.ultra.ucore.skill.SkillAction.heal(1.0));
        sk.addAction(com.ultra.ucore.skill.SkillAction.playSound("BLOCK_NOTE_BLOCK_HARP", 0.6f, 1.8f));
        List<Skill> allSk = SkillDataHelper.getSkills(data);
        allSk.add(sk);
        SkillDataHelper.setSkills(data, allSk);

        plugin.getItemManager().registerItem(data);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
        player.sendMessage(TextUtil.success(Messages.skillPresetAdded("Regen Aura")));
    }
}
