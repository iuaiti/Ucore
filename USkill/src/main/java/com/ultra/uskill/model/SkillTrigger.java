package com.ultra.uskill.model;

import java.util.Locale;

public enum SkillTrigger {

    RIGHT_CLICK(true, "Right Click", "Activate on right click"),
    LEFT_CLICK(true, "Left Click", "Activate on left click"),
    SHIFT_RIGHT_CLICK(true, "Shift + Right Click", "Activate on shift + right click"),
    SHIFT_LEFT_CLICK(true, "Shift + Left Click", "Activate on shift + left click"),
    DROP(true, "Drop (Q)", "Activate when dropping the item (Q)"),
    SWAP_HAND(true, "Swap Hand (F)", "Activate when swapping hands (F)"),
    THROW(true, "Throw", "Activate when throwing the item"),
    SHOOT(true, "Shoot / Throw", "Activate when shooting with bow/crossbow or throwing trident"),

    HIT(false, "On Hit", "Activate when hitting a target"),
    TAKE_DAMAGE(false, "On Damaged", "Activate when taking damage"),
    KILL(false, "On Kill", "Activate when killing with this item"),

    EQUIP(false, "On Equip", "Activate when equipping or holding"),
    UNEQUIP(false, "On Unequip", "Activate when unequipping"),

    PERIODIC(false, "Periodic", "Repeats every period (for regen, aura...)"),
    WHILE_HOLDING(false, "While Holding", "Repeats while holding in hand"),
    WHILE_WEARING(false, "While Wearing", "Repeats while wearing as armor");

    private final boolean active;
    private final String displayName;
    private final String description;

    SkillTrigger(boolean active, String displayName, String description) {
        this.active = active;
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static SkillTrigger fromString(String s) {
        if (s == null) return RIGHT_CLICK;
        try {
            return SkillTrigger.valueOf(s.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return switch (s.toUpperCase(Locale.ROOT)) {
                case "RIGHT", "RC", "RIGHTCLICK", "RIGHT_CLICK" -> RIGHT_CLICK;
                case "LEFT", "LC", "LEFTCLICK", "LEFT_CLICK" -> LEFT_CLICK;
                case "SHIFT_RIGHT", "SRIGHT", "SHIFTRIGHT" -> SHIFT_RIGHT_CLICK;
                case "SHIFT_LEFT", "SLEFT", "SHIFTLEFT" -> SHIFT_LEFT_CLICK;
                case "Q", "DROP_ITEM" -> DROP;
                case "F", "SWAP" -> SWAP_HAND;
                case "THROW", "TOSS", "DROP_ITEMS" -> THROW;
                case "SHOOT", "LAUNCH", "BOW", "BOW_SHOOT", "TRIDENT", "TRIDENT_THROW", "FIRE", "PROJECTILE" -> SHOOT;
                case "ON_HIT", "ATTACK", "DAMAGE_DEALT" -> HIT;
                case "ON_DAMAGE", "HURT", "DAMAGED" -> TAKE_DAMAGE;
                case "ON_EQUIP", "EQUIPPED", "WEAR" -> EQUIP;
                case "ON_UNEQUIP", "UNEQUIPPED" -> UNEQUIP;
                case "TICK", "REPEAT", "AURA" -> PERIODIC;
                default -> RIGHT_CLICK;
            };
        }
    }
}
