package com.ultra.ucore.gui;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple paginated GUI implementation.
 */
public class PaginatedGui extends Gui {

    private final List<Button> content = new ArrayList<>();
    private final int[] contentSlots;
    private int page;

    public PaginatedGui(String title, int size, int[] contentSlots) {
        super(title, size);
        this.contentSlots = contentSlots;
    }

    public void addContent(Button button) {
        content.add(button);
    }

    public void open(Player player, int page) {
        this.page = Math.max(0, page);
        renderPage();
        super.open(player);
    }

    public void nextPage(Player player) {
        open(player, page + 1);
    }

    public void previousPage(Player player) {
        open(player, Math.max(0, page - 1));
    }

    private void renderPage() {
        for (int slot : contentSlots) {
            getInventory().setItem(slot, null);
        }

        int start = page * contentSlots.length;

        for (int i = 0; i < contentSlots.length; i++) {
            int contentIndex = start + i;

            if (contentIndex < content.size()) {
                setButton(contentSlots[i], content.get(contentIndex));
            }
        }
    }
}
