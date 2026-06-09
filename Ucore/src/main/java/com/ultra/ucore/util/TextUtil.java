package com.ultra.ucore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Text utilities.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
    }

    public static Component parse(String text) {
        return MINI_MESSAGE.deserialize(text);
    }

    public static Component parse(String text, boolean disableItalic) {
        Component component = MINI_MESSAGE.deserialize(text);
        if (disableItalic) {
            component = component.decoration(TextDecoration.ITALIC, false);
        }
        return component;
    }

    // === Convenience methods ===

    public static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    public static Component error(String message) {
        return Component.text(message, NamedTextColor.RED);
    }

    public static Component success(String message) {
        return Component.text(message, NamedTextColor.GREEN);
    }

    public static Component info(String message) {
        return Component.text(message, NamedTextColor.GRAY);
    }
}
