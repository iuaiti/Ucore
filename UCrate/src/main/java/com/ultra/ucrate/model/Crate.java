package com.ultra.ucrate.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Crate {

    private final String id;
    private String displayName;
    private ItemStack displayItem;
    private CrateLocation location;
    private CrateType type;
    private String animationId;
    private String keyId;
    private String permission;
    private boolean requireKey;
    private int cooldownSeconds;
    private final List<Reward> rewards;

    public Crate(String id) {
        this.id = id;
        this.displayName = id;
        this.type = CrateType.DEFAULT;
        this.animationId = "instant";
        this.keyId = id;
        this.requireKey = true;
        this.cooldownSeconds = 0;
        this.rewards = new ArrayList<>();
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemStack displayItem() {
        return displayItem;
    }

    public void displayItem(ItemStack displayItem) {
        this.displayItem = displayItem;
    }

    public CrateLocation location() {
        return location;
    }

    public void location(CrateLocation location) {
        this.location = location;
    }

    public CrateType type() {
        return type;
    }

    public void type(CrateType type) {
        this.type = type;
    }

    public String animationId() {
        return animationId;
    }

    public void animationId(String animationId) {
        this.animationId = animationId;
    }

    public String keyId() {
        return keyId;
    }

    public void keyId(String keyId) {
        this.keyId = keyId;
    }

    public String permission() {
        return permission;
    }

    public void permission(String permission) {
        this.permission = permission;
    }

    public boolean requireKey() {
        return requireKey;
    }

    public void requireKey(boolean requireKey) {
        this.requireKey = requireKey;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public void cooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    public List<Reward> rewards() {
        return rewards;
    }

    public void addReward(Reward reward) {
        this.rewards.add(reward);
    }

    public void removeReward(String rewardId) {
        this.rewards.removeIf(r -> r.id().equals(rewardId));
    }

    public Reward getReward(String rewardId) {
        return rewards.stream()
                .filter(r -> r.id().equals(rewardId))
                .findFirst()
                .orElse(null);
    }

    public double totalChance() {
        return rewards.stream().mapToDouble(Reward::chance).sum();
    }
}
