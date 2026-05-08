package com.tnhzr.ihs.disease.listeners;

import com.tnhzr.ihs.disease.DiseaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        // Defensive cleanup: legacy installs (pre-particle tremor) may
        // have left freezeTicks pinned at the cap. Clear once on join
        // so upgraded servers don't see lingering freezing vignettes.
        manager.revertTremor(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.revertTremor(e.getPlayer());
        manager.saveAndForget(e.getPlayer().getUniqueId());
        manager.spitCooldown().remove(e.getPlayer().getUniqueId());
    }
}
