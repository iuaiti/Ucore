package com.ultra.uskill.api;

import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillService;
import com.ultra.uskill.cooldown.CooldownManager;
import com.ultra.uskill.execution.SkillExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Public API cho USkill plugin.
 * <p>
 * Kế thừa {@link SkillService} để các module khác (UItem, UMenu)
 * có thể dùng qua {@link com.ultra.ucore.api.UCoreAPI#getSkillService()}
 * mà không cần import USkill trực tiếp.
 */
public interface USkillAPI extends SkillService {

    CooldownManager getCooldownManager();

    SkillExecutor getExecutor();

    boolean executeSkill(org.bukkit.entity.Player player, Skill skill, org.bukkit.entity.LivingEntity target);

    boolean checkConditions(org.bukkit.entity.Player player, Skill skill, org.bukkit.entity.LivingEntity target);

    List<Skill> parseSkills(Map<String, Object> raw);

    Map<String, Object> serializeSkills(Collection<Skill> skills);
}
