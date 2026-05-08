package com.tnhzr.ihs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when the tranquilizer routine forces a player to sleep.
 * Listeners can read sleep duration / onset timing for analytics or
 * to show their own UI.
 */
public class IHSPlayerSleptEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int onsetSeconds;
    private final int sleepSeconds;
    private final TranquilizerSource source;

    public IHSPlayerSleptEvent(@NotNull Player who, int onsetSeconds,
                               int sleepSeconds,
                               @NotNull TranquilizerSource source) {
        super(who);
        this.onsetSeconds = onsetSeconds;
        this.sleepSeconds = sleepSeconds;
        this.source = source;
    }

    public int onsetSeconds() { return onsetSeconds; }
    public int sleepSeconds() { return sleepSeconds; }
    public @NotNull TranquilizerSource source() { return source; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }

    public enum TranquilizerSource {
        /** Player drank / ate the tranquilizer directly. */
        CONSUMED,
        /** Player ate food laced with tranquilizer. */
        LACED_FOOD,
        /** Player was hit by a tranquilizer-coated arrow. */
        ARROW,
        /** Triggered via the public API. */
        API
    }
}
