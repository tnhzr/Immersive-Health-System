package com.tnhzr.ihs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired immediately before a player gains a new infection. Cancelling
 * the event prevents the infection from being applied.
 */
public class IHSPlayerInfectedEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String diseaseId;
    private final InfectionSource source;
    private boolean cancelled;

    public IHSPlayerInfectedEvent(@NotNull Player who,
                                  @NotNull String diseaseId,
                                  @NotNull InfectionSource source) {
        super(who);
        this.diseaseId = diseaseId;
        this.source = source;
    }

    public @NotNull String diseaseId() { return diseaseId; }
    public @NotNull InfectionSource source() { return source; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }

    /** How this infection was triggered. */
    public enum InfectionSource {
        /** Particle / contact transmission from another infected player. */
        TRANSMISSION,
        /** Daily global roll. */
        GLOBAL_ROLL,
        /** Admin command (/ihs inject). */
        COMMAND,
        /** Triggered through the public API. */
        API
    }
}
