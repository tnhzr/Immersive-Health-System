package com.tnhzr.ihs.api;

import org.bukkit.entity.Player;

import java.nio.file.Path;

/** Public surface for resourcepack injection / detection. */
public interface ResourcePackService {

    /**
     * @return active installer id ({@code craftengine}, {@code itemsadder},
     *         {@code nexo}, {@code oraxen}, {@code manual}) or
     *         {@code "none"} if injection was disabled.
     */
    String activeInstaller();

    /** Filesystem path the installer wrote the pack to (or {@code null}). */
    Path activeInstallerTarget();

    /**
     * @return true if the plugin currently believes {@code player}'s
     *         client has the IHS pack loaded. Honours the
     *         {@code resource_pack_detection.assume_loaded} override.
     */
    boolean hasPack(Player player);

    /**
     * Force the tracker into the "pack loaded" state for this player.
     * Useful for forks that ship the pack via launcher/modpack and want
     * to bypass {@code PlayerResourcePackStatusEvent} entirely.
     */
    void markLoaded(Player player);
}
