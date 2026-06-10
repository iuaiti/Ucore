package com.ultra.uskill.tree;

import java.util.LinkedHashMap;
import java.util.Map;

public enum StatType {
    DAMAGE("Damage", "Increases damage dealt"),
    DEFENSE("Defense", "Reduces damage taken"),
    MAX_HEALTH("Max Health", "Increases max health"),
    MOVEMENT_SPEED("Movement Speed", "Increases movement speed"),
    ATTACK_SPEED("Attack Speed", "Increases attack speed"),
    CRIT_CHANCE("Crit Chance", "Chance for critical hit"),
    CRIT_DAMAGE("Crit Damage", "Critical damage multiplier"),
    LUCK("Luck", "Increases luck"),
    REGENERATION("Regeneration", "Health regen per second"),
    MANA("Mana", "Increases mana pool"),
    MANA_REGEN("Mana Regen", "Mana per second"),
    JUMP_BOOST("Jump Boost", "Increases jump height"),
    FIRE_RESISTANCE("Fire Resistance", "Reduces fire damage"),
    ABSORPTION("Absorption", "Extra absorption hearts"),
    LIFE_STEAL("Life Steal", "% damage healed"),
    THORNS("Thorns", "% damage reflected");

    private final String displayName;
    private final String description;

    StatType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public static StatType fromString(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Map<String, Object> serialize(Map<StatType, Double> stats) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<StatType, Double> e : stats.entrySet()) {
            map.put(e.getKey().name(), e.getValue());
        }
        return map;
    }

    public static Map<StatType, Double> deserialize(Map<?, ?> raw) {
        Map<StatType, Double> map = new LinkedHashMap<>();
        if (raw == null) return map;
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            StatType type = fromString(String.valueOf(e.getKey()));
            if (type != null) {
                double val = e.getValue() instanceof Number n ? n.doubleValue() : 0;
                map.put(type, val);
            }
        }
        return map;
    }
}
