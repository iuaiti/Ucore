package com.uitem.legacy.skill;

import com.uitem.UItemData;
import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SkillDataHelper {
    private SkillDataHelper() {}

    private static SkillService skillService() {
        return UCoreAPI.getSkillService();
    }

    public static List<Skill> getSkills(UItemData data) {
        Map<String, Object> raw = data.getSkillsRaw();
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        return skillService().parseSkills(raw);
    }

    public static void setSkills(UItemData data, List<Skill> skills) {
        data.setSkillsRaw(skillService().serializeSkills(skills));
    }

    public static int skillCount(UItemData data) {
        Map<String, Object> raw = data.getSkillsRaw();
        return (raw != null) ? raw.size() : 0;
    }

    /** Serialize a single modified Skill back into the raw map. */
    public static void mergeSkill(UItemData data, Skill skill) {
        Map<String, Object> existing = data.getSkillsRaw();
        Map<String, Object> serialized = skillService().serializeSkills(List.of(skill));
        Map<String, Object> merged = new java.util.LinkedHashMap<>(existing);
        merged.putAll(serialized);
        data.setSkillsRaw(merged);
    }
}
