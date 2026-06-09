package com.uitem.legacy.skill;

import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.Locale;

/**
 * Helper class cho potion-related skill actions.
 * Cung cấp utility methods và preset combos.
 */
public class PotionEffectHelper {

    /**
     * Các preset combo phổ biến để thêm nhanh
     */
    public static class PotionPreset {
        public final String name;
        public final List<PotionEffectCombo> effects;

        public PotionPreset(String name, List<PotionEffectCombo> effects) {
            this.name = name;
            this.effects = effects;
        }
    }

    public static class PotionEffectCombo {
        public final String effectType;    // e.g., "STRENGTH"
        public final int durationSeconds;
        public final int amplifier;        // 0-based (0 = Level I, 1 = Level II)

        public PotionEffectCombo(String effectType, int durationSeconds, int amplifier) {
            this.effectType = effectType;
            this.durationSeconds = durationSeconds;
            this.amplifier = amplifier;
        }
    }

    // Preset combo collections
    private static final Map<String, PotionPreset> PRESETS = new LinkedHashMap<>();

    static {
        // Buff presets (useful for RPG items)
        registerPreset("Warrior Buff", List.of(
            new PotionEffectCombo("STRENGTH", 30, 1),      // Strength II for 30s
            new PotionEffectCombo("RESISTANCE", 30, 0)     // Resistance I for 30s
        ));

        registerPreset("Mage Buff", List.of(
            new PotionEffectCombo("SPEED", 30, 1),         // Speed II for 30s
            new PotionEffectCombo("HASTE", 30, 0)          // Haste I for 30s
        ));

        registerPreset("Healer Buff", List.of(
            new PotionEffectCombo("REGENERATION", 30, 1),  // Regeneration II for 30s
            new PotionEffectCombo("ABSORPTION", 30, 0)     // Absorption I for 30s
        ));

        registerPreset("Speed Boost", List.of(
            new PotionEffectCombo("SPEED", 20, 2)          // Speed III for 20s
        ));

        registerPreset("Night Vision", List.of(
            new PotionEffectCombo("NIGHT_VISION", 60, 0)   // Night Vision for 60s (3 min)
        ));

        registerPreset("Defensive Aura", List.of(
            new PotionEffectCombo("RESISTANCE", 15, 2),    // Resistance III for 15s
            new PotionEffectCombo("FIRE_RESISTANCE", 15, 0) // Fire Resistance for 15s
        ));

        registerPreset("Damage Boost", List.of(
            new PotionEffectCombo("STRENGTH", 15, 2)       // Strength III for 15s
        ));

        registerPreset("Healing Potion", List.of(
            new PotionEffectCombo("INSTANT_HEALTH", 1, 2)  // Instant Health III (immediate)
        ));

        registerPreset("Debuff: Weakness", List.of(
            new PotionEffectCombo("WEAKNESS", 20, 0)       // Weakness for 20s
        ));

        registerPreset("Debuff: Poison", List.of(
            new PotionEffectCombo("POISON", 10, 1)         // Poison II for 10s
        ));
    }

    private static void registerPreset(String name, List<PotionEffectCombo> effects) {
        PRESETS.put(name, new PotionPreset(name, effects));
    }

    /**
     * Lấy tất cả preset combos
     */
    public static Map<String, PotionPreset> getAllPresets() {
        return new LinkedHashMap<>(PRESETS);
    }

    /**
     * Lấy preset theo tên
     */
    public static PotionPreset getPreset(String name) {
        return PRESETS.get(name);
    }

    /**
     * Lấy preset bằng index
     */
    public static PotionPreset getPresetByIndex(int index) {
        List<PotionPreset> list = new ArrayList<>(PRESETS.values());
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    /**
     * Tổng số presets
     */
    public static int getTotalPresets() {
        return PRESETS.size();
    }

    /**
     * Kiểm tra potion effect có hợp lệ không
     */
    public static boolean isValidEffect(String effectName) {
        try {
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effectName.toLowerCase(Locale.ROOT)));
            return type != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tạo SkillAction từ PotionEffectCombo
     */
    public static SkillAction createPotionAction(PotionEffectCombo combo) {
        return SkillAction.givePotion(combo.effectType, combo.durationSeconds, combo.amplifier);
    }

    /**
     * Add toàn bộ preset combo vào skill
     */
    public static void addPresetToSkill(Skill skill, PotionPreset preset) {
        for (PotionEffectCombo combo : preset.effects) {
            skill.addAction(createPotionAction(combo));
        }
    }
}
