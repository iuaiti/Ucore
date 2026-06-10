package com.uitem.util;

import com.uitem.UItemData;
import com.uitem.recipe.UItemRecipe;
import com.ultra.uskill.USkillPlugin;
import com.ultra.uskill.tree.PlayerSkillData;
import com.ultra.uskill.tree.SkillTreeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public final class SkillRequirementHelper {

    private static Boolean uskillLoaded;

    private SkillRequirementHelper() {}

    public static boolean isUSkillLoaded() {
        if (uskillLoaded == null) {
            uskillLoaded = Bukkit.getPluginManager().getPlugin("USkill") != null;
        }
        return uskillLoaded;
    }

    public static SkillTreeManager getSkillTreeManager() {
        if (!isUSkillLoaded()) return null;
        try {
            USkillPlugin uskill = (USkillPlugin) Bukkit.getPluginManager().getPlugin("USkill");
            return uskill != null ? uskill.getSkillTreeManager() : null;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[UItem] Failed to get USkill SkillTreeManager", e);
            return null;
        }
    }

    public static boolean canCraft(Player player, UItemRecipe recipe) {
        if (recipe == null || !recipe.hasSkillRequirement()) return true;
        return checkRequirement(player, recipe.getRequiredSkillNode(), recipe.getRequiredSkillLevel());
    }

    public static boolean canUse(Player player, UItemData data) {
        if (data == null || !data.hasSkillRequirement()) return true;
        return checkRequirement(player, data.getRequiredSkillNode(), data.getRequiredSkillLevel());
    }

    private static boolean checkRequirement(Player player, String nodeId, int requiredLevel) {
        if (!isUSkillLoaded() || nodeId == null) return true;
        SkillTreeManager manager = getSkillTreeManager();
        if (manager == null) return true;
        PlayerSkillData skillData = manager.getPlayerData(player.getUniqueId());
        return skillData.getSkillLevel(nodeId) >= requiredLevel;
    }

    public static String getRequirementText(UItemRecipe recipe) {
        if (recipe == null || !recipe.hasSkillRequirement()) return null;
        return "§cYêu cầu: §6" + recipe.getRequiredSkillNode()
                + " §cCấp §e" + recipe.getRequiredSkillLevel();
    }

    public static String getRequirementText(UItemData data) {
        if (data == null || !data.hasSkillRequirement()) return null;
        return "§cYêu cầu: §6" + data.getRequiredSkillNode()
                + " §cCấp §e" + data.getRequiredSkillLevel();
    }
}
