package com.uitem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Utility class cho text và component.
 * <p>
 * Toàn bộ logic text delegate lên UCore's TextUtil. Các method prefix/prefixed
 * là riêng của UItem (UCore không có prefix riêng cho từng plugin).
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
    }

    /**
     * Parse MiniMessage string thành Component (italic tắt).
     */
    public static Component parse(String miniMessageStr) {
        if (miniMessageStr == null || miniMessageStr.isEmpty()) {
            return Component.empty();
        }
        return com.ultra.ucore.util.TextUtil.parse(miniMessageStr, true);
    }

    /**
     * Tạo Component đơn giản với màu (italic tắt).
     */
    public static Component colored(String text, NamedTextColor color) {
        return com.ultra.ucore.util.TextUtil.colored(text, color);
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
        return prefix().append(com.ultra.ucore.util.TextUtil.parse(message));
    }

    /**
     * Tin nhắn lỗi (có prefix UItem).
     */
    public static Component error(String message) {
        return prefix().append(com.ultra.ucore.util.TextUtil.error(message));
    }

    /**
     * Tin nhắn thành công (có prefix UItem).
     */
    public static Component success(String message) {
        return prefix().append(com.ultra.ucore.util.TextUtil.success(message));
    }

    /**
     * Tin nhắn thông tin (có prefix UItem).
     */
    public static Component info(String message) {
        return prefix().append(com.ultra.ucore.util.TextUtil.info(message));
    }
}
