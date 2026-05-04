package com.tnhzr.ihs.disease.listeners;

import com.tnhzr.ihs.disease.DiseaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {

    private final DiseaseManager manager;

    public PlayerLifecycleListener(DiseaseManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        manager.loadPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Clear the freezing visual we used as a tremor symptom so the
        // player rejoins without an icy overlay still pinned on screen.
        manager.revertTremor(e.getPlayer());
        manager.saveAndForget(e.getPlayer().getUniqueId());
        manager.spitCooldown().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        manager.resetOnDeath(e.getEntity().getUniqueId());
    }

    /**
     * The tremor symptom drives the vanilla freezing visual by pinning
     * {@code freezeTicks} to the maximum every tick. Vanilla deals 1 HP
     * of {@link EntityDamageEvent.DamageCause#FREEZING} damage every 40
     * ticks once {@code freezeTicks >= maxFreezeTicks} regardless of
     * whether the entity is actually inside powdered snow, so we have
     * to cancel that damage explicitly while the symptom is active.
     * Real powdered-snow damage is still allowed for everyone else.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFreezeDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FREEZE) return;
        if (!(e.getEntity() instanceof Player player)) return;
        if (manager.isTremorActive(player.getUniqueId())) {
            e.setCancelled(true);
        }
    }
}
