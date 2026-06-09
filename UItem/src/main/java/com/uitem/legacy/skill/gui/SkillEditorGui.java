package com.uitem.legacy.skill.gui;

import com.uitem.UItemData;
import com.uitem.gui.AbstractGui;
import com.uitem.gui.PotionPresetComboGui;
import com.uitem.gui.PotionPresetGui;
import com.uitem.UItemPlugin;
import com.uitem.legacy.skill.Skill;
import com.uitem.legacy.skill.SkillAction;
import com.uitem.legacy.skill.SkillTrigger;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor cho 1 skill/passive.
 * Cho phép:
 * - Đổi trigger
 * - Đặt cooldown + chance + cancel
 * - Thêm action nhanh (preset buttons)
 * - Nhập action chi tiết qua chat
 */
public class SkillEditorGui extends AbstractGui {

    private final UItemData itemData;
    private final Skill skill;
    private int actionPage = 0;

    private static final List<SkillTrigger> ACTIVE_TRIGGERS = List.of(
            SkillTrigger.RIGHT_CLICK, SkillTrigger.LEFT_CLICK,
            SkillTrigger.SHIFT_RIGHT_CLICK, SkillTrigger.SHIFT_LEFT_CLICK,
            SkillTrigger.DROP, SkillTrigger.SWAP_HAND,
            SkillTrigger.THROW, SkillTrigger.SHOOT   // THROW = drop with velocity; SHOOT = bow/crossbow/trident launch (new)
    );

    private static final List<SkillTrigger> PASSIVE_TRIGGERS = List.of(
            SkillTrigger.HIT, SkillTrigger.TAKE_DAMAGE, SkillTrigger.KILL,
            SkillTrigger.EQUIP, SkillTrigger.UNEQUIP,
            SkillTrigger.PERIODIC, SkillTrigger.WHILE_HOLDING, SkillTrigger.WHILE_WEARING
    );

    private static final List<List<SkillAction.ActionType>> ACTION_PAGES = List.of(
            List.of( // Page 1: Communication + Combat
                    SkillAction.ActionType.MESSAGE,
                    SkillAction.ActionType.ACTION_BAR,
                    SkillAction.ActionType.TITLE,
                    SkillAction.ActionType.PLAY_SOUND,
                    SkillAction.ActionType.SPAWN_PARTICLE,
                    SkillAction.ActionType.GIVE_POTION,
                    SkillAction.ActionType.HEAL,
                    SkillAction.ActionType.DAMAGE,
                    SkillAction.ActionType.SHOOT_PROJECTILE
            ),
            List.of( // Page 2: Commands + Movement
                    SkillAction.ActionType.CONSOLE_COMMAND,
                    SkillAction.ActionType.PLAYER_COMMAND,
                    SkillAction.ActionType.LAUNCH,
                    SkillAction.ActionType.TELEPORT,
                    SkillAction.ActionType.STRIKE_LIGHTNING,
                    SkillAction.ActionType.SET_FIRE,
                    SkillAction.ActionType.FEED,
                    SkillAction.ActionType.CONSUME_DURABILITY,
                    SkillAction.ActionType.CONSUME_ITEM
            ),
            List.of( // Page 3: Items + Effects
                    SkillAction.ActionType.GIVE_ITEM,
                    SkillAction.ActionType.REMOVE_POTION,
                    SkillAction.ActionType.BLOCK_BREAK,
                    SkillAction.ActionType.EXPLOSION,
                    SkillAction.ActionType.AREA_DAMAGE,
                    SkillAction.ActionType.POTION_AREA,
                    SkillAction.ActionType.BLIND,
                    SkillAction.ActionType.GLOW,
                    SkillAction.ActionType.CLEAR_EFFECTS
            ),
            List.of( // Page 4: Misc
                    SkillAction.ActionType.LEVITATE,
                    SkillAction.ActionType.SPAWN_MOB,
                    SkillAction.ActionType.FREEZE
            )
    );

    private static final java.util.Map<SkillAction.ActionType, Material> ACTION_MATERIALS = new java.util.LinkedHashMap<>();
    static {
        ACTION_MATERIALS.put(SkillAction.ActionType.MESSAGE, Material.PAPER);
        ACTION_MATERIALS.put(SkillAction.ActionType.ACTION_BAR, Material.NAME_TAG);
        ACTION_MATERIALS.put(SkillAction.ActionType.TITLE, Material.FILLED_MAP);
        ACTION_MATERIALS.put(SkillAction.ActionType.PLAY_SOUND, Material.NOTE_BLOCK);
        ACTION_MATERIALS.put(SkillAction.ActionType.SPAWN_PARTICLE, Material.BLAZE_POWDER);
        ACTION_MATERIALS.put(SkillAction.ActionType.GIVE_POTION, Material.POTION);
        ACTION_MATERIALS.put(SkillAction.ActionType.HEAL, Material.GOLDEN_APPLE);
        ACTION_MATERIALS.put(SkillAction.ActionType.DAMAGE, Material.IRON_SWORD);
        ACTION_MATERIALS.put(SkillAction.ActionType.SHOOT_PROJECTILE, Material.BOW);
        ACTION_MATERIALS.put(SkillAction.ActionType.CONSOLE_COMMAND, Material.COMMAND_BLOCK);
        ACTION_MATERIALS.put(SkillAction.ActionType.PLAYER_COMMAND, Material.REPEATER);
        ACTION_MATERIALS.put(SkillAction.ActionType.LAUNCH, Material.FIREWORK_ROCKET);
        ACTION_MATERIALS.put(SkillAction.ActionType.TELEPORT, Material.ENDER_PEARL);
        ACTION_MATERIALS.put(SkillAction.ActionType.STRIKE_LIGHTNING, Material.NETHER_STAR);
        ACTION_MATERIALS.put(SkillAction.ActionType.SET_FIRE, Material.FLINT_AND_STEEL);
        ACTION_MATERIALS.put(SkillAction.ActionType.FEED, Material.COOKED_BEEF);
        ACTION_MATERIALS.put(SkillAction.ActionType.CONSUME_DURABILITY, Material.ANVIL);
        ACTION_MATERIALS.put(SkillAction.ActionType.CONSUME_ITEM, Material.ITEM_FRAME);
        ACTION_MATERIALS.put(SkillAction.ActionType.GIVE_ITEM, Material.CHEST);
        ACTION_MATERIALS.put(SkillAction.ActionType.REMOVE_POTION, Material.GLASS_BOTTLE);
        ACTION_MATERIALS.put(SkillAction.ActionType.BLOCK_BREAK, Material.DIAMOND_PICKAXE);
        ACTION_MATERIALS.put(SkillAction.ActionType.EXPLOSION, Material.TNT);
        ACTION_MATERIALS.put(SkillAction.ActionType.AREA_DAMAGE, Material.DRAGON_BREATH);
        ACTION_MATERIALS.put(SkillAction.ActionType.POTION_AREA, Material.LINGERING_POTION);
        ACTION_MATERIALS.put(SkillAction.ActionType.BLIND, Material.INK_SAC);
        ACTION_MATERIALS.put(SkillAction.ActionType.GLOW, Material.GLOWSTONE_DUST);
        ACTION_MATERIALS.put(SkillAction.ActionType.CLEAR_EFFECTS, Material.MILK_BUCKET);
        ACTION_MATERIALS.put(SkillAction.ActionType.LEVITATE, Material.FEATHER);
        ACTION_MATERIALS.put(SkillAction.ActionType.SPAWN_MOB, Material.EGG);
        ACTION_MATERIALS.put(SkillAction.ActionType.FREEZE, Material.ICE);
    }

    public SkillEditorGui(UItemPlugin plugin, Player player, UItemData itemData, Skill skill) {
        super(plugin, player, 54, Messages.skillEditorTitle(skill.getId()));
        this.itemData = itemData;
        this.skill = skill;
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
        setItem(4, createItem(Material.ENCHANTED_BOOK,
                "<light_purple><bold>" + skill.getId() + "</bold> <dark_gray>- " + skill.getTrigger().getDisplayName(),
                List.of(
                        "<gray>Cooldown: <white>" + skill.getCooldown() + "s",
                        "<gray>Chance: <white>" + (int) skill.getChance() + "%",
                        "<gray>Actions: <white>" + skill.getActions().size()
                )));

        // === Trigger selection (active) ===
        setItem(9, createItem(Material.DIAMOND_SWORD, Messages.skillEditorActiveTriggers(), List.of("<gray>Right / Left Click")));

        int slot = 10;
        for (SkillTrigger t : ACTIVE_TRIGGERS) {
            boolean sel = t == skill.getTrigger();
            Material m = sel ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(slot++, createItem(m, (sel ? "<green>✔ " : "<gray>") + t.getDisplayName(),
                    List.of("<gray>" + t.getDescription())), ev -> {
                skill.setTrigger(t);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                saveAndRefresh();
            });
        }

        // === Passive triggers ===
        setItem(18, createItem(Material.CLOCK, Messages.skillEditorPassiveTriggers(), List.of()));

        slot = 19;
        for (SkillTrigger t : PASSIVE_TRIGGERS) {
            boolean sel = t == skill.getTrigger();
            Material m = sel ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            setItem(slot++, createItem(m, (sel ? "<green>✔ " : "<gray>") + t.getDisplayName(),
                    List.of("<gray>" + t.getDescription())), ev -> {
                skill.setTrigger(t);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
                saveAndRefresh();
            });
        }

        // === Cooldown (row 3) ===
        setItem(28, createItem(Material.CLOCK, String.format(Messages.skillEditorCooldown(), skill.getCooldown()),
                List.of("<yellow>" + Messages.skillEditorCooldownHint().replace("<yellow>", ""))), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player, "<yellow>Nhập cooldown (giây, ví dụ 4.5):", input -> {
                try {
                    double cd = Double.parseDouble(input.trim());
                    skill.setCooldown(cd);
                } catch (Exception ignored) {}
                saveAndReopen();
            }, this::reopen);
        });

        // Chance
        setItem(29, createItem(Material.GHAST_TEAR, "<gold><bold>Chance: <white>" + (int)skill.getChance() + "%</bold>",
                List.of("<gray>% kích hoạt (hữu ích cho passive)")), ev -> {
            plugin.getChatInputManager().requestInput(player, "<yellow>Nhập chance (0-100):", input -> {
                try { skill.setChance(Double.parseDouble(input.trim())); } catch (Exception ignored) {}
                saveAndReopen();
            }, this::reopen);
        });

        // Cancel event
        setItem(30, createItem(skill.isCancelEvent() ? Material.BARRIER : Material.PAPER,
                "<gold><bold>Cancel Event: <white>" + (skill.isCancelEvent() ? "BẬT" : "TẮT") + "</bold>",
                List.of("<gray>Chỉ ảnh hưởng active trigger (click/drop)",
                        "<yellow>Click để bật/tắt")), ev -> {
            skill.setCancelEvent(!skill.isCancelEvent());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            saveAndRefresh();
        });

        // === Actions section header (row 3) ===
        setItem(31, createItem(Material.WRITABLE_BOOK,
                Messages.skillEditorActionsHeader(skill.getActions().size()),
                List.of(Messages.skillEditorActionsHint())));

        // Action list items (row 3, slots 32-33 only to avoid overlap with potion presets at 34-35)
        List<SkillAction> acts = skill.getActions();
        int[] actSlots = {32, 33};
        for (int i = 0; i < Math.min(acts.size(), actSlots.length); i++) {
            final int idx = i;
            SkillAction a = acts.get(i);
            ItemStack icon = buildActionIcon(a);
            setItem(actSlots[i], icon, ev -> {
                if (idx >= skill.getActions().size()) return;
                if (ev.isRightClick()) {
                    skill.getActions().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    saveAndRefresh();
                } else {
                    editActionMainValue(a);
                }
            });
        }
        // Extra actions overflow (row 4, slots 36-43)
        int[] actSlotsExtra = {36, 37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < Math.min(acts.size() - actSlots.length, actSlotsExtra.length); i++) {
            final int idx = i + actSlots.length;
            if (idx >= acts.size()) break;
            SkillAction a = acts.get(idx);
            ItemStack icon = buildActionIcon(a);
            setItem(actSlotsExtra[i], icon, ev -> {
                if (idx >= skill.getActions().size()) return;
                if (ev.isRightClick()) {
                    skill.getActions().remove(idx);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    saveAndRefresh();
                } else {
                    editActionMainValue(a);
                }
            });
        }

        if (acts.isEmpty()) {
            setItem(31, createItem(Material.BARRIER, "<gray><italic>" + Messages.skillEditorNoActions() + "</italic>",
                    List.of("<yellow>Use the action buttons below to add actions")));
        }

        // Quick condition buttons (row 1, after active triggers)
        setItem(16, createItem(Material.EMERALD, "<green>Cond: Chance 75%", List.of("<gray>Click to add 75% chance")), ev -> {
            skill.addCondition(com.uitem.legacy.skill.SkillCondition.chance(75));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            saveAndRefresh();
        });
        setItem(17, createItem(Material.REDSTONE, "<red>Cond: Chance 50%", List.of("<gray>Click to add 50% chance")), ev -> {
            skill.addCondition(com.uitem.legacy.skill.SkillCondition.chance(50));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            saveAndRefresh();
        });

        // Potion presets (row 3, slots 34-35 - safe from action list)
        setItem(34, createItem(Material.REDSTONE, "<light_purple>⚡ Potion Presets",
                List.of("<gray>Click để xem danh sách potion")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            new PotionPresetGui(plugin, player, itemData, skill).openAsChildOf(this);
        });
        setItem(35, createItem(Material.BREWING_STAND, "<light_purple>🍶 Potion Combos",
                List.of("<gray>Click để xem preset combo")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            new PotionPresetComboGui(plugin, player, itemData, skill).openAsChildOf(this);
        });

        // === Action type buttons (paged, slots 44-52, slot 53 = navigation) ===
        List<SkillAction.ActionType> pageActions = ACTION_PAGES.get(actionPage);
        int actionSlot = 44;
        for (SkillAction.ActionType at : pageActions) {
            if (actionSlot >= 53) break;
            Material mat = ACTION_MATERIALS.getOrDefault(at, Material.PAPER);
            addActionTypeButton(actionSlot++, mat, at, () -> addActionByType(at));
        }
        for (int i = actionSlot; i < 53; i++) {
            setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        // Page navigation at slot 53
        int totalPages = ACTION_PAGES.size();
        if (actionPage < totalPages - 1) {
            setItem(53, createItem(Material.ARROW, "<yellow>Trang " + (actionPage + 2) + " ►",
                    List.of("<gray>Click để xem thêm action")), ev -> {
                actionPage++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                refresh();
            });
        } else {
            setItem(53, createItem(Material.ARROW, "<yellow>◄ Trang 1",
                    List.of("<gray>Quay lại trang đầu")), ev -> {
                actionPage = 0;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                refresh();
            });
        }

        // Back (slot 27 is safe - passive triggers end at slot 26 for 8 triggers)
        setItem(27, createBackButton(), ev -> goBack());
    }

    private ItemStack buildActionIcon(SkillAction a) {
        Material mat = Material.PAPER;
        String name = "<white>" + a.getType().getDisplay();
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Loại: <white>" + a.getType().name());

        // Hiển thị tham số chính cho dễ đọc
        switch (a.getType()) {
            case MESSAGE, ACTION_BAR -> {
                String txt = a.getStringParam("text", "");
                if (!txt.isEmpty()) lore.add("<gray>Nội dung: <white>" + (txt.length() > 25 ? txt.substring(0, 25) + "..." : txt));
            }
            case PLAY_SOUND -> lore.add("<gray>Âm thanh: <white>" + a.getStringParam("sound", "?"));
            case GIVE_POTION -> lore.add("<gray>Effect: <white>" + a.getStringParam("effect", "?") + " " + a.getIntParam("amplifier", 0));
            case HEAL, DAMAGE -> lore.add("<gray>Giá trị: <white>" + a.getDoubleParam("amount", 0));
            case SHOOT_PROJECTILE -> lore.add("<gray>Projectile: <white>" + a.getStringParam("projectile", "FIREBALL"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> {
                String cmd = a.getStringParam("command", "");
                if (!cmd.isEmpty()) lore.add("<gray>Lệnh: <white>" + (cmd.length() > 28 ? cmd.substring(0, 28) + "..." : cmd));
            }
            case LAUNCH -> lore.add("<gray>Velocity: <white>" + a.getDoubleParam("y", 0));
            case TELEPORT -> lore.add("<gray>Y offset: <white>" + a.getDoubleParam("y", 0));
            case STRIKE_LIGHTNING -> lore.add("<gray>Sét đánh tại mục tiêu");
            case SET_FIRE -> lore.add("<gray>Lửa: <white>" + a.getIntParam("seconds", 4) + "s");
            case FEED -> lore.add("<gray>Đói: <white>" + a.getIntParam("amount", 4));
            case CONSUME_DURABILITY -> lore.add("<gray>Độ bền: <white>" + a.getIntParam("amount", 1));
            case CONSUME_ITEM -> lore.add("<gray>Tiêu hao 1 item");
            case GIVE_ITEM -> lore.add("<gray>Vật phẩm: <white>" + a.getStringParam("material", "DIAMOND") + " x" + a.getIntParam("amount", 1));
            case REMOVE_POTION -> lore.add("<gray>Xóa effect: <white>" + a.getStringParam("effect", "all"));
            case BLOCK_BREAK -> {
                int r = a.getIntParam("range", 5);
                int rad = a.getIntParam("radius", 0);
                lore.add("<gray>Tầm: <white>" + r + (rad > 0 ? "  <yellow>Radius: " + rad + " (3x3+)" : ""));
            }
            case EXPLOSION -> lore.add("<gray>Power: <white>" + a.getDoubleParam("power", 3) + " Fire: " + a.getBooleanParam("fire", false));
            case AREA_DAMAGE -> lore.add("<gray>DMG: <white>" + a.getDoubleParam("amount", 5) + " Radius: " + a.getDoubleParam("radius", 5));
            case POTION_AREA -> lore.add("<gray>Effect: <white>" + a.getStringParam("effect", "?") + " TK: " + a.getDoubleParam("radius", 5));
            case BLIND, GLOW, LEVITATE -> lore.add("<gray>TGian: <white>" + a.getIntParam("duration", 5) + "s");
            case CLEAR_EFFECTS -> lore.add("<gray>Xóa toàn bộ hiệu ứng");
            case SPAWN_MOB -> lore.add("<gray>Entity: <white>" + a.getStringParam("entity", "ZOMBIE") + " x" + a.getIntParam("count", 1));
            case FREEZE -> lore.add("<gray>Đóng băng: <white>" + a.getIntParam("duration", 5) + "s");
        }

        lore.add("");
        lore.add("<yellow>Left: Edit value");
        lore.add("<red>Right: Delete");
        return createItem(mat, name, lore);
    }

    /** Add quick action type button (bilingual) */
    private void addActionTypeButton(int slot, Material mat, SkillAction.ActionType type, Runnable onClick) {
        String display = Messages.skillActionDisplay(type);
        setItem(slot, createItem(mat, "<green>+ " + display, List.of("<gray>" + type.getDisplay())), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            onClick.run();
        });
    }

    private void addActionByType(SkillAction.ActionType type) {
        switch (type) {
            case MESSAGE, ACTION_BAR, TITLE, PLAY_SOUND, GIVE_POTION, SHOOT_PROJECTILE,
                 CONSOLE_COMMAND, PLAYER_COMMAND, SPAWN_PARTICLE ->
                addActionWithPrompt(type);
            case HEAL, DAMAGE, FEED, SET_FIRE, CONSUME_DURABILITY ->
                addNumericAction(type);
            case LAUNCH, TELEPORT ->
                addActionWithPrompt(type);
            case BLOCK_BREAK, EXPLOSION, AREA_DAMAGE, POTION_AREA, BLIND,
                 GLOW, LEVITATE, SPAWN_MOB, FREEZE ->
                addActionWithPrompt(type);
            case STRIKE_LIGHTNING, CLEAR_EFFECTS, CONSUME_ITEM, GIVE_ITEM,
                 REMOVE_POTION, NOTHING ->
                addDirectAction(type);
        }
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
        refresh();
    }

    private void saveAndReopen() {
        plugin.getItemManager().registerItem(itemData);
        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
        reopen();
    }

    // ==================== Helpers thêm action hoàn toàn bằng GUI ====================

    /**
     * Thêm action và ngay lập tức hỏi giá trị chính qua chat (dễ dùng nhất cho text/command).
     * Action chỉ được thêm vào skill sau khi người dùng xác nhận input.
     */
    private void addActionWithPrompt(SkillAction.ActionType type) {
        SkillAction action = new SkillAction(type);

        // Đặt giá trị mặc định hợp lý cho một số loại
        switch (type) {
            case PLAY_SOUND -> {
                action.setParam("sound", "UI_BUTTON_CLICK");
                action.setParam("volume", 1.0);
                action.setParam("pitch", 1.0);
            }
            case GIVE_POTION -> {
                action.setParam("effect", "SPEED");
                action.setParam("duration", 10);
                action.setParam("amplifier", 0);
            }
            case SHOOT_PROJECTILE -> {
                action.setParam("projectile", "SMALL_FIREBALL");
                action.setParam("speed", 1.5);
                action.setParam("damage", 5.0);
            }
            case LAUNCH -> {
                action.setParam("x", 0);
                action.setParam("y", 1.2);
                action.setParam("z", 0);
                action.setParam("relative", true);
            }
            case TELEPORT -> {
                action.setParam("x", 0);
                action.setParam("y", 5);
                action.setParam("z", 0);
                action.setParam("relative", true);
            }
            case BLOCK_BREAK -> {
                action.setParam("range", 5);
                action.setParam("radius", 0);
            }
            case EXPLOSION -> {
                action.setParam("power", 3.0);
                action.setParam("fire", false);
            }
            case AREA_DAMAGE -> {
                action.setParam("amount", 5.0);
                action.setParam("radius", 5.0);
            }
            case POTION_AREA -> {
                action.setParam("effect", "POISON");
                action.setParam("duration", 5);
                action.setParam("amplifier", 0);
                action.setParam("radius", 5.0);
            }
            case SPAWN_PARTICLE -> {
                action.setParam("particle", "FLAME");
                action.setParam("count", 10);
                action.setParam("offset", 0.3);
            }
            case BLIND, GLOW, LEVITATE, FREEZE -> {
                action.setParam("duration", 5);
                if (type == SkillAction.ActionType.LEVITATE) action.setParam("amplifier", 0);
            }
            case SPAWN_MOB -> {
                action.setParam("entity", "ZOMBIE");
                action.setParam("count", 1);
            }
        }

        // Ask for the most important value first, before adding to skill
        // Chỉ addAction khi user cung cấp input hợp lệ
        switch (type) {
            case MESSAGE -> promptForActionParamThenAdd(action, "text", "Enter message content (supports %player% %target%):");
            case ACTION_BAR -> promptForActionParamThenAdd(action, "text", "Enter action bar text:");
            case TITLE -> promptForActionParamThenAdd(action, "title", "Enter Title:");
            case PLAY_SOUND -> promptForActionParamThenAdd(action, "sound", "Enter sound name (e.g. ENTITY_BLAZE_SHOOT):");
            case SPAWN_PARTICLE -> promptForActionParamThenAdd(action, "particle", "Enter particle name (e.g. FLAME, HEART):");
            case GIVE_POTION -> promptForActionParamThenAdd(action, "effect", "Enter potion effect (e.g. SPEED, STRENGTH):");
            case SHOOT_PROJECTILE -> promptForActionParamThenAdd(action, "projectile", "Projectile (SMALL_FIREBALL / ARROW / WITHER_SKULL...):");
            case CONSOLE_COMMAND, PLAYER_COMMAND -> promptForActionParamThenAdd(action, "command", "Enter command (supports %player% %target%):");
            case LAUNCH -> promptForActionParamThenAdd(action, "y", "Enter launch Y power (e.g. 1.5):");
            case TELEPORT -> promptForActionParamThenAdd(action, "y", "Enter teleport Y offset:");
            case BLOCK_BREAK -> promptForActionParamThenAdd(action, "range", "Enter block break range (1-10). Radius=0 for single block; set later via edit or YAML:");
            case EXPLOSION -> promptForActionParamThenAdd(action, "power", "Enter explosion power (0.1-10):");
            case AREA_DAMAGE -> promptForActionParamThenAdd(action, "amount", "Enter area damage amount:");
            case POTION_AREA -> promptForActionParamThenAdd(action, "effect", "Enter area potion effect (e.g. POISON, WITHER):");
            case BLIND -> promptForActionParamThenAdd(action, "duration", "Enter blind duration (seconds):");
            case GLOW -> promptForActionParamThenAdd(action, "duration", "Enter glow duration (seconds):");
            case LEVITATE -> promptForActionParamThenAdd(action, "duration", "Enter levitate duration (seconds):");
            case SPAWN_MOB -> promptForActionParamThenAdd(action, "entity", "Enter entity type (e.g. ZOMBIE, SKELETON, WOLF):");
            case FREEZE -> promptForActionParamThenAdd(action, "duration", "Enter freeze duration (seconds):");
            default -> {
                action.setParam("value", 1);
                skill.addAction(action);
                player.sendMessage(TextUtil.success(Messages.skillAdded(type.name())));
                saveAndRefresh();
            }
        }
    }

    /**
     * Thêm action có giá trị số, cho phép chọn nhanh bằng button trước khi hỏi custom.
     * Action chỉ được thêm vào skill sau khi người dùng xác nhận input.
     */
    private void addNumericAction(SkillAction.ActionType type) {
        String prompt;
        double defaultValue = 1.0;
        String paramKey = "amount";

        switch (type) {
            case HEAL -> { prompt = "Enter heal amount (e.g. 3.5):"; defaultValue = 3.0; }
            case DAMAGE -> { prompt = "Enter damage amount (e.g. 5):"; defaultValue = 4.0; }
            case FEED -> { prompt = "Enter food amount (1-20):"; defaultValue = 4; }
            case SET_FIRE -> { prompt = "Enter fire duration (seconds):"; defaultValue = 4; paramKey = "seconds"; }
            case CONSUME_DURABILITY -> { prompt = "Enter durability cost:"; defaultValue = 1; }
            default -> prompt = "Enter value:";
        }

        SkillAction action = new SkillAction(type);
        if (paramKey.equals("seconds")) {
            action.setParam(paramKey, (int) defaultValue);
        } else {
            action.setParam(paramKey, defaultValue);
        }

        promptForActionParamThenAdd(action, paramKey, prompt);
    }

    /** Thêm action không cần input (có giá trị mặc định tốt) */
    private void addDirectAction(SkillAction.ActionType type) {
        SkillAction action = new SkillAction(type);
        skill.addAction(action);
        player.sendMessage(TextUtil.success(Messages.skillAdded(type.getDisplay())));
        saveAndRefresh();
    }

    /** Hỏi giá trị 1 param rồi lưu + refresh (action đã tồn tại trong skill) */
    private void promptForActionParam(SkillAction action, String paramKey, String promptMessage, String successMsg) {
        plugin.getChatInputManager().requestInput(player, "<yellow>" + promptMessage, input -> {
            try {
                String val = input.trim();
                if (paramKey.equals("amount") || paramKey.equals("y") || paramKey.equals("seconds") || paramKey.equals("speed") || paramKey.equals("damage")) {
                    double num = Double.parseDouble(val);
                    action.setParam(paramKey, num);
                } else {
                    action.setParam(paramKey, val);
                }
                player.sendMessage(TextUtil.success(successMsg));
            } catch (Exception e) {
                player.sendMessage(TextUtil.info("Added with default value (click the action again to edit)."));
            }
            saveAndReopen();
        }, this::reopen);
    }

    /**
     * Hỏi giá trị param, chỉ add action vào skill nếu input hợp lệ.
     */
    private void promptForActionParamThenAdd(SkillAction action, String paramKey, String promptMessage) {
        plugin.getChatInputManager().requestInput(player, "<yellow>" + promptMessage, input -> {
            try {
                String val = input.trim();
                if (paramKey.equals("amount") || paramKey.equals("y") || paramKey.equals("seconds") || paramKey.equals("speed") || paramKey.equals("damage")) {
                    double num = Double.parseDouble(val);
                    action.setParam(paramKey, num);
                } else {
                    action.setParam(paramKey, val);
                }
            } catch (Exception e) {
                player.sendMessage(TextUtil.info("Using default value."));
            }
            skill.addAction(action);
            player.sendMessage(TextUtil.success(Messages.skillAdded(action.getType().getDisplay())));
            saveAndReopen();
        }, this::reopen);
    }

    /**
     * Khi click vào action đã có trong list → cho sửa giá trị chính.
     */
    private void editActionMainValue(SkillAction action) {
        switch (action.getType()) {
            case MESSAGE, ACTION_BAR -> promptForActionParam(action, "text", "Enter new content:", Messages.skillUpdated("content"));
            case PLAY_SOUND -> promptForActionParam(action, "sound", "Enter new sound:", Messages.skillUpdated("sound"));
            case GIVE_POTION -> promptForActionParam(action, "effect", "Enter new effect (e.g. STRENGTH):", Messages.skillUpdated("potion"));
            case HEAL, DAMAGE, AREA_DAMAGE -> promptForActionParam(action, "amount", "Enter new value:", Messages.skillUpdated("value"));
            case SHOOT_PROJECTILE -> promptForActionParam(action, "projectile", "Enter new projectile type:", Messages.skillUpdated("projectile"));
            case CONSOLE_COMMAND, PLAYER_COMMAND -> promptForActionParam(action, "command", "Enter new command:", Messages.skillUpdated("command"));
            case LAUNCH -> promptForActionParam(action, "y", "Enter new Y power:", Messages.skillUpdated("launch"));
            case TELEPORT -> promptForActionParam(action, "y", "Enter new Y offset:", Messages.skillUpdated("teleport"));
            case CONSUME_DURABILITY -> promptForActionParam(action, "amount", "Enter durability cost:", Messages.skillUpdated("durability"));
            case BLOCK_BREAK -> {
                // Chain: edit range, then immediately edit radius for convenience (3x3 support)
                plugin.getChatInputManager().requestInput(player, "<yellow>Enter new range (1-10):", input -> {
                    try {
                        action.setParam("range", Integer.parseInt(input.trim()));
                    } catch (Exception ignored) {}
                    // Now prompt radius right after
                    plugin.getChatInputManager().requestInput(player, "<yellow>Enter radius for area (0=single, 1=3x3, 2=5x5...):", rinput -> {
                        try {
                            action.setParam("radius", Integer.parseInt(rinput.trim()));
                        } catch (Exception ignored) {}
                        player.sendMessage(TextUtil.success(Messages.skillUpdated("range+radius")));
                        saveAndReopen();
                    }, this::reopen);
                }, this::reopen);
            }
            case EXPLOSION -> promptForActionParam(action, "power", "Enter new explosion power:", Messages.skillUpdated("power"));
            case POTION_AREA -> promptForActionParam(action, "effect", "Enter new effect:", Messages.skillUpdated("effect"));
            case BLIND, GLOW, LEVITATE, FREEZE -> promptForActionParam(action, "duration", "Enter new duration (seconds):", Messages.skillUpdated("duration"));
            case SPAWN_MOB -> promptForActionParam(action, "entity", "Enter new entity type:", Messages.skillUpdated("entity"));
            default -> {
                player.sendMessage(TextUtil.info("This action mainly uses default values. You can delete and re-add it."));
            }
        }
    }
}
