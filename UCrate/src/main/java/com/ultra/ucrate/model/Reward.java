package com.ultra.ucrate.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class Reward {

    private final String id;
    private double chance;
    private String displayName;
    private ItemStack displayItem;
    private List<String> commands;
    private ItemStack item;
    private double money;
    private List<String> permissions;
    private List<String> messages;
    private List<String> broadcasts;
    private Map<String, Object> options;

    public Reward(String id) {
        this.id = id;
        this.chance = 10.0;
        this.displayName = id;
        this.commands = List.of();
        this.permissions = List.of();
        this.messages = List.of();
        this.broadcasts = List.of();
    }

    public String id() {
        return id;
    }

    public double chance() {
        return chance;
    }

    public void chance(double chance) {
        this.chance = Math.max(0, chance);
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

    public List<String> commands() {
        return commands;
    }

    public void commands(List<String> commands) {
        this.commands = commands != null ? List.copyOf(commands) : List.of();
    }

    public ItemStack item() {
        return item;
    }

    public void item(ItemStack item) {
        this.item = item;
    }

    public double money() {
        return money;
    }

    public void money(double money) {
        this.money = Math.max(0, money);
    }

    public List<String> permissions() {
        return permissions;
    }

    public void permissions(List<String> permissions) {
        this.permissions = permissions != null ? List.copyOf(permissions) : List.of();
    }

    public List<String> messages() {
        return messages;
    }

    public void messages(List<String> messages) {
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }

    public List<String> broadcasts() {
        return broadcasts;
    }

    public void broadcasts(List<String> broadcasts) {
        this.broadcasts = broadcasts != null ? List.copyOf(broadcasts) : List.of();
    }

    public Map<String, Object> options() {
        return options;
    }

    public void options(Map<String, Object> options) {
        this.options = options;
    }
}
