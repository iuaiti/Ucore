package com.uitem.legacy.skill;

import java.util.*;

/**
 * Điều kiện đơn giản cho skill.
 * Hỗ trợ: permission, chance, health, sneaking, world, etc.
 */
public class SkillCondition {

    public enum ConditionType {
        PERMISSION("Có permission"),
        CHANCE("Tỷ lệ phần trăm"),
        HEALTH_MIN("Máu tối thiểu"),
        HEALTH_MAX("Máu tối đa"),
        FOOD_MIN("Độ đói tối thiểu"),
        SNEAKING("Đang shift"),
        NOT_SNEAKING("Không shift"),
        IN_WORLD("Ở world cụ thể"),
        HAS_OFFHAND("Có item ở tay phụ"),
        HAS_PERMISSION("Có permission (alias)"),
        ALWAYS("Luôn đúng");

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

    // Common factories
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
