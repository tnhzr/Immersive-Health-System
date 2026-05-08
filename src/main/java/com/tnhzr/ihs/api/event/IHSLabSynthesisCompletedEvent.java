package com.tnhzr.ihs.api.event;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a laboratory finishes producing one batch of its result.
 * Listeners can inspect the produced stack and decide to top it up
 * (e.g. progression rewards) by mutating its amount before the
 * laboratory commits it to the output buffer.
 */
public class IHSLabSynthesisCompletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location laboratory;
    private final String recipeId;
    private final ItemStack result;

    public IHSLabSynthesisCompletedEvent(@NotNull Location laboratory,
                                         @NotNull String recipeId,
                                         @NotNull ItemStack result) {
        this.laboratory = laboratory;
        this.recipeId = recipeId;
        this.result = result;
    }

    public @NotNull Location laboratory() { return laboratory; }
    public @NotNull String recipeId() { return recipeId; }
    public @NotNull ItemStack result() { return result; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
