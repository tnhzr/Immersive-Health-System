package com.tnhzr.ihs.disease.listeners;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.disease.Disease;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Replaces the vanilla chat death message when a player was killed by a
 * disease's stage "kill" action. The disease that triggered the kill is
 * tracked by {@link com.tnhzr.ihs.disease.DiseaseManager#consumeLastKillingDisease}.
 *
 * <p>The message template is read from {@code infections.yml ->
 * <disease>.death_message} and supports the {@code %player%} and
 * {@code %disease%} placeholders together with legacy {@code &}-color
 * codes. The replacement uses {@link PlayerDeathEvent#deathMessage(Component)}
 * so the broadcast goes through the vanilla death-broadcast pipeline (every
 * player who can see vanilla deaths sees ours, and admin tools that intercept
 * death messages still get them).</p>
 */
public final class DiseaseDeathListener implements Listener {

    private final ImmersiveHealthSystem plugin;

    public DiseaseDeathListener(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        String diseaseId = plugin.diseases().consumeLastKillingDisease(player.getUniqueId());
        // Reset disease state on death (both for natural deaths and for
        // disease-caused ones) — keeps the previous behaviour from the
        // old PlayerLifecycleListener.onDeath handler.
        plugin.diseases().resetOnDeath(player.getUniqueId());
        if (diseaseId == null) return;
        Disease disease = plugin.diseases().disease(diseaseId);
        if (disease == null) return;
        String template = disease.deathMessage();
        if (template == null || template.isBlank()) return;
        String formatted = template
                .replace("%player%", player.getName())
                .replace("%disease%", disease.name());
        e.deathMessage(Text.component(formatted));
    }
}
