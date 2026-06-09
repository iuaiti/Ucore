package com.ultra.ucore.placeholder;

/**
 * Represents a simple placeholder resolver.
 */
@FunctionalInterface
public interface Placeholder {
    String resolve(PlaceholderManager.PlaceholderContext context);
}
