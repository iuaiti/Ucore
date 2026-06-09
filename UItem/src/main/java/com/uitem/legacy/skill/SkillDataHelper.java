package com.uitem.legacy.skill;

import com.uitem.UItemData;
import com.ultra.uskill.api.USkillProvider;
import com.ultra.uskill.model.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SkillDataHelper {
    private SkillDataHelper() {}

    public static List<Skill> getSkills(UItemData data) {
        Map<String, Object> raw = data.getSkillsRaw();
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        return USkillProvider.get().parseSkills(raw);
    }

    public static void setSkills(UItemData data, List<Skill> skills) {
        data.setSkillsRaw(USkillProvider.get().serializeSkills(skills));
    }

    public static int skillCount(UItemData data) {
        Map<String, Object> raw = data.getSkillsRaw();
        return (raw != null) ? raw.size() : 0;
    }

    /** Serialize a single modified Skill back into the raw map. */
    public static void mergeSkill(UItemData data, Skill skill) {
        Map<String, Object> existing = data.getSkillsRaw();
        Map<String, Object> serialized = USkillProvider.get().serializeSkills(List.of(skill));
        Map<String, Object> merged = new java.util.LinkedHashMap<>(existing);
        merged.putAll(serialized);
        data.setSkillsRaw(merged);
    }
}
