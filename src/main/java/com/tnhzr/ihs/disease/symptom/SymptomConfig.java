package com.tnhzr.ihs.disease.symptom;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and resolves per-event symptom settings from {@code symptoms.yml}.
 * Every call goes through a cached {@link FileConfiguration} so runtime
 * lookups are O(1) after the initial load.
 */
public final class SymptomConfig {

    private final ImmersiveHealthSystem plugin;
    private FileConfiguration file;
    private final Map<String, EventSettings> cache = new LinkedHashMap<>();
    private boolean resourcePackDetectionEnabled = true;
    private long resourcePackGracePeriodMs = 3000L;

    public SymptomConfig(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File target = new File(plugin.getDataFolder(), "symptoms.yml");
        if (!target.exists()) {
            plugin.saveResource("symptoms.yml", false);
        }
        this.file = YamlConfiguration.loadConfiguration(target);
        // Overlay bundled defaults so newly-added keys resolve without
        // forcing the user to regenerate the file.
        InputStream defaults = plugin.getResource("symptoms.yml");
        if (defaults != null) {
            this.file.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
        this.cache.clear();
        this.resourcePackDetectionEnabled = file.getBoolean(
                "resource_pack_detection.enabled", true);
        this.resourcePackGracePeriodMs = file.getLong(
                "resource_pack_detection.grace_period_ms", 3000L);
    }

    public boolean resourcePackDetectionEnabled() {
        return resourcePackDetectionEnabled;
    }

    public long resourcePackGracePeriodMs() {
        return resourcePackGracePeriodMs;
    }

    public EventSettings event(String id) {
        EventSettings cached = cache.get(id);
        if (cached != null) return cached;
        ConfigurationSection sec = file == null
                ? null
                : file.getConfigurationSection("events." + id);
        EventSettings es = new EventSettings(sec);
        cache.put(id, es);
        return es;
    }

    /** Per-event settings container. All methods tolerate a {@code null} section. */
    public static final class EventSettings {
        private final ParticleSettings particle;
        private final SoundSettings sound;
        private final int blockLifetimeSeconds;

        EventSettings(ConfigurationSection sec) {
            ConfigurationSection p = sec == null ? null : sec.getConfigurationSection("particle");
            ConfigurationSection s = sec == null ? null : sec.getConfigurationSection("sound");
            this.particle = new ParticleSettings(p);
            this.sound = new SoundSettings(s);
            this.blockLifetimeSeconds = sec == null
                    ? 3
                    : sec.getInt("block_lifetime_seconds", 3);
        }

        public ParticleSettings particle() { return particle; }
        public SoundSettings sound() { return sound; }
        public int blockLifetimeSeconds() { return blockLifetimeSeconds; }
    }

    public static final class ParticleSettings {
        private final Particle type;
        private final int count;
        private final double offsetX, offsetY, offsetZ;
        private final double speed;
        private final double forwardOffset;
        private final int coneSteps;
        private final double coneStepLength;
        private final List<Integer> dustColor;
        private final double dustSize;

        ParticleSettings(ConfigurationSection sec) {
            if (sec == null) {
                this.type = null;
                this.count = 0;
                this.offsetX = this.offsetY = this.offsetZ = 0;
                this.speed = 0;
                this.forwardOffset = 0;
                this.coneSteps = 0;
                this.coneStepLength = 0;
                this.dustColor = Collections.emptyList();
                this.dustSize = 1.0;
                return;
            }
            String typeName = sec.getString("type", "");
            Particle resolved = null;
            if (typeName != null && !typeName.isBlank()) {
                try {
                    resolved = Particle.valueOf(typeName.toUpperCase());
                } catch (IllegalArgumentException ignored) { /* unknown — disable */ }
            }
            this.type = resolved;
            this.count = sec.getInt("count", 0);
            this.offsetX = sec.getDouble("offset_x", 0);
            this.offsetY = sec.getDouble("offset_y", 0);
            this.offsetZ = sec.getDouble("offset_z", 0);
            this.speed = sec.getDouble("speed", 0);
            this.forwardOffset = sec.getDouble("forward_offset", 0);
            this.coneSteps = sec.getInt("cone_steps", 0);
            this.coneStepLength = sec.getDouble("cone_step_length", 0);
            this.dustColor = sec.getIntegerList("dust_color");
            this.dustSize = sec.getDouble("dust_size", 1.0);
        }

        public Particle type() { return type; }
        public int count() { return count; }
        public double offsetX() { return offsetX; }
        public double offsetY() { return offsetY; }
        public double offsetZ() { return offsetZ; }
        public double speed() { return speed; }
        public double forwardOffset() { return forwardOffset; }
        public int coneSteps() { return coneSteps; }
        public double coneStepLength() { return coneStepLength; }
        public List<Integer> dustColor() { return dustColor; }
        public double dustSize() { return dustSize; }
    }

    public static final class SoundSettings {
        private final String vanillaKey;
        private final String customKey;
        private final float volume;
        private final float pitch;

        SoundSettings(ConfigurationSection sec) {
            if (sec == null) {
                this.vanillaKey = null;
                this.customKey = null;
                this.volume = 1.0F;
                this.pitch = 1.0F;
                return;
            }
            this.vanillaKey = sec.getString("vanilla");
            this.customKey = sec.getString("custom");
            this.volume = (float) sec.getDouble("volume", 1.0);
            this.pitch  = (float) sec.getDouble("pitch",  1.0);
        }

        public String vanillaKey() { return vanillaKey; }
        public String customKey()  { return customKey; }
        public float volume()      { return volume; }
        public float pitch()       { return pitch; }
    }
}
