package com.ultra.uskill.model;

import java.util.*;

public class Skill {

    private String id;
    private String displayName;
    private SkillTrigger trigger = SkillTrigger.RIGHT_CLICK;
    private double cooldown = 0.0;
    private boolean cancelEvent = true;
    private double chance = 100.0;

    private List<SkillCondition> conditions = new ArrayList<>();
    private List<SkillAction> actions = new ArrayList<>();

    private int periodTicks = 20;

    public Skill() {}

    public Skill(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName != null ? displayName : id; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public SkillTrigger getTrigger() { return trigger; }
    public void setTrigger(SkillTrigger trigger) { this.trigger = trigger != null ? trigger : SkillTrigger.RIGHT_CLICK; }

    public double getCooldown() { return cooldown; }
    public void setCooldown(double cooldown) { this.cooldown = Math.max(0, cooldown); }

    public boolean isCancelEvent() { return cancelEvent; }
    public void setCancelEvent(boolean cancelEvent) { this.cancelEvent = cancelEvent; }

    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = Math.max(0, Math.min(100, chance)); }

    public List<SkillCondition> getConditions() { return conditions; }
    public void setConditions(List<SkillCondition> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public List<SkillAction> getActions() { return actions; }
    public void setActions(List<SkillAction> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public int getPeriodTicks() { return Math.max(1, periodTicks); }
    public void setPeriodTicks(int periodTicks) { this.periodTicks = Math.max(1, periodTicks); }

    public void addAction(SkillAction action) {
        if (action != null) actions.add(action);
    }

    public void addCondition(SkillCondition cond) {
        if (cond != null) conditions.add(cond);
    }

    public Skill copy() {
        Skill s = new Skill(this.id);
        s.displayName = this.displayName;
        s.trigger = this.trigger;
        s.cooldown = this.cooldown;
        s.cancelEvent = this.cancelEvent;
        s.chance = this.chance;
        s.periodTicks = this.periodTicks;

        s.conditions = new ArrayList<>();
        for (SkillCondition c : this.conditions) s.conditions.add(c.copy());

        s.actions = new ArrayList<>();
        for (SkillAction a : this.actions) s.actions.add(a.copy());

        return s;
    }

    @Override
    public String toString() {
        return "Skill{" + id + ", trigger=" + trigger + ", cd=" + cooldown + "s, actions=" + actions.size() + "}";
    }
}
