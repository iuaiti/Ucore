package com.ultra.ucrate.gui;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.CrateType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CrateEditMenu {

    private static final String TITLE = "<gradient:#FF6B35:#FFD700>Edit Crate</gradient>";

    private final Crate crate;

    public CrateEditMenu(Crate crate) {
        this.crate = crate;
    }

    public void open(Player player) {
        Gui gui = new Gui(TITLE, 27);

        ItemStack info = ItemBuilder.create(Material.ENDER_CHEST)
                .name("<gold>" + crate.displayName())
                .lore(
                        "<gray>ID: " + crate.id() + "</gray>",
                        "<gray>Type: " + crate.type().name() + "</gray>",
                        "<gray>Animation: " + crate.animationId() + "</gray>",
                        "<gray>Key: " + crate.keyId() + "</gray>",
                        "<gray>Cooldown: " + crate.cooldownSeconds() + "s</gray>",
                        "<gray>Require Key: " + crate.requireKey() + "</gray>"
                )
                .build();
        gui.setButton(13, new Button(info, null));

        ItemStack typeItem = ItemBuilder.create(Material.COMPARATOR)
                .name("<yellow>Change Type</yellow>")
                .lore("<gray>Current: " + crate.type().name() + "</gray>", "", "<green>Click to cycle</green>")
                .build();
        gui.setButton(10, new Button(typeItem, event -> {
            CrateType[] types = CrateType.values();
            int next = (crate.type().ordinal() + 1) % types.length;
            crate.type(types[next]);
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            open(player);
        }));

        ItemStack animationItem = ItemBuilder.create(Material.FIREWORK_ROCKET)
                .name("<yellow>Change Animation</yellow>")
                .lore("<gray>Current: " + crate.animationId() + "</gray>", "", "<green>Click to change</green>")
                .build();
        gui.setButton(11, new Button(animationItem, event -> {
            var animations = UCrateProvider.get().getAnimationManager().getAnimations();
            var ids = animations.keySet().toArray(String[]::new);
            if (ids.length == 0) return;
            int currentIndex = -1;
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equals(crate.animationId())) {
                    currentIndex = i;
                    break;
                }
            }
            int next = (currentIndex + 1) % ids.length;
            crate.animationId(ids[next]);
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            open(player);
        }));

        ItemStack keyItem = ItemBuilder.create(Material.TRIPWIRE_HOOK)
                .name("<yellow>Key Settings</yellow>")
                .lore("<gray>Key ID: " + crate.keyId() + "</gray>", "<gray>Require: " + crate.requireKey() + "</gray>", "", "<green>Click to toggle require-key</green>")
                .build();
        gui.setButton(12, new Button(keyItem, event -> {
            crate.requireKey(!crate.requireKey());
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            open(player);
        }));

        ItemStack rewardsItem = ItemBuilder.create(Material.GOLD_INGOT)
                .name("<yellow>Rewards</yellow>")
                .lore("<gray>Count: " + crate.rewards().size() + "</gray>", "", "<green>Click to manage</green>")
                .build();
        gui.setButton(14, new Button(rewardsItem, event -> {
            new RewardListMenu(crate).open(player);
        }));

        ItemStack cooldownItem = ItemBuilder.create(Material.CLOCK)
                .name("<yellow>Cooldown: " + crate.cooldownSeconds() + "s</yellow>")
                .lore("<gray>Click to reset</gray>")
                .build();
        gui.setButton(15, new Button(cooldownItem, event -> {
            crate.cooldownSeconds(0);
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            open(player);
        }));

        ItemStack back = ItemBuilder.create(Material.ARROW)
                .name("<red>Back</red>")
                .build();
        gui.setButton(22, new Button(back, event -> new CrateListMenu().open(player)));

        gui.open(player);
    }
}
