package com.ultra.ucrate.manager;

import com.ultra.ucrate.animation.CrateAnimation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimationManager {

    private final Map<String, CrateAnimation> animations = new ConcurrentHashMap<>();

    public void register(@NotNull CrateAnimation animation) {
        animations.put(animation.id().toLowerCase(), animation);
    }

    public void unregister(@NotNull String id) {
        animations.remove(id.toLowerCase());
    }

    @Nullable
    public CrateAnimation getAnimation(@NotNull String id) {
        return animations.get(id.toLowerCase());
    }

    public boolean hasAnimation(@NotNull String id) {
        return animations.containsKey(id.toLowerCase());
    }

    @NotNull
    public Map<String, CrateAnimation> getAnimations() {
        return Map.copyOf(animations);
    }
}
