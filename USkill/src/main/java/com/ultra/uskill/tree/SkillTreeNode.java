package com.ultra.uskill.tree;

import org.bukkit.Material;

import java.util.*;

public class SkillTreeNode {

    private final String id;
    private String displayName;
    private List<String> description;
    private Material icon;
    private int slot;
    private int maxLevel;
    private int[] costPerLevel;
    private Set<String> prerequisites;
    private int requiredLevel;
    private Map<StatType, Double> statsPerLevel;
    private List<String> children;

    public SkillTreeNode(String id) {
        this.id = id;
        this.displayName = id;
        this.description = new ArrayList<>();
        this.icon = Material.NETHER_STAR;
        this.slot = 22;
        this.maxLevel = 5;
        this.costPerLevel = new int[]{1, 2, 3, 4, 5};
        this.prerequisites = new LinkedHashSet<>();
        this.requiredLevel = 1;
        this.statsPerLevel = new LinkedHashMap<>();
        this.children = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description; }

    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = Math.max(1, maxLevel); }

    public int[] getCostPerLevel() { return costPerLevel; }
    public void setCostPerLevel(int[] costPerLevel) { this.costPerLevel = costPerLevel; }
    public int getCost(int level) {
        if (costPerLevel == null || level >= costPerLevel.length) return 1;
        return costPerLevel[level];
    }

    public Set<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(Set<String> prerequisites) { this.prerequisites = prerequisites; }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = Math.max(1, requiredLevel); }

    public Map<StatType, Double> getStatsPerLevel() { return statsPerLevel; }
    public void setStatsPerLevel(Map<StatType, Double> statsPerLevel) { this.statsPerLevel = statsPerLevel; }

    public List<String> getChildren() { return children; }
    public void setChildren(List<String> children) { this.children = children; }

    public double getStatAtLevel(StatType type, int level) {
        Double base = statsPerLevel.get(type);
        if (base == null) return 0;
        return base * level;
    }

    @SuppressWarnings("unchecked")
    public static SkillTreeNode fromConfig(String id, Map<String, Object> config) {
        SkillTreeNode node = new SkillTreeNode(id);
        if (config.containsKey("display-name"))
            node.setDisplayName(String.valueOf(config.get("display-name")));
        if (config.containsKey("description")) {
            Object desc = config.get("description");
            if (desc instanceof List<?> list) {
                node.setDescription((List<String>) (List<?>) list);
            }
        }
        if (config.containsKey("icon")) {
            try {
                node.setIcon(Material.valueOf(String.valueOf(config.get("icon")).toUpperCase(Locale.ROOT)));
            } catch (Exception ignored) {}
        }
        if (config.containsKey("slot"))
            node.setSlot(((Number) config.get("slot")).intValue());
        if (config.containsKey("max-level"))
            node.setMaxLevel(((Number) config.get("max-level")).intValue());
        if (config.containsKey("cost-per-level")) {
            Object costs = config.get("cost-per-level");
            if (costs instanceof List<?> costList) {
                int[] arr = new int[costList.size()];
                for (int i = 0; i < costList.size(); i++) {
                    arr[i] = ((Number) costList.get(i)).intValue();
                }
                node.setCostPerLevel(arr);
            }
        }
        if (config.containsKey("prerequisites")) {
            Object prereqs = config.get("prerequisites");
            if (prereqs instanceof List<?> list) {
                node.setPrerequisites(new LinkedHashSet<>((List<String>) (List<?>) list));
            }
        }
        if (config.containsKey("required-level"))
            node.setRequiredLevel(((Number) config.get("required-level")).intValue());
        if (config.containsKey("stats-per-level")) {
            Object stats = config.get("stats-per-level");
            if (stats instanceof Map<?, ?> map) {
                node.setStatsPerLevel(StatType.deserialize((Map<?, ?>) map));
            }
        }
        if (config.containsKey("children")) {
            Object childs = config.get("children");
            if (childs instanceof List<?> list) {
                node.setChildren((List<String>) (List<?>) list);
            }
        }
        return node;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("display-name", displayName);
        map.put("description", description);
        map.put("icon", icon.name());
        map.put("slot", slot);
        map.put("max-level", maxLevel);
        List<Integer> costs = new ArrayList<>();
        for (int c : costPerLevel) costs.add(c);
        map.put("cost-per-level", costs);
        map.put("prerequisites", new ArrayList<>(prerequisites));
        map.put("required-level", requiredLevel);
        map.put("stats-per-level", StatType.serialize(statsPerLevel));
        map.put("children", children);
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillTreeNode that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
