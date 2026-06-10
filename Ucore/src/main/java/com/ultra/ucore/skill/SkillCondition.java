package com.ultra.ucore.skill;

import java.util.*;

public class SkillCondition {

    public enum ConditionType {
        PERMISSION("Has permission"),
        CHANCE("Chance percentage"),
        HEALTH_MIN("Minimum health"),
        HEALTH_MAX("Maximum health"),
        FOOD_MIN("Minimum food"),
        SNEAKING("Is sneaking"),
        NOT_SNEAKING("Not sneaking"),
        IN_WORLD("In specific world"),
        HAS_OFFHAND("Has offhand item"),
        HAS_PERMISSION("Has permission (alias)"),
        ALWAYS("Always true"),
        HAS_POTION("Has potion effect"),
        LEVEL_MIN("Minimum player level"),
        EXPERIENCE_MIN("Minimum total exp"),
        IS_IN_WATER("In water"),
        IS_IN_LAVA("In lava"),
        IS_ON_GROUND("On ground"),
        IS_RAINING("Raining in world"),
        IS_NIGHT("Night time"),
        IS_DAY("Day time"),
        BIOME("In specific biome"),
        IS_SPRINTING("Is sprinting"),
        IS_WEARING("Is wearing material"),
        HAS_MONEY("Has enough money"),
        IS_ON_FIRE("Is on fire"),
        HEALTH_PERCENT_MIN("Min health percent"),
        TARGET_IN_RANGE("Target in range"),
        HAS_ITEM("Has item in inventory"),
        IS_MOUNTED("Is riding entity"),
        LIGHT_LEVEL("Light level check");

        private final String display;
        ConditionType(String d) { this.display = d; }
        public String getDisplay() { return display; }
    }

    private ConditionType type;
    private Map<String, Object> params = new LinkedHashMap<>();

    public SkillCondition() {
        this.type = ConditionType.ALWAYS;
    }

    public SkillCondition(ConditionType type) {
        this.type = type;
    }

    public SkillCondition(ConditionType type, Map<String, Object> params) {
        this.type = type;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public ConditionType getType() { return type; }
    public void setType(ConditionType type) { this.type = type; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>(); }

    public void setParam(String k, Object v) {
        if (params == null) params = new LinkedHashMap<>();
        params.put(k, v);
    }

    public Object getParam(String k) { return params != null ? params.get(k) : null; }

    public double getDouble(String k, double def) {
        Object v = getParam(k);
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    public int getInt(String k, int def) {
        Object v = getParam(k);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    public String getString(String k, String def) {
        Object v = getParam(k);
        return v != null ? String.valueOf(v) : def;
    }

    public boolean getBool(String k, boolean def) {
        Object v = getParam(k);
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    public SkillCondition copy() {
        return new SkillCondition(type, new LinkedHashMap<>(params));
    }

    public static SkillCondition chance(double percent) {
        SkillCondition c = new SkillCondition(ConditionType.CHANCE);
        c.setParam("value", percent);
        return c;
    }

    public static SkillCondition permission(String perm) {
        SkillCondition c = new SkillCondition(ConditionType.PERMISSION);
        c.setParam("permission", perm);
        return c;
    }

    @Override
    public String toString() {
        return type + params.toString();
    }
}
