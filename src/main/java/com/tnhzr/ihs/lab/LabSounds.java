package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Resolves laboratory block sounds from {@code config.yml ->
 * laboratory.block.sounds.*} and plays them either at a world location
 * (everyone in earshot) or for a specific listener (so resource-pack
 * detection can pick a per-player key — currently unused but ready).
 *
 * <p>Each entry can be either:
 * <ul>
 *   <li>An object: {@code { key: "...", volume: 1.0, pitch: 1.0 }}</li>
 *   <li>A bare string: just the sound key, default volume/pitch.</li>
 * </ul>
 * Empty / missing key disables that particular sound silently.</p>
 */
public final class LabSounds {

    private final ImmersiveHealthSystem plugin;

    public LabSounds(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    /** Plays the named lab sound at the given location for everyone nearby. */
    public void play(Location at, String name) {
        if (at == null || at.getWorld() == null) return;
        Entry e = entry(name);
        if (e == null) return;
        try {
            at.getWorld().playSound(at, e.key, SoundCategory.BLOCKS, e.volume, e.pitch);
            return;
        } catch (Throwable ignored) { /* fall through to enum lookup */ }
        try {
            Sound s = Sound.valueOf(e.key.toUpperCase()
                    .replace('.', '_').replace(':', '_'));
            at.getWorld().playSound(at, s, SoundCategory.BLOCKS, e.volume, e.pitch);
        } catch (Throwable ignored) { /* bad key — silent no-op */ }
    }

    /** Plays the named lab sound for one specific listener (used for
     *  per-player resource-pack-aware sound swaps in the future). */
    public void playFor(Player listener, Location at, String name) {
        if (listener == null || at == null) return;
        Entry e = entry(name);
        if (e == null) return;
        try {
            listener.playSound(at, e.key, SoundCategory.BLOCKS, e.volume, e.pitch);
        } catch (Throwable ignored) { /* silent */ }
    }

    private Entry entry(String name) {
        ConfigurationSection sounds = plugin.configs().main()
                .getConfigurationSection("laboratory.block.sounds");
        if (sounds == null) return null;
        Object raw = sounds.get(name);
        if (raw == null) return null;
        if (raw instanceof String s) {
            if (s.isBlank()) return null;
            return new Entry(s, 1.0F, 1.0F);
        }
        ConfigurationSection sec = sounds.getConfigurationSection(name);
        if (sec == null) return null;
        String key = sec.getString("key", "");
        if (key.isBlank()) return null;
        float volume = (float) sec.getDouble("volume", 1.0);
        float pitch  = (float) sec.getDouble("pitch", 1.0);
        return new Entry(key, volume, pitch);
    }

    private record Entry(String key, float volume, float pitch) {}
}
