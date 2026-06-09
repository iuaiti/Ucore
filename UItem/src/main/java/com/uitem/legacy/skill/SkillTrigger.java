package com.uitem.legacy.skill;

import com.uitem.util.Messages;

import java.util.Locale;

/**
 * Trigger cho skill/passive của UItem.
 * Hỗ trợ song ngữ EN/VI qua Messages.
 */
public enum SkillTrigger {

    // ==================== ACTIVE ====================
    RIGHT_CLICK(true),
    LEFT_CLICK(true),
    SHIFT_RIGHT_CLICK(true),
    SHIFT_LEFT_CLICK(true),
    DROP(true),
    SWAP_HAND(true),
    THROW(true),
    // Shooting / launching projectile with a weapon (bow, crossbow, trident, etc.)
    // Use this for skills that should activate when firing the custom bow/trident item.
    SHOOT(true),

    // ==================== COMBAT PASSIVE ====================
    HIT(false),
    TAKE_DAMAGE(false),
    KILL(false),

    // ==================== EQUIP PASSIVE ====================
    EQUIP(false),
    UNEQUIP(false),

    // ==================== PERIODIC ====================
    PERIODIC(false),
    WHILE_HOLDING(false),
    WHILE_WEARING(false);

    private final boolean active;

    SkillTrigger(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public String getDisplayName() {
        return Messages.skillTriggerDisplayName(name());
    }

    public String getDescription() {
        return Messages.skillTriggerDescription(name());
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
