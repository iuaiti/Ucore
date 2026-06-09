package com.uitem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Utility class cho text và component.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
    }

    /**
     * Parse MiniMessage string thành Component.
     */
    public static Component parse(String miniMessageStr) {
        if (miniMessageStr == null || miniMessageStr.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessageStr)
                .decoration(TextDecoration.ITALIC, false); // Tắt italic mặc định của lore
    }

    /**
     * Tạo Component đơn giản với màu.
     */
    public static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Prefix cho tin nhắn plugin.
     */
    public static Component prefix() {
        return MINI_MESSAGE.deserialize("<gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <dark_gray>│ ");
    }

    /**
     * Tin nhắn với prefix.
     */
    public static Component prefixed(String message) {
        return prefix().append(MINI_MESSAGE.deserialize(message));
    }

    /**
     * Tin nhắn lỗi.
     */
    public static Component error(String message) {
        return prefix().append(Component.text(message, NamedTextColor.RED));
    }

    /**
     * Tin nhắn thành công.
     */
    public static Component success(String message) {
        return prefix().append(Component.text(message, NamedTextColor.GREEN));
    }

    /**
     * Tin nhắn thông tin.
     */
    public static Component info(String message) {
        return prefix().append(Component.text(message, NamedTextColor.GRAY));
    }
}
