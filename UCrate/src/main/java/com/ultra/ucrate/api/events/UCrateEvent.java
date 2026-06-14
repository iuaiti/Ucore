package com.ultra.ucrate.api.events;

import com.ultra.ucrate.model.Crate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class UCrateEvent extends Event {

    private final Crate crate;

    protected UCrateEvent(@NotNull Crate crate) {
        this.crate = crate;
    }

    protected UCrateEvent(@NotNull Crate crate, boolean async) {
        super(async);
        this.crate = crate;
    }

    @NotNull
    public final Crate getCrate() {
        return crate;
    }
}
