package com.ultra.uskill.api;

import com.ultra.uskill.cooldown.CooldownManager;
import com.ultra.uskill.execution.SkillExecutor;
import com.ultra.uskill.model.Skill;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Public API cho USkill plugin.
 * Các plugin khác (UItem, UMenu, UBoss) dùng interface này để tương tác.
 */
public interface USkillAPI {

    CooldownManager getCooldownManager();

    SkillExecutor getExecutor();

    /**
     * Thực thi một skill cho player.
     * @return true nếu skill được kích hoạt thành công
     */
    boolean executeSkill(org.bukkit.entity.Player player, Skill skill, org.bukkit.entity.LivingEntity target);

    /**
     * Kiểm tra tất cả conditions của skill.
     */
    boolean checkConditions(org.bukkit.entity.Player player, Skill skill, org.bukkit.entity.LivingEntity target);

    /**
     * Parse raw YAML section data thành List<Skill>.
     * @param raw Map<String, Object> từ ConfigurationSection.getValues(false)
     * @return parsed skills
     */
    List<Skill> parseSkills(Map<String, Object> raw);

    /**
     * Serialize List<Skill> về raw Map để lưu YAML.
     */
    Map<String, Object> serializeSkills(Collection<Skill> skills);
}
