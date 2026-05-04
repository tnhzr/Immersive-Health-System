package com.tnhzr.ihs.disease.symptom;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have successfully loaded a server resourcepack.
 * When detection is enabled in {@code symptoms.yml} the plugin swaps
 * vanilla fallback sounds for their {@code ihs:*} custom counterparts
 * for players reported as loaded.
 *
 * <p>The Minecraft protocol reports pack state as a sequence of events
 * per pack UUID: typically {@code ACCEPTED} → ({@code DOWNLOADED}) →
 * {@code SUCCESSFULLY_LOADED} or {@code FAILED_DOWNLOAD}. We treat
 * {@code SUCCESSFULLY_LOADED} as authoritative. If the client only ever
 * reports {@code ACCEPTED} and nothing else within the configured grace
 * window we still consider the pack loaded — some clients (notably older
 * Forge hybrids) skip the success event.
 */
public final class ResourcePackTracker implements Listener {

    private final ImmersiveHealthSystem plugin;
    private final Map<UUID, Long> acceptedAt = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> loaded  = new ConcurrentHashMap<>();

    public ResourcePackTracker(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * @return true if the given player is known to have a server
     *         resourcepack successfully applied, or accepted it past
     *         the configured grace window.
     */
    public boolean hasPack(Player player) {
        if (!plugin.symptoms().resourcePackDetectionEnabled()) return false;
        UUID id = player.getUniqueId();
        if (loaded.getOrDefault(id, false)) return true;
        Long ts = acceptedAt.get(id);
        if (ts == null) return false;
        long grace = plugin.symptoms().resourcePackGracePeriodMs();
        return grace > 0 && System.currentTimeMillis() - ts >= grace;
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        switch (e.getStatus()) {
            case ACCEPTED -> acceptedAt.put(id, System.currentTimeMillis());
            case SUCCESSFULLY_LOADED -> loaded.put(id, true);
            case DECLINED, FAILED_DOWNLOAD, FAILED_RELOAD, DISCARDED, INVALID_URL -> {
                acceptedAt.remove(id);
                loaded.put(id, false);
            }
            default -> { /* DOWNLOADED and newer intermediate states — ignore */ }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        acceptedAt.remove(id);
        loaded.remove(id);
    }
}
