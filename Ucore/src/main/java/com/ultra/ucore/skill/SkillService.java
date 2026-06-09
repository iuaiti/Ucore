package com.ultra.ucore.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Service interface cho skill system.
 * <p>
 * USkill implement interface này và đăng ký qua {@link com.ultra.ucore.api.UCoreAPI},
 * UItem và các module khác truy cập skill system thông qua interface này
 * thay vì import USkill trực tiếp.
 */
public interface SkillService {

    void register(Player player, String itemId, List<Skill> skills, SkillTrigger trigger);
    void unregister(Player player);
    boolean executeSkill(Player player, Skill skill, LivingEntity target);
    boolean executeSkills(Player player, String itemId, List<Skill> skills, SkillTrigger trigger, LivingEntity target);
    void tickPeriodicSkills(Player player, String itemId, List<Skill> skills, SkillTrigger triggerType);
    boolean isReady(Player player, String itemId, String skillId, double cooldownSeconds);
    double getRemainingSeconds(Player player, String itemId, String skillId, double cooldownSeconds);
    List<Skill> parseSkills(Map<String, Object> raw);
    Map<String, Object> serializeSkills(java.util.Collection<Skill> skills);
    void onPlayerQuit(Player player);
}
