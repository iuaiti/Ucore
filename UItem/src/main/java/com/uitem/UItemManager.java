package com.uitem;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.placeholder.PlaceholderManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.uitem.rarity.RarityConfig;

/**
 * Quản lý toàn bộ custom items.
 * Cung cấp CRUD operations và build ItemStack từ UItemData.
 */
public class UItemManager {

    private final UItemPlugin plugin;
    private final Map<String, UItemData> items;
    private final NamespacedKey uitemKey;

    public UItemManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.items = new ConcurrentHashMap<>();
        this.uitemKey = new NamespacedKey(plugin, "uitem_id");
    }

    /**
     * Lấy NamespacedKey dùng để đánh dấu item.
     */
    public NamespacedKey getUItemKey() {
        return uitemKey;
    }

    /**
     * Thêm hoặc cập nhật một custom item.
     */
    public void registerItem(UItemData data) {
        if (data == null || data.getId() == null || data.getId().isBlank()) {
            return;
        }
        items.put(data.getId().toLowerCase(Locale.ROOT), data);
    }

    /**
     * Xóa một custom item.
     */
    public void removeItem(String id) {
        if (id == null) {
            return;
        }
        items.remove(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Lấy UItemData theo ID.
     */
    public UItemData getItem(String id) {
        if (id == null) {
            return null;
        }
        return items.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Lấy tất cả items.
     */
    public Map<String, UItemData> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Xóa tất cả items (dùng khi reload).
     */
    public void clearAll() {
        items.clear();
    }


    /**
     * Tạo snapshot deep-copy để lưu async an toàn.
     */
    public Map<String, UItemData> snapshotAll() {
        Map<String, UItemData> snapshot = new LinkedHashMap<>();
        List<String> sortedKeys = new ArrayList<>(items.keySet());
        sortedKeys.sort(String::compareToIgnoreCase);
        for (String key : sortedKeys) {
            UItemData data = items.get(key);
            if (data != null) {
                snapshot.put(key, data.clone());
            }
        }
        return snapshot;
    }

    /**
     * Kiểm tra item tồn tại.
     */
    public boolean hasItem(String id) {
        return id != null && items.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Build ItemStack từ UItemData.
     * Đây là method quan trọng nhất — chuyển đổi data thành item thực tế.
     */
    public ItemStack buildItemStack(UItemData data) {
        if (data == null) return new ItemStack(Material.STONE);

        // Build base (integration or vanilla)
        ItemStack integrationBase = plugin.getIntegrationManager().tryBuildFromIntegration(data);
        ItemStack itemStack;
        if (integrationBase != null) {
            itemStack = integrationBase;
        } else {
            Material material = data.getMaterial() != null && data.getMaterial().isItem() ? data.getMaterial() : Material.STONE;
            itemStack = new ItemStack(material);
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        // Always apply/override display name + lore from UItemData (for both vanilla and integration base)
        String displayName = data.getDisplayName();
        String rarity = data.getRarity();
        if (displayName != null && !displayName.isEmpty()) {
            if (rarity != null) {
                String prefix = RarityConfig.getRarityPrefix(rarity);
                if (prefix != null && !prefix.isEmpty()) {
                    displayName = prefix + " " + displayName;
                }
            }
            meta.displayName(safeDeserialize(displayName, data.getId()));
        }

        List<Component> loreComponents = new ArrayList<>();
        if (data.getLore() != null) {
            for (String line : data.getLore()) {
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }
        if (rarity != null) {
            List<String> rarityLore = RarityConfig.getRarityLore(rarity);
            for (String line : rarityLore) {
                loreComponents.add(safeDeserialize(line, data.getId()));
            }
        }
        if (!loreComponents.isEmpty()) {
            meta.lore(loreComponents);
        }

        // Enchantments
        if (data.getEnchantments() != null) {
            for (Map.Entry<String, Integer> entry : data.getEnchantments().entrySet()) {
                Enchantment ench = getEnchantmentByKey(entry.getKey());
                if (ench != null) {
                    meta.addEnchant(ench, entry.getValue(), true);
                }
            }
        }

        // Flags
        if (data.getFlags() != null) {
            for (String flagName : data.getFlags()) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Custom Model Data
        if (data.getCustomModelData() > 0) {
            meta.setCustomModelData(data.getCustomModelData());
        }

        // Unbreakable
        meta.setUnbreakable(data.isUnbreakable());

        // Glowing (enchant glow effect mà không hiện enchant text)
        if (data.isGlowing() && (data.getEnchantments() == null || data.getEnchantments().isEmpty())) {
            meta.setEnchantmentGlintOverride(true);
        }

        // Durability
        if (meta instanceof Damageable damageable) {
            int maxDur = data.getMaxDurability();
            if (maxDur > 0) {
                damageable.setMaxDamage(maxDur);
                damageable.setDamage(0);
            } else if (maxDur == 0) {
                damageable.setMaxDamage(0);
                damageable.setDamage(0);
            }
        }

        // Attributes
        if (data.getAttributes() != null) {
            for (UItemData.AttributeData attrData : data.getAttributes()) {
                try {
                    Attribute attribute = getAttributeByKey(attrData.getAttribute());
                    if (attribute == null) continue;
                    AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(attrData.getOperation());
                    EquipmentSlotGroup slotGroup = parseEquipmentSlotGroup(attrData.getSlot());

                    NamespacedKey attrKey = new NamespacedKey(plugin, data.getId() + "_" + attrData.getAttribute().toLowerCase() + "_" + attrData.getOperation() + "_" + attrData.getSlot());
                    AttributeModifier modifier = new AttributeModifier(
                            attrKey,
                            attrData.getAmount(),
                            operation,
                            slotGroup
                    );
                    meta.addAttributeModifier(attribute, modifier);
                } catch (Exception e) {
                    plugin.getLogger().warning("Không thể thêm attribute " + attrData.getAttribute()
                            + " cho item " + data.getId() + ": " + e.getMessage());
                }
            }
        }

        // Tag item với PersistentDataContainer
        meta.getPersistentDataContainer().set(uitemKey, PersistentDataType.STRING, data.getId());

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Build ItemStack với UCore placeholder support (player-aware).
     * Áp dụng %placeholder% từ UCore's PlaceholderManager vào display name và lore.
     */
    public ItemStack buildItemStack(UItemData data, Player player) {
        ItemStack item = buildItemStack(data);
        if (player == null) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PlaceholderManager pm = UCoreAPI.getPlaceholderManager();
        var ctx = new PlaceholderManager.PlaceholderContext(player, player);

        if (meta.hasDisplayName()) {
            String rawName = data.getDisplayName() != null ? data.getDisplayName() : "";
            if (!rawName.isEmpty()) {
                String rarity = data.getRarity();
                if (rarity != null) {
                    String prefix = RarityConfig.getRarityPrefix(rarity);
                    if (prefix != null && !prefix.isEmpty()) {
                        rawName = prefix + " " + rawName;
                    }
                }
                String parsed = pm.apply(rawName, ctx);
                meta.displayName(safeDeserialize(parsed, data.getId()));
            }
        }

        if (meta.hasLore()) {
            List<Component> newLore = new ArrayList<>();
            if (data.getLore() != null) {
                for (String line : data.getLore()) {
                    String parsed = pm.apply(line, ctx);
                    newLore.add(safeDeserialize(parsed, data.getId()));
                }
            }
            String rarity = data.getRarity();
            if (rarity != null) {
                List<String> rarityLore = RarityConfig.getRarityLore(rarity);
                for (String line : rarityLore) {
                    String parsed = pm.apply(line, ctx);
                    newLore.add(safeDeserialize(parsed, data.getId()));
                }
            }
            meta.lore(newLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Build ItemStack cho preview trong GUI (có thêm thông tin phụ).
     * Mặc định hiển thị ID và thông tin nội bộ (dành cho admin/editor).
     */
    public ItemStack buildPreviewItemStack(UItemData data) {
        return buildPreviewItemStack(data, true);
    }

    /**
     * Build ItemStack cho preview trong GUI.
     *
     * @param data    dữ liệu item
     * @param showId  nếu true thì thêm ID, Material, CMD và hướng dẫn click (dành cho admin).
     *                Nếu false thì chỉ giữ item gốc + lore người chơi (ẩn ID cho player thường).
     */
    public ItemStack buildPreviewItemStack(UItemData data, boolean showId) {
        ItemStack item = buildItemStack(data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();

            if (showId) {
                lore.add(Component.empty());
                lore.add(com.uitem.util.TextUtil.parse("<dark_gray>──────────────"));
                lore.add(com.uitem.util.TextUtil.parse("<gray>ID: <white>" + data.getId()));
                Material previewMaterial = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
                lore.add(com.uitem.util.TextUtil.parse("<gray>Material: <white>" + previewMaterial.name()));
                lore.add(com.uitem.util.TextUtil.parse("<gray>CMD: <white>" + (data.getCustomModelData() > 0 ? data.getCustomModelData() : "Không")));
                lore.add(com.uitem.util.TextUtil.parse("<dark_gray>──────────────"));
                lore.add(com.uitem.util.TextUtil.parse("<yellow>Click trái: <white>Chỉnh sửa"));
                lore.add(com.uitem.util.TextUtil.parse("<red>Click phải: <white>Xóa"));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // === Helper methods ===

    /**
     * Safe MiniMessage deserialize — fallback to plain text on parse error.
     * Prevents crashes from user-input MiniMessage syntax errors.
     */
    private Component safeDeserialize(String miniMessageStr, String itemId) {
        try {
            return com.uitem.util.TextUtil.parse(miniMessageStr);
        } catch (Exception e) {
            plugin.getLogger().warning(
                "[UItem] Invalid MiniMessage in item '" + itemId + "': " + miniMessageStr
                + " — falling back to plain text. Error: " + e.getMessage());
            return Component.text(miniMessageStr);
        }
    }

    // (removed applyDisplayProperties and tagItem - logic inlined in buildItemStack for both vanilla + integration cases,
    // so enchants/flags/attributes/etc from UItemData now apply on top of IA/Oraxen/EI base items too.
    // This fixes "fake" high-level enchants not being real on external model items.)

    /**
     * Lấy enchantment từ key string (e.g., "sharpness", "minecraft:sharpness").
     */
    public Enchantment getEnchantmentByKey(String key) {
        if (key == null) return null;
        String cleanKey = key.toLowerCase(Locale.ROOT).replace(" ", "_");
        if (!cleanKey.contains(":")) {
            cleanKey = "minecraft:" + cleanKey;
        }
        try {
            NamespacedKey namespacedKey = NamespacedKey.fromString(cleanKey);
            if (namespacedKey == null) return null;
            @SuppressWarnings("deprecation")
            Registry<Enchantment> registry = Registry.ENCHANTMENT;
            return registry.get(namespacedKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lấy attribute từ key string.
     */
    private Attribute getAttributeByKey(String key) {
        if (key == null) return null;
        String cleanKey = key.toLowerCase(Locale.ROOT).replace(" ", "_");
        if (!cleanKey.contains(":")) {
            cleanKey = "minecraft:" + cleanKey;
        }
        try {
            NamespacedKey namespacedKey = NamespacedKey.fromString(cleanKey);
            if (namespacedKey == null) return null;
            Registry<Attribute> registry = Registry.ATTRIBUTE;
            return registry.get(namespacedKey);
        } catch (Exception e) {
            return null;
        }
    }

    private EquipmentSlotGroup parseEquipmentSlotGroup(String slot) {
        if (slot == null) return EquipmentSlotGroup.ANY;
        return switch (slot.toUpperCase(Locale.ROOT)) {
            case "HAND", "MAINHAND", "MAIN_HAND" -> EquipmentSlotGroup.MAINHAND;
            case "OFF_HAND", "OFFHAND" -> EquipmentSlotGroup.OFFHAND;
            case "HEAD", "HELMET" -> EquipmentSlotGroup.HEAD;
            case "CHEST", "CHESTPLATE" -> EquipmentSlotGroup.CHEST;
            case "LEGS", "LEGGINGS" -> EquipmentSlotGroup.LEGS;
            case "FEET", "BOOTS" -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ANY;
        };
    }

    /**
     * Lấy danh sách tất cả enchantment phổ biến cho GUI.
     */
    public List<Enchantment> getCommonEnchantments() {
        List<Enchantment> list = new ArrayList<>();
        String[] keys = {
                "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect",
                "looting", "sweeping_edge", "efficiency", "silk_touch", "fortune",
                "unbreaking", "mending", "power", "punch", "flame", "infinity",
                "luck_of_the_sea", "lure", "protection", "fire_protection",
                "blast_protection", "projectile_protection", "feather_falling",
                "respiration", "aqua_affinity", "thorns", "depth_strider",
                "frost_walker", "soul_speed", "swift_sneak",
                "channeling", "loyalty", "riptide", "impaling",
                "multishot", "quick_charge", "piercing",
                "wind_burst", "breach", "density"
        };
        for (String key : keys) {
            Enchantment ench = getEnchantmentByKey(key);
            if (ench != null) {
                list.add(ench);
            }
        }
        return list;
    }

    /**
     * Lấy tên hiển thị dễ đọc cho enchantment.
     */
    public String getEnchantmentDisplayName(Enchantment ench) {
        String key = ench.getKey().getKey();
        return switch (key) {
            case "sharpness" -> "Sharpness";
            case "smite" -> "Smite";
            case "bane_of_arthropods" -> "Bane of Arthropods";
            case "knockback" -> "Knockback";
            case "fire_aspect" -> "Fire Aspect";
            case "looting" -> "Looting";
            case "sweeping_edge" -> "Sweeping Edge";
            case "efficiency" -> "Efficiency";
            case "silk_touch" -> "Silk Touch";
            case "fortune" -> "Fortune";
            case "unbreaking" -> "Unbreaking";
            case "mending" -> "Mending";
            case "power" -> "Power";
            case "punch" -> "Punch";
            case "flame" -> "Flame";
            case "infinity" -> "Infinity";
            case "luck_of_the_sea" -> "Luck of the Sea";
            case "lure" -> "Lure";
            case "protection" -> "Protection";
            case "fire_protection" -> "Fire Protection";
            case "blast_protection" -> "Blast Protection";
            case "projectile_protection" -> "Projectile Protection";
            case "feather_falling" -> "Feather Falling";
            case "respiration" -> "Respiration";
            case "aqua_affinity" -> "Aqua Affinity";
            case "thorns" -> "Thorns";
            case "depth_strider" -> "Depth Strider";
            case "frost_walker" -> "Frost Walker";
            case "soul_speed" -> "Soul Speed";
            case "swift_sneak" -> "Swift Sneak";
            case "channeling" -> "Channeling";
            case "loyalty" -> "Loyalty";
            case "riptide" -> "Riptide";
            case "impaling" -> "Impaling";
            case "multishot" -> "Multishot";
            case "quick_charge" -> "Quick Charge";
            case "piercing" -> "Piercing";
            case "wind_burst" -> "Wind Burst";
            case "breach" -> "Breach";
            case "density" -> "Density";
            default -> key.replace("_", " ");
        };
    }
}
