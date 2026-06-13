package com.ultra.ucore.skill;

import java.util.Locale;

public enum SkillTrigger {

    //                       active  displayName (EN)          description (EN)                                        displayName (VI)              description (VI)
    RIGHT_CLICK       (true,  "Right Click",              "Activate on right click",                              "Nhấp Chuột Phải",            "Kích hoạt khi nhấp chuột phải"),
    LEFT_CLICK        (true,  "Left Click",               "Activate on left click",                               "Nhấp Chuột Trái",            "Kích hoạt khi nhấp chuột trái"),
    SHIFT_RIGHT_CLICK (true,  "Shift + Right Click",      "Activate on shift + right click",                      "Shift + Chuột Phải",         "Kích hoạt khi giữ Shift + nhấp chuột phải"),
    SHIFT_LEFT_CLICK  (true,  "Shift + Left Click",       "Activate on shift + left click",                       "Shift + Chuột Trái",         "Kích hoạt khi giữ Shift + nhấp chuột trái"),
    DROP              (true,  "Drop (Q)",                 "Activate when dropping the item (Q)",                  "Vứt Vật Phẩm (Q)",           "Kích hoạt khi vứt vật phẩm xuống đất (Q)"),
    SWAP_HAND         (true,  "Swap Hand (F)",            "Activate when swapping hands (F)",                     "Đổi Tay (F)",                "Kích hoạt khi đổi tay cầm (F)"),
    THROW             (true,  "Throw",                    "Activate when throwing the item",                      "Ném",                        "Kích hoạt khi ném vật phẩm"),
    SHOOT             (true,  "Shoot",                    "Activate when shooting bow/crossbow or throwing trident", "Bắn",                     "Kích hoạt khi bắn cung/nỏ hoặc ném đinh ba"),
    BREAK_BLOCK       (true,  "Break Block",              "Activate when breaking a block while holding this item","Phá Khối",                   "Kích hoạt khi phá khối trong khi cầm vật phẩm"),

    HIT               (false, "On Hit",                   "Activate when hitting a target",                       "Khi Đánh Trúng",             "Kích hoạt khi đánh trúng mục tiêu"),
    TAKE_DAMAGE       (false, "On Damaged",               "Activate when taking damage",                          "Khi Nhận Sát Thương",        "Kích hoạt khi bị nhận sát thương"),
    KILL              (false, "On Kill",                  "Activate when killing with this item",                 "Khi Tiêu Diệt",              "Kích hoạt khi tiêu diệt mục tiêu bằng vật phẩm này"),

    EQUIP             (false, "On Equip",                 "Activate when equipping or holding",                   "Khi Trang Bị",               "Kích hoạt khi mặc hoặc cầm vật phẩm"),
    UNEQUIP           (false, "On Unequip",               "Activate when unequipping",                            "Khi Tháo Ra",                "Kích hoạt khi tháo vật phẩm ra"),

    PERIODIC          (false, "Periodic",                 "Repeats every period (for regen, aura...)",            "Định Kỳ",                    "Lặp lại theo chu kỳ (hồi máu, hào quang...)"),
    WHILE_HOLDING     (false, "While Holding",            "Repeats while holding in hand",                        "Trong Khi Cầm",              "Lặp lại trong khi đang cầm trên tay"),
    WHILE_WEARING     (false, "While Wearing",            "Repeats while wearing as armor",                       "Trong Khi Mặc",              "Lặp lại trong khi đang mặc như giáp");

    private final boolean active;
    private final String displayName;
    private final String description;
    private final String displayNameVi;
    private final String descriptionVi;

    SkillTrigger(boolean active,
                 String displayName, String description,
                 String displayNameVi, String descriptionVi) {
        this.active = active;
        this.displayName = displayName;
        this.description = description;
        this.displayNameVi = displayNameVi;
        this.descriptionVi = descriptionVi;
    }

    public boolean isActive() { return active; }

    public String getDisplayName()     { return displayName; }
    public String getDescription()     { return description; }
    public String getDisplayNameVi()   { return displayNameVi; }
    public String getDescriptionVi()   { return descriptionVi; }

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
                case "MINE", "BLOCK_BREAK", "MINING" -> BREAK_BLOCK;
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
