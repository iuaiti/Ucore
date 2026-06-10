package com.ultra.uskill.tree;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;

import java.util.*;

public class PlayerStats {

    private final Map<StatType, Double> stats = new EnumMap<>(StatType.class);

    public PlayerStats() {}

    public void add(StatType type, double amount) {
        stats.merge(type, amount, Double::sum);
    }

    public double get(StatType type) {
        return stats.getOrDefault(type, 0.0);
    }

    public Map<StatType, Double> getAll() {
        return Collections.unmodifiableMap(stats);
    }

    public void clear() {
        stats.clear();
    }

    public static PlayerStats calculate(PlayerSkillData data, SkillTreeManager manager) {
        PlayerStats ps = new PlayerStats();
        for (Map.Entry<String, Integer> entry : data.getSkillLevels().entrySet()) {
            SkillTreeNode node = manager.getNode(entry.getKey());
            if (node == null) continue;
            int level = entry.getValue();
            for (Map.Entry<StatType, Double> statEntry : node.getStatsPerLevel().entrySet()) {
                double value = statEntry.getValue() * level;
                ps.add(statEntry.getKey(), value);
            }
        }
        return ps;
    }

    public void applyToPlayer(Player player) {
        applyAttributeModifiers(player);
        applyPotionEffects(player);
    }

    private void applyAttributeModifiers(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double bonus = get(StatType.MAX_HEALTH);
            double base = maxHealth.getDefaultValue();
            maxHealth.setBaseValue(base + bonus);
        }

        AttributeInstance movement = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement != null) {
            double bonus = get(StatType.MOVEMENT_SPEED) * 0.01;
            double base = movement.getDefaultValue();
            movement.setBaseValue(base + base * bonus);
        }

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) {
            double bonus = get(StatType.ATTACK_SPEED) * 0.01;
            double base = attackSpeed.getDefaultValue();
            attackSpeed.setBaseValue(base + base * bonus);
        }

        AttributeInstance luck = player.getAttribute(Attribute.LUCK);
        if (luck != null) {
            double bonus = get(StatType.LUCK);
            double base = luck.getDefaultValue();
            luck.setBaseValue(base + bonus);
        }
    }

    private void applyPotionEffects(Player player) {
        double regen = get(StatType.REGENERATION);
        if (regen > 0) {
            int amplifier = Math.min(4, (int) (regen / 2));
            addEffect(player, "regeneration", Integer.MAX_VALUE, amplifier);
        }

        double manaRegen = get(StatType.MANA_REGEN);
        if (manaRegen > 0) {
        }

        double jump = get(StatType.JUMP_BOOST);
        if (jump > 0) {
            int amplifier = Math.min(4, (int) jump);
            addEffect(player, "jump_boost", Integer.MAX_VALUE, amplifier);
        }

        double fireResist = get(StatType.FIRE_RESISTANCE);
        if (fireResist >= 1) {
            addEffect(player, "fire_resistance", Integer.MAX_VALUE, 0);
        }

        double absorption = get(StatType.ABSORPTION);
        if (absorption > 0) {
            player.setAbsorptionAmount(absorption);
        }
    }

    private void addEffect(Player player, String effectName, int duration, int amplifier) {
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effectName));
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, false, false, true));
        }
    }

    public void removeFromPlayer(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getDefaultValue());
        }
        AttributeInstance movement = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(movement.getDefaultValue());
        }
        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.setBaseValue(attackSpeed.getDefaultValue());
        }
        AttributeInstance luck = player.getAttribute(Attribute.LUCK);
        if (luck != null) {
            luck.setBaseValue(luck.getDefaultValue());
        }

        player.setAbsorptionAmount(0);

        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            if (effect.getDuration() > 1000000) {
                player.removePotionEffect(effect.getType());
            }
        }
    }
}
