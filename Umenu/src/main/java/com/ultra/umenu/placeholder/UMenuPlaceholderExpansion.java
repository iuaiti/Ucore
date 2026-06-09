package com.ultra.umenu.placeholder;

import com.ultra.ucore.placeholder.PlaceholderManager;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.menu.MenuManager;
import org.bukkit.entity.Player;

import java.util.function.Function;

public final class UMenuPlaceholderExpansion {

    private final MenuManager menuManager;

    public UMenuPlaceholderExpansion(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    public void register() {
        var pm = com.ultra.ucore.api.UCoreAPI.getPlaceholderManager();

        register(pm, "umenu_balance", p -> String.format("%,.2f", menuManager.getProfileManager().getBalance(p)));
        register(pm, "umenu_rank", p -> menuManager.getProfileManager().getRank(p));
        register(pm, "umenu_playtime", p -> menuManager.getProfileManager().getPlaytime(p));
        register(pm, "umenu_kills", p -> String.valueOf(menuManager.getProfileManager().getKills(p)));
        register(pm, "umenu_deaths", p -> String.valueOf(menuManager.getProfileManager().getDeaths(p)));
        register(pm, "umenu_level", p -> String.valueOf(p.getLevel()));
        register(pm, "umenu_online", p -> String.valueOf(org.bukkit.Bukkit.getOnlinePlayers().size()));
        register(pm, "umenu_max_players", p -> String.valueOf(org.bukkit.Bukkit.getMaxPlayers()));
        register(pm, "umenu_world", p -> p.getWorld().getName());
        register(pm, "umenu_health", p -> String.valueOf((int) p.getHealth()));
        register(pm, "umenu_food", p -> String.valueOf(p.getFoodLevel()));
    }

    private void register(PlaceholderManager pm, String key, Function<Player, String> resolver) {
        pm.register(key, context -> {
            if (context.player() == null) return "";
            return resolver.apply(context.player());
        });
    }
}
