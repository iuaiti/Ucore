package com.uitem;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

/**
 * Data class đại diện cho một custom item trong UItem.
 * Chứa toàn bộ thông tin cần thiết để build một ItemStack.
 */
public class UItemData {

    private final String id;
    private Material material;
    private String displayName; // MiniMessage format
    private List<String> lore; // Mỗi dòng là MiniMessage string
    private Map<String, Integer> enchantments; // key = enchantment key (e.g., "sharpness"), value = level
    private Set<String> flags; // ItemFlag names
    private int customModelData; // -1 = không set
    private boolean unbreakable;
    private boolean glowing;
    private int maxDurability; // -1 = use default material durability
    private List<AttributeData> attributes;

    // Oraxen/ItemsAdder/ExecutableItems integration
    private String rarity;

    private String oraxenId;
    private String itemsAdderId;
    private String executableItemsId;

    // General attachable effects (reflect, extra damage on hit/damage/equip/fullset/die)
    private List<ItemEffect> effects = new ArrayList<>();

    // Raw skill data (parsed by USkillAPI when needed — keeps UItem decoupled)
    private Map<String, Object> skillsRaw = new LinkedHashMap<>();

    // Optional custom model reference for resource pack (Oraxen/ItemsAdder style)
    // Can be just name "my_model" -> looks for models/my_model.json and uses uitem:item/my_model
    // Or full "uitem:item/some_path" or even different namespace.
    private String model;

    // Recipe/Item categories for classification (VU_KHI, GIAP, CONG_CU, etc.)
    private Set<String> categories = new HashSet<>();

    // Skill tree requirement — player must have this node at required level to craft/use
    private String requiredSkillNode;
    private int requiredSkillLevel = 1;

    public UItemData(String id) {
        this.id = id;
        this.material = Material.STONE;
        this.displayName = "<white>" + id;
        this.lore = new ArrayList<>();
        this.enchantments = new LinkedHashMap<>();
        this.flags = new LinkedHashSet<>();
        this.customModelData = -1;
        this.unbreakable = false;
        this.glowing = false;
        this.maxDurability = -1;
        this.attributes = new ArrayList<>();
        this.rarity = null;
        this.oraxenId = null;
        this.itemsAdderId = null;
        this.executableItemsId = null;
        this.effects = new ArrayList<>();
        this.skillsRaw = new LinkedHashMap<>();
        this.model = null;
        this.categories = new HashSet<>();
    }

    // === Getters & Setters ===

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public void setFlags(Set<String> flags) {
        this.flags = flags;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public void setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
    }

    public List<AttributeData> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeData> attributes) {
        this.attributes = attributes;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getOraxenId() {
        return oraxenId;
    }

    public void setOraxenId(String oraxenId) {
        this.oraxenId = oraxenId;
    }

    public String getItemsAdderId() {
        return itemsAdderId;
    }

    public void setItemsAdderId(String itemsAdderId) {
        this.itemsAdderId = itemsAdderId;
    }

    public String getExecutableItemsId() {
        return executableItemsId;
    }

    public void setExecutableItemsId(String executableItemsId) {
        this.executableItemsId = executableItemsId;
    }

    public List<ItemEffect> getEffects() {
        return effects;
    }

    public void setEffects(List<ItemEffect> effects) {
        this.effects = (effects != null) ? effects : new ArrayList<>();
    }

    public Map<String, Object> getSkillsRaw() {
        return skillsRaw;
    }

    public void setSkillsRaw(Map<String, Object> skillsRaw) {
        this.skillsRaw = (skillsRaw != null) ? skillsRaw : new LinkedHashMap<>();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = (model != null && !model.isBlank()) ? model : null;
    }

    public Set<String> getCategories() { return categories; }
    public void setCategories(Set<String> categories) { this.categories = categories != null ? new HashSet<>(categories) : new HashSet<>(); }
    public void addCategory(String category) { categories.add(category); }
    public void removeCategory(String category) { categories.remove(category); }
    public boolean hasCategory(String category) { return categories.contains(category); }
    public void toggleCategory(String category) {
        if (categories.contains(category)) categories.remove(category);
        else categories.add(category);
    }

    public String getRequiredSkillNode() { return requiredSkillNode; }
    public void setRequiredSkillNode(String node) { this.requiredSkillNode = (node != null && !node.isBlank()) ? node : null; }
    public int getRequiredSkillLevel() { return requiredSkillLevel; }
    public void setRequiredSkillLevel(int level) { this.requiredSkillLevel = Math.max(1, level); }
    public boolean hasSkillRequirement() { return requiredSkillNode != null; }

    /**
     * General effect that can be attached to a custom item.
     * Supports reflect damage, extra damage on various triggers.
     * Configurable in items.yml under "effects:".
     */
    public static class ItemEffect {
        private String trigger;     // HIT, DAMAGE, DIE, EQUIP, FULL_SET (mặc = EQUIP)
        private String type;        // REFLECT, EXTRA_DAMAGE
        private double chance = 100.0;
        private double value = 0.0; // percent for REFLECT, amount for EXTRA_DAMAGE
        private boolean applyToTarget = false; // for HIT: true = apply effect to the victim (target), false = self (holder)

        public ItemEffect() {}

        public String getTrigger() { return trigger; }
        public void setTrigger(String trigger) { this.trigger = trigger != null ? trigger.toUpperCase(Locale.ROOT) : null; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type != null ? type.toUpperCase(Locale.ROOT) : null; }

        public double getChance() { return chance; }
        public void setChance(double chance) { this.chance = Math.max(0, Math.min(100, chance)); }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public boolean isApplyToTarget() { return applyToTarget; }
        public void setApplyToTarget(boolean applyToTarget) { this.applyToTarget = applyToTarget; }

        public ItemEffect copy() {
            ItemEffect c = new ItemEffect();
            c.trigger = this.trigger;
            c.type = this.type;
            c.chance = this.chance;
            c.value = this.value;
            c.applyToTarget = this.applyToTarget;
            return c;
        }
    }

    /**
     * Deep clone UItemData.
     */
    public UItemData clone() {
        UItemData clone = new UItemData(this.id);
        clone.material = this.material;
        clone.displayName = this.displayName;
        clone.lore = this.lore != null ? new ArrayList<>(this.lore) : new ArrayList<>();
        clone.enchantments = this.enchantments != null ? new LinkedHashMap<>(this.enchantments) : new LinkedHashMap<>();
        clone.flags = this.flags != null ? new LinkedHashSet<>(this.flags) : new LinkedHashSet<>();
        clone.customModelData = this.customModelData;
        clone.unbreakable = this.unbreakable;
        clone.glowing = this.glowing;
        clone.maxDurability = this.maxDurability;
        clone.attributes = new ArrayList<>();
        for (AttributeData attribute : this.attributes) {
            clone.attributes.add(attribute.copy());
        }
        clone.rarity = this.rarity;
        clone.oraxenId = this.oraxenId;
        clone.itemsAdderId = this.itemsAdderId;
        clone.executableItemsId = this.executableItemsId;
        clone.effects = new ArrayList<>();
        for (ItemEffect e : this.effects) {
            clone.effects.add(e.copy());
        }
        clone.skillsRaw = new LinkedHashMap<>(this.skillsRaw);
        clone.model = this.model;
        clone.categories = new HashSet<>(this.categories);
        clone.requiredSkillNode = this.requiredSkillNode;
        clone.requiredSkillLevel = this.requiredSkillLevel;
        return clone;
    }

    /**
     * Data class cho attribute modifier.
     */
    public static class AttributeData {
        private String attribute; // e.g., "GENERIC_ATTACK_DAMAGE"
        private double amount;
        private String operation; // "ADD_NUMBER", "ADD_SCALAR", "MULTIPLY_SCALAR_1"
        private String slot; // "HAND", "OFF_HAND", "HEAD", "CHEST", "LEGS", "FEET", "ANY"

        public AttributeData(String attribute, double amount, String operation, String slot) {
            this.attribute = attribute;
            this.amount = amount;
            this.operation = operation;
            this.slot = slot;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot) {
            this.slot = slot;
        }

        public AttributeData copy() {
            return new AttributeData(attribute, amount, operation, slot);
        }
    }
}
