package com.ultra.uskill.tree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSkillData {

    private final UUID playerId;
    private final Map<String, Integer> skillLevels;
    private int skillPoints;
    private int totalSpent;

    public PlayerSkillData(UUID playerId) {
        this.playerId = playerId;
        this.skillLevels = new ConcurrentHashMap<>();
        this.skillPoints = 0;
        this.totalSpent = 0;
    }

    public UUID getPlayerId() { return playerId; }

    public Map<String, Integer> getSkillLevels() { return skillLevels; }

    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }

    public void setSkillLevel(String skillId, int level) {
        if (level <= 0) {
            skillLevels.remove(skillId);
        } else {
            skillLevels.put(skillId, level);
        }
    }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = Math.max(0, skillPoints); }
    public void addSkillPoints(int amount) { this.skillPoints = Math.max(0, this.skillPoints + amount); }
    public boolean spendPoints(int amount) {
        if (amount < 0 || this.skillPoints < amount) return false;
        this.skillPoints -= amount;
        this.totalSpent += amount;
        return true;
    }

    public int getTotalSpent() { return totalSpent; }
    public void setTotalSpent(int totalSpent) { this.totalSpent = totalSpent; }

    public boolean isUnlocked(String skillId) {
        return skillLevels.containsKey(skillId) && skillLevels.get(skillId) > 0;
    }

    public boolean canUpgrade(SkillTreeNode node, SkillTreeManager manager) {
        int currentLevel = getSkillLevel(node.getId());
        if (currentLevel >= node.getMaxLevel()) return false;
        int nextLevel = currentLevel;
        int cost = node.getCost(nextLevel);
        if (skillPoints < cost) return false;

        for (String prereqId : node.getPrerequisites()) {
            SkillTreeNode prereq = manager.getNode(prereqId);
            if (prereq == null) continue;
            if (getSkillLevel(prereqId) < prereq.getMaxLevel()) return false;
        }

        return true;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("skill-points", skillPoints);
        map.put("total-spent", totalSpent);

        Map<String, Object> levels = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : skillLevels.entrySet()) {
            if (e.getValue() > 0) levels.put(e.getKey(), e.getValue());
        }
        map.put("skill-levels", levels);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static PlayerSkillData deserialize(UUID playerId, Map<String, Object> data) {
        PlayerSkillData psd = new PlayerSkillData(playerId);
        if (data == null) return psd;

        if (data.containsKey("skill-points"))
            psd.setSkillPoints(((Number) data.get("skill-points")).intValue());
        if (data.containsKey("total-spent"))
            psd.setTotalSpent(((Number) data.get("total-spent")).intValue());

        if (data.containsKey("skill-levels")) {
            Object raw = data.get("skill-levels");
            if (raw instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String skillId = String.valueOf(e.getKey());
                    int level = e.getValue() instanceof Number n ? n.intValue() : 0;
                    if (level > 0) psd.skillLevels.put(skillId, level);
                }
            }
        }
        return psd;
    }
}
