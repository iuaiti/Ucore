package com.ultra.umenu.model;

import org.bukkit.Material;

import java.util.List;

public record MenuItem(
        String key,
        Material material,
        String name,
        List<String> lore,
        String texture,
        int customModelData,
        int amount,
        boolean enchanted
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String key;
        private Material material = Material.STONE;
        private String name = "";
        private List<String> lore = List.of();
        private String texture;
        private int customModelData;
        private int amount = 1;
        private boolean enchanted;

        public Builder key(String key) { this.key = key; return this; }
        public Builder material(Material material) { this.material = material; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder lore(List<String> lore) { this.lore = lore; return this; }
        public Builder texture(String texture) { this.texture = texture; return this; }
        public Builder customModelData(int customModelData) { this.customModelData = customModelData; return this; }
        public Builder amount(int amount) { this.amount = amount; return this; }
        public Builder enchanted(boolean enchanted) { this.enchanted = enchanted; return this; }

        public MenuItem build() {
            return new MenuItem(key, material, name, lore, texture, customModelData, amount, enchanted);
        }
    }
}
