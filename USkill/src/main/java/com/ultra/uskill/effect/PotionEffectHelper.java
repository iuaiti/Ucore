package com.ultra.uskill.effect;

import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillAction;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PotionEffectHelper {

    public static class PotionPreset {
        public final String name;
        public final List<PotionEffectCombo> effects;

        public PotionPreset(String name, List<PotionEffectCombo> effects) {
            this.name = name;
            this.effects = effects;
        }
    }

    public static class PotionEffectCombo {
        public final String effectType;
        public final int durationSeconds;
        public final int amplifier;

        public PotionEffectCombo(String effectType, int durationSeconds, int amplifier) {
            this.effectType = effectType;
            this.durationSeconds = durationSeconds;
            this.amplifier = amplifier;
        }
    }

    private static final Map<String, PotionPreset> PRESETS = new LinkedHashMap<>();

    static {
        registerPreset("Warrior Buff", List.of(
            new PotionEffectCombo("STRENGTH", 30, 1),
            new PotionEffectCombo("RESISTANCE", 30, 0)
        ));

        registerPreset("Mage Buff", List.of(
            new PotionEffectCombo("SPEED", 30, 1),
            new PotionEffectCombo("HASTE", 30, 0)
        ));

        registerPreset("Healer Buff", List.of(
            new PotionEffectCombo("REGENERATION", 30, 1),
            new PotionEffectCombo("ABSORPTION", 30, 0)
        ));

        registerPreset("Speed Boost", List.of(
            new PotionEffectCombo("SPEED", 20, 2)
        ));

        registerPreset("Night Vision", List.of(
            new PotionEffectCombo("NIGHT_VISION", 60, 0)
        ));

        registerPreset("Defensive Aura", List.of(
            new PotionEffectCombo("RESISTANCE", 15, 2),
            new PotionEffectCombo("FIRE_RESISTANCE", 15, 0)
        ));

        registerPreset("Damage Boost", List.of(
            new PotionEffectCombo("STRENGTH", 15, 2)
        ));

        registerPreset("Healing Potion", List.of(
            new PotionEffectCombo("INSTANT_HEALTH", 1, 2)
        ));

        registerPreset("Debuff: Weakness", List.of(
            new PotionEffectCombo("WEAKNESS", 20, 0)
        ));

        registerPreset("Debuff: Poison", List.of(
            new PotionEffectCombo("POISON", 10, 1)
        ));
    }

    private static void registerPreset(String name, List<PotionEffectCombo> effects) {
        PRESETS.put(name, new PotionPreset(name, effects));
    }

    public static Map<String, PotionPreset> getAllPresets() {
        return new LinkedHashMap<>(PRESETS);
    }

    public static PotionPreset getPreset(String name) {
        return PRESETS.get(name);
    }

    public static PotionPreset getPresetByIndex(int index) {
        List<PotionPreset> list = new ArrayList<>(PRESETS.values());
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    public static int getTotalPresets() {
        return PRESETS.size();
    }

    public static boolean isValidEffect(String effectName) {
        try {
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effectName.toLowerCase(Locale.ROOT)));
            return type != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static SkillAction createPotionAction(PotionEffectCombo combo) {
        return SkillAction.givePotion(combo.effectType, combo.durationSeconds, combo.amplifier);
    }

    public static void addPresetToSkill(Skill skill, PotionPreset preset) {
        for (PotionEffectCombo combo : preset.effects) {
            skill.addAction(createPotionAction(combo));
        }
    }
}
