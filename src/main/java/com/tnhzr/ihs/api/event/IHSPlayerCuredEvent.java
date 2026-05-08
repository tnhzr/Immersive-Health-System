package com.tnhzr.ihs.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after the player's infection scale for {@link #diseaseId()}
 * dropped to zero, regardless of whether the cure came from a medicine,
 * the {@code /ihs heal} command or the API.
 */
public class IHSPlayerCuredEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String diseaseId;

    public IHSPlayerCuredEvent(@NotNull Player who, @NotNull String diseaseId) {
        super(who);
        this.diseaseId = diseaseId;
    }

    public @NotNull String diseaseId() { return diseaseId; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
