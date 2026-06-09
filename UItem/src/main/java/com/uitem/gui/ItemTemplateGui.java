package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.legacy.skill.SkillDataHelper;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI chọn template item sẵn (giống Oraxen/ItemsAdder).
 * Người chơi chọn loại item → plugin tự điền thuộc tính mặc định phù hợp.
 * Thay vì phải config tay từng thứ.
 */
public class ItemTemplateGui extends AbstractGui {

    private final String newItemId;

    public ItemTemplateGui(UItemPlugin plugin, Player player, String newItemId) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Chọn Template Item</bold></gradient>");
        this.newItemId = newItemId;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // Header
        setItem(4, createItem(Material.NETHER_STAR,
                "<gradient:#ff6b6b:#ffd93d><bold>Chọn Template</bold></gradient>",
                List.of(
                        "<gray>Chọn loại item để tự động điền thuộc tính",
                        "<gray>Bạn có thể chỉnh sửa sau trong Item Editor",
                        "",
                        "<yellow>ID: <white>" + newItemId
                )));

        // ===== WEAPONS =====
        setItem(10, createItem(Material.DIAMOND_SWORD,
                "<red><bold>⚔ Kiếm (Sword)</bold>",
                List.of(
                        "<gray>Vũ khí cận chiến cơ bản",
                        "<gray>• Sharpness V, Unbreaking III",
                        "<gray>• Attack Damage +7",
                        "<gray>• Unbreakable",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createSwordTemplate()));

        setItem(11, createItem(Material.DIAMOND_AXE,
                "<gold><bold>🪓 Rìu (Axe)</bold>",
                List.of(
                        "<gray>Vũ khí/công cụ đa năng",
                        "<gray>• Sharpness III, Efficiency V",
                        "<gray>• Attack Damage +9",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createAxeTemplate()));

        setItem(12, createItem(Material.BOW,
                "<aqua><bold>🏹 Cung (Bow)</bold>",
                List.of(
                        "<gray>Vũ khí tầm xa",
                        "<gray>• Power V, Infinity",
                        "<gray>• Punch II",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createBowTemplate()));

        setItem(13, createItem(Material.CROSSBOW,
                "<light_purple><bold>🎯 Nỏ (Crossbow)</bold>",
                List.of(
                        "<gray>Vũ khí tầm xa mạnh hơn",
                        "<gray>• Quick Charge III, Multishot",
                        "<gray>• Piercing IV",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createCrossbowTemplate()));

        setItem(14, createItem(Material.TRIDENT,
                "<blue><bold>🔱 Đinh Ba (Trident)</bold>",
                List.of(
                        "<gray>Vũ khí huyền thoại",
                        "<gray>• Loyalty III, Channeling",
                        "<gray>• Riptide III, Impaling V",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createTridentTemplate()));

        setItem(15, createItem(Material.MACE,
                "<dark_red><bold>🔨 Chùy (Mace)</bold>",
                List.of(
                        "<gray>Vũ khí đặc biệt 1.21+",
                        "<gray>• Density V, Breach IV",
                        "<gray>• Wind Burst III",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createMaceTemplate()));

        // ===== ARMOR =====
        setItem(19, createItem(Material.DIAMOND_HELMET,
                "<aqua><bold>⛑ Mũ (Helmet)</bold>",
                List.of(
                        "<gray>Giáp đầu",
                        "<gray>• Protection IV, Unbreaking III",
                        "<gray>• Respiration III, Aqua Affinity",
                        "<gray>• Armor +3",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createHelmetTemplate()));

        setItem(20, createItem(Material.DIAMOND_CHESTPLATE,
                "<aqua><bold>🛡 Áo Giáp (Chestplate)</bold>",
                List.of(
                        "<gray>Giáp thân",
                        "<gray>• Protection IV, Unbreaking III",
                        "<gray>• Thorns III",
                        "<gray>• Armor +8",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createChestplateTemplate()));

        setItem(21, createItem(Material.DIAMOND_LEGGINGS,
                "<aqua><bold>👖 Quần Giáp (Leggings)</bold>",
                List.of(
                        "<gray>Giáp chân",
                        "<gray>• Protection IV, Unbreaking III",
                        "<gray>• Swift Sneak III",
                        "<gray>• Armor +6",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createLeggingsTemplate()));

        setItem(22, createItem(Material.DIAMOND_BOOTS,
                "<aqua><bold>👟 Giày (Boots)</bold>",
                List.of(
                        "<gray>Giáp chân",
                        "<gray>• Protection IV, Feather Falling IV",
                        "<gray>• Depth Strider III",
                        "<gray>• Armor +3",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createBootsTemplate()));

        // ===== TOOLS =====
        setItem(28, createItem(Material.DIAMOND_PICKAXE,
                "<gray><bold>⛏ Cuốc (Pickaxe)</bold>",
                List.of(
                        "<gray>Công cụ khai thác",
                        "<gray>• Efficiency V, Fortune III",
                        "<gray>• Unbreaking III, Mending",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createPickaxeTemplate()));

        setItem(29, createItem(Material.DIAMOND_SHOVEL,
                "<yellow><bold>🪣 Xẻng (Shovel)</bold>",
                List.of(
                        "<gray>Công cụ đào đất",
                        "<gray>• Efficiency V, Fortune III",
                        "<gray>• Unbreaking III",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createShovelTemplate()));

        setItem(30, createItem(Material.DIAMOND_HOE,
                "<green><bold>🌾 Cuốc Nông (Hoe)</bold>",
                List.of(
                        "<gray>Công cụ nông nghiệp",
                        "<gray>• Efficiency V, Fortune III",
                        "<gray>• Silk Touch",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createHoeTemplate()));

        // ===== SPECIAL =====
        setItem(32, createItem(Material.GOLDEN_APPLE,
                "<gold><bold>🍎 Thức Ăn (Food)</bold>",
                List.of(
                        "<gray>Item thức ăn đặc biệt",
                        "<gray>• Glowing effect",
                        "<gray>• Lore mô tả hiệu ứng",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createFoodTemplate()));

        setItem(33, createItem(Material.TOTEM_OF_UNDYING,
                "<light_purple><bold>✨ Bùa Hộ Mệnh (Totem)</bold>",
                List.of(
                        "<gray>Item phòng thủ đặc biệt",
                        "<gray>• Unbreakable",
                        "<gray>• Glowing",
                        "<gray>• Rarity: LEGENDARY",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createTotemTemplate()));

        setItem(34, createItem(Material.ENDER_PEARL,
                "<dark_purple><bold>🌀 Bùa Dịch Chuyển (Teleport)</bold>",
                List.of(
                        "<gray>Item dịch chuyển",
                        "<gray>• Skill: Right Click → Teleport",
                        "<gray>• Cooldown 10s",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createTeleportTemplate()));

        setItem(35, createItem(Material.BLAZE_ROD,
                "<red><bold>🔥 Đũa Phép (Wand)</bold>",
                List.of(
                        "<gray>Vũ khí phép thuật",
                        "<gray>• Skill: Right Click → Fireball",
                        "<gray>• Cooldown 5s",
                        "<gray>• Glowing",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createWandTemplate()));

        // Blank template
        setItem(40, createItem(Material.PAPER,
                "<white><bold>📄 Trống (Blank)</bold>",
                List.of(
                        "<gray>Tạo item trống không có thuộc tính",
                        "<gray>Tự cấu hình hoàn toàn",
                        "",
                        "<yellow>Click để tạo"
                )), ev -> applyTemplate(createBlankTemplate()));

        // Back
        setItem(49, createBackButton(), ev -> goBack());
    }

    private void applyTemplate(UItemData template) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
        // Register and save
        plugin.getItemManager().registerItem(template);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveItem(plugin.getItemManager(), template);
        player.sendMessage(TextUtil.success("Đã tạo item '" + template.getId() + "' từ template!"));
        // Open editor
        new ItemEditorGui(plugin, player, template).open();
    }

    // ===== TEMPLATE FACTORIES =====

    private UItemData createBlankTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.STONE);
        d.setDisplayName("<white>" + newItemId);
        return d;
    }

    private UItemData createSwordTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_SWORD);
        d.setDisplayName("<gradient:#ff6b6b:#ffd93d><bold>" + formatId(newItemId) + "</bold></gradient>");
        d.setLore(List.of(
                "<gray>Một thanh kiếm huyền thoại",
                "<dark_gray>──────────────",
                "<red>⚔ Sát thương: <white>+7",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("sharpness", 5);
        d.getEnchantments().put("unbreaking", 3);
        d.getEnchantments().put("mending", 1);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.getAttributes().add(new UItemData.AttributeData("generic_attack_damage", 7.0, "ADD_NUMBER", "HAND"));
        d.setRarity("RARE");
        return d;
    }

    private UItemData createAxeTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_AXE);
        d.setDisplayName("<gold><bold>" + formatId(newItemId) + "</bold></gold>");
        d.setLore(List.of(
                "<gray>Rìu chiến mạnh mẽ",
                "<dark_gray>──────────────",
                "<red>⚔ Sát thương: <white>+9",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("sharpness", 3);
        d.getEnchantments().put("efficiency", 5);
        d.getEnchantments().put("unbreaking", 3);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.getAttributes().add(new UItemData.AttributeData("generic_attack_damage", 9.0, "ADD_NUMBER", "HAND"));
        d.setRarity("UNCOMMON");
        return d;
    }

    private UItemData createBowTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.BOW);
        d.setDisplayName("<aqua><bold>" + formatId(newItemId) + "</bold></aqua>");
        d.setLore(List.of(
                "<gray>Cung thần tốc",
                "<dark_gray>──────────────",
                "<aqua>🏹 Tầm xa: <white>Vô hạn",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("power", 5);
        d.getEnchantments().put("infinity", 1);
        d.getEnchantments().put("punch", 2);
        d.getEnchantments().put("unbreaking", 3);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.setRarity("RARE");
        return d;
    }

    private UItemData createCrossbowTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.CROSSBOW);
        d.setDisplayName("<light_purple><bold>" + formatId(newItemId) + "</bold></light_purple>");
        d.setLore(List.of(
                "<gray>Nỏ xuyên giáp",
                "<dark_gray>──────────────",
                "<light_purple>🎯 Xuyên: <white>4 mục tiêu",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("quick_charge", 3);
        d.getEnchantments().put("multishot", 1);
        d.getEnchantments().put("piercing", 4);
        d.getEnchantments().put("unbreaking", 3);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.setRarity("EPIC");
        return d;
    }

    private UItemData createTridentTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.TRIDENT);
        d.setDisplayName("<blue><bold>" + formatId(newItemId) + "</bold></blue>");
        d.setLore(List.of(
                "<gray>Đinh ba của thần biển",
                "<dark_gray>──────────────",
                "<blue>🔱 Huyền thoại",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("loyalty", 3);
        d.getEnchantments().put("channeling", 1);
        d.getEnchantments().put("impaling", 5);
        d.getEnchantments().put("unbreaking", 3);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.setRarity("LEGENDARY");

        // Demo skill for trident: on throw (SHOOT trigger for trident/bow/crossbow)
        com.ultra.ucore.skill.Skill throwSkill = new com.ultra.ucore.skill.Skill("ocean_throw");
        throwSkill.setDisplayName("<blue>🌊 Ocean Throw");
        throwSkill.setTrigger(com.ultra.ucore.skill.SkillTrigger.SHOOT);
        throwSkill.setCooldown(0.4);
        throwSkill.setCancelEvent(false);
        com.ultra.ucore.skill.SkillAction soundAction = com.ultra.ucore.skill.SkillAction.playSound("ITEM_TRIDENT_THROW", 0.8f, 1.1f);
        com.ultra.ucore.skill.SkillAction particleAction = new com.ultra.ucore.skill.SkillAction(com.ultra.ucore.skill.SkillAction.ActionType.SPAWN_PARTICLE);
        particleAction.setParam("particle", "CLOUD");
        particleAction.setParam("count", 12);
        particleAction.setParam("offset", 0.4);
        com.ultra.ucore.skill.SkillAction barAction = com.ultra.ucore.skill.SkillAction.actionBar("<blue>🔱 Đinh ba đã lao đi!");
        throwSkill.addAction(soundAction);
        throwSkill.addAction(particleAction);
        throwSkill.addAction(barAction);
        SkillDataHelper.setSkills(d, List.of(throwSkill));
        return d;
    }

    private UItemData createMaceTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.MACE);
        d.setDisplayName("<dark_red><bold>" + formatId(newItemId) + "</bold></dark_red>");
        d.setLore(List.of(
                "<gray>Chùy hủy diệt",
                "<dark_gray>──────────────",
                "<dark_red>🔨 Sức mạnh: <white>Tối thượng",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("density", 5);
        d.getEnchantments().put("breach", 4);
        d.getEnchantments().put("wind_burst", 3);
        d.getEnchantments().put("unbreaking", 3);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.setRarity("LEGENDARY");
        return d;
    }

    private UItemData createHelmetTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_HELMET);
        d.setDisplayName("<aqua><bold>" + formatId(newItemId) + "</bold></aqua>");
        d.setLore(List.of(
                "<gray>Mũ giáp huyền thoại",
                "<dark_gray>──────────────",
                "<aqua>🛡 Giáp: <white>+3",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("protection", 4);
        d.getEnchantments().put("unbreaking", 3);
        d.getEnchantments().put("respiration", 3);
        d.getEnchantments().put("aqua_affinity", 1);
        d.getEnchantments().put("mending", 1);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.getAttributes().add(new UItemData.AttributeData("generic_armor", 3.0, "ADD_NUMBER", "HEAD"));
        d.setRarity("RARE");
        return d;
    }

    private UItemData createChestplateTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_CHESTPLATE);
        d.setDisplayName("<aqua><bold>" + formatId(newItemId) + "</bold></aqua>");
        d.setLore(List.of(
                "<gray>Áo giáp huyền thoại",
                "<dark_gray>──────────────",
                "<aqua>🛡 Giáp: <white>+8",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("protection", 4);
        d.getEnchantments().put("unbreaking", 3);
        d.getEnchantments().put("thorns", 3);
        d.getEnchantments().put("mending", 1);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.getAttributes().add(new UItemData.AttributeData("generic_armor", 8.0, "ADD_NUMBER", "CHEST"));
        d.setRarity("RARE");
        return d;
    }

    private UItemData createLeggingsTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_LEGGINGS);
        d.setDisplayName("<aqua><bold>" + formatId(newItemId) + "</bold></aqua>");
        d.setLore(List.of(
                "<gray>Quần giáp huyền thoại",
                "<dark_gray>──────────────",
                "<aqua>🛡 Giáp: <white>+6",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("protection", 4);
        d.getEnchantments().put("unbreaking", 3);
        d.getEnchantments().put("swift_sneak", 3);
        d.getEnchantments().put("mending", 1);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.getAttributes().add(new UItemData.AttributeData("generic_armor", 6.0, "ADD_NUMBER", "LEGS"));
        d.setRarity("RARE");
        return d;
    }

    private UItemData createBootsTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_BOOTS);
        d.setDisplayName("<aqua><bold>" + formatId(newItemId) + "</bold></aqua>");
        d.setLore(List.of(
                "<gray>Giày giáp huyền thoại",
                "<dark_gray>──────────────",
                "<aqua>🛡 Giáp: <white>+3",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("protection", 4);
        d.getEnchantments().put("feather_falling", 4);
        d.getEnchantments().put("depth_strider", 3);
        d.getEnchantments().put("unbreaking", 3);
        d.getEnchantments().put("mending", 1);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.getAttributes().add(new UItemData.AttributeData("generic_armor", 3.0, "ADD_NUMBER", "FEET"));
        d.setRarity("RARE");
        return d;
    }

    private UItemData createPickaxeTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_PICKAXE);
        d.setDisplayName("<gray><bold>" + formatId(newItemId) + "</bold></gray>");
        d.setLore(List.of(
                "<gray>Cuốc khai thác siêu tốc",
                "<dark_gray>──────────────",
                "<yellow>⛏ Hiệu suất: <white>Tối đa",
                "<dark_gray>──────────────"
        ));
        d.getEnchantments().put("efficiency", 5);
        d.getEnchantments().put("fortune", 3);
        d.getEnchantments().put("unbreaking", 3);
        d.getEnchantments().put("mending", 1);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.setRarity("UNCOMMON");
        return d;
    }

    private UItemData createShovelTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_SHOVEL);
        d.setDisplayName("<yellow><bold>" + formatId(newItemId) + "</bold></yellow>");
        d.setLore(List.of("<gray>Xẻng đào đất siêu tốc"));
        d.getEnchantments().put("efficiency", 5);
        d.getEnchantments().put("fortune", 3);
        d.getEnchantments().put("unbreaking", 3);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.setRarity("UNCOMMON");
        return d;
    }

    private UItemData createHoeTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.DIAMOND_HOE);
        d.setDisplayName("<green><bold>" + formatId(newItemId) + "</bold></green>");
        d.setLore(List.of("<gray>Cuốc nông nghiệp thần thánh"));
        d.getEnchantments().put("efficiency", 5);
        d.getEnchantments().put("fortune", 3);
        d.getEnchantments().put("silk_touch", 1);
        d.getFlags().add("HIDE_ENCHANTS");
        d.setUnbreakable(true);
        d.setRarity("UNCOMMON");
        return d;
    }

    private UItemData createFoodTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.GOLDEN_APPLE);
        d.setDisplayName("<gold><bold>" + formatId(newItemId) + "</bold></gold>");
        d.setLore(List.of(
                "<gray>Thức ăn thần kỳ",
                "<dark_gray>──────────────",
                "<green>❤ Hồi phục: <white>Toàn bộ HP",
                "<yellow>⚡ Hiệu ứng: <white>Strength II (30s)",
                "<dark_gray>──────────────"
        ));
        d.setGlowing(true);
        d.setRarity("EPIC");
        return d;
    }

    private UItemData createTotemTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.TOTEM_OF_UNDYING);
        d.setDisplayName("<light_purple><bold>" + formatId(newItemId) + "</bold></light_purple>");
        d.setLore(List.of(
                "<gray>Bùa hộ mệnh huyền thoại",
                "<dark_gray>──────────────",
                "<light_purple>✨ Bảo vệ: <white>Khỏi cái chết",
                "<dark_gray>──────────────"
        ));
        d.setUnbreakable(true);
        d.setGlowing(true);
        d.setRarity("LEGENDARY");
        return d;
    }

    private UItemData createTeleportTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.ENDER_PEARL);
        d.setDisplayName("<dark_purple><bold>" + formatId(newItemId) + "</bold></dark_purple>");
        d.setLore(List.of(
                "<gray>Bùa dịch chuyển tức thì",
                "<dark_gray>──────────────",
                "<dark_purple>🌀 Cooldown: <white>10 giây",
                "<yellow>Click phải để dịch chuyển",
                "<dark_gray>──────────────"
        ));
        d.setGlowing(true);
        d.setUnbreakable(true);
        d.setRarity("EPIC");

        // Add teleport skill
        com.ultra.ucore.skill.Skill teleportSkill = new com.ultra.ucore.skill.Skill("teleport");
        teleportSkill.setDisplayName("<dark_purple>Dịch Chuyển");
        teleportSkill.setTrigger(com.ultra.ucore.skill.SkillTrigger.RIGHT_CLICK);
        teleportSkill.setCooldown(10.0);
        teleportSkill.setCancelEvent(true);
        com.ultra.ucore.skill.SkillAction soundAction = com.ultra.ucore.skill.SkillAction.playSound("ENTITY_ENDERMAN_TELEPORT", 1.0f, 1.0f);
        com.ultra.ucore.skill.SkillAction teleportAction = new com.ultra.ucore.skill.SkillAction(com.ultra.ucore.skill.SkillAction.ActionType.TELEPORT);
        teleportAction.setParam("y", 0.0);
        teleportAction.setParam("relative", true);
        com.ultra.ucore.skill.SkillAction msgAction = com.ultra.ucore.skill.SkillAction.actionBar("<dark_purple>✨ Đã dịch chuyển!");
        teleportSkill.addAction(soundAction);
        teleportSkill.addAction(msgAction);
        SkillDataHelper.setSkills(d, List.of(teleportSkill));
        return d;
    }

    private UItemData createWandTemplate() {
        UItemData d = new UItemData(newItemId);
        d.setMaterial(Material.BLAZE_ROD);
        d.setDisplayName("<red><bold>" + formatId(newItemId) + "</bold></red>");
        d.setLore(List.of(
                "<gray>Đũa phép lửa",
                "<dark_gray>──────────────",
                "<red>🔥 Cooldown: <white>5 giây",
                "<yellow>Click phải để bắn fireball",
                "<dark_gray>──────────────"
        ));
        d.setGlowing(true);
        d.setUnbreakable(true);
        d.setRarity("EPIC");

        // Add fireball skill
        com.ultra.ucore.skill.Skill fireballSkill = new com.ultra.ucore.skill.Skill("fireball");
        fireballSkill.setDisplayName("<red>Fireball");
        fireballSkill.setTrigger(com.ultra.ucore.skill.SkillTrigger.RIGHT_CLICK);
        fireballSkill.setCooldown(5.0);
        fireballSkill.setCancelEvent(true);
        com.ultra.ucore.skill.SkillAction soundAction = com.ultra.ucore.skill.SkillAction.playSound("ENTITY_BLAZE_SHOOT", 1.0f, 1.0f);
        com.ultra.ucore.skill.SkillAction fireAction = com.ultra.ucore.skill.SkillAction.shootProjectile("SMALL_FIREBALL", 1.6, 7.0);
        com.ultra.ucore.skill.SkillAction msgAction = com.ultra.ucore.skill.SkillAction.actionBar("<red>🔥 Fireball!");
        fireballSkill.addAction(soundAction);
        fireballSkill.addAction(fireAction);
        fireballSkill.addAction(msgAction);
        SkillDataHelper.setSkills(d, List.of(fireballSkill));
        return d;
    }

    /** Format ID thành tên đẹp: "ruby_sword" → "Ruby Sword" */
    private String formatId(String id) {
        String[] parts = id.replace("-", "_").split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
