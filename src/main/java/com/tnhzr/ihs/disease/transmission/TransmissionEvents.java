package com.tnhzr.ihs.disease.transmission;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.disease.Disease;
import com.tnhzr.ihs.disease.TransmissionSettings;
import com.tnhzr.ihs.disease.symptom.SymptomConfig;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fires symptom events from infected players: cough, sneeze, spit,
 * vomit, cough_blood. Particles and sounds are read from
 * {@code symptoms.yml}; per-player sound selection (vanilla vs. custom
 * {@code ihs:*}) is delegated to {@link ImmersiveHealthSystem#resourcePacks()}.
 */
public final class TransmissionEvents {

    private final ImmersiveHealthSystem plugin;
    private final Map<String, Handler> handlers = new HashMap<>();

    public TransmissionEvents(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
        register("cough", this::cough);
        register("sneeze", this::sneeze);
        register("vomit", this::vomit);
        register("cough_blood", this::coughBlood);
    }

    public void register(String id, Handler h) { handlers.put(id, h); }

    public void fire(Player source, String id, Disease disease) {
        Handler h = handlers.get(id);
        TransmissionSettings ts = disease.transmissions().get(id);
        if (ts == null) {
            // Some symptom events have no transmission entry (e.g. brain cancer's effect events).
            ts = new TransmissionSettings(0.0, 0.0, 0.0);
        }
        if (h != null) h.fire(source, disease, ts);
    }

    public void manualSpit(Player source) {
        TransmissionSettings ts = new TransmissionSettings(
                plugin.configs().main().getDouble("disease.spit.range_blocks", 6.0),
                0.0,
                100.0);
        var state = plugin.diseases().state(source.getUniqueId());
        // Always play visuals/sound; only transmit when infected.
        SymptomConfig.EventSettings es = plugin.symptoms().event("spit");
        spawnParticles(source, es.particle());
        playConfigured("spit", source);
        if (state.infections().isEmpty()) return;

        String firstId = state.infections().keySet().iterator().next();
        Disease d = plugin.diseases().disease(firstId);
        Player target = rayTracePlayer(source, ts.radius);
        double bonus = plugin.configs().main()
                .getDouble("disease.spit.direct_chance_bonus_percent", 75.0);
        if (target != null && d != null) {
            plugin.diseases().tryInfect(target, d, bonus);
        }
    }

    // --- Symptom implementations -------------------------------------------------

    private void cough(Player source, Disease disease, TransmissionSettings ts) {
        SymptomConfig.EventSettings es = plugin.symptoms().event("cough");
        playConfigured("cough", source);
        spawnParticles(source, es.particle());
        applyTransmission(source, disease, ts);
    }

    private void sneeze(Player source, Disease disease, TransmissionSettings ts) {
        SymptomConfig.EventSettings es = plugin.symptoms().event("sneeze");
        playConfigured("sneeze", source);
        spawnParticles(source, es.particle());
        applyTransmission(source, disease, ts);
    }

    private void coughBlood(Player source, Disease disease, TransmissionSettings ts) {
        SymptomConfig.EventSettings es = plugin.symptoms().event("cough_blood");
        playConfigured("cough_blood", source);
        spawnParticles(source, es.particle());
        applyTransmission(source, disease, ts);
    }

    private void vomit(Player source, Disease disease, TransmissionSettings ts) {
        SymptomConfig.EventSettings es = plugin.symptoms().event("vomit");
        playConfigured("vomit", source);
        spawnParticles(source, es.particle());
        BlockData data = Material.SLIME_BLOCK.createBlockData();
        Location head = source.getEyeLocation();
        int lifetimeTicks = resolveVomitLifetimeTicks(es);
        for (int i = 0; i < 4; i++) {
            FallingBlock fb = source.getWorld().spawnFallingBlock(head, data);
            fb.setDropItem(false);
            fb.setHurtEntities(false);
            Vector v = source.getLocation().getDirection()
                    .multiply(0.6).add(new Vector(
                            (Math.random() - 0.5) * 0.2,
                            0.2 + Math.random() * 0.1,
                            (Math.random() - 0.5) * 0.2));
            fb.setVelocity(v);
            new BukkitRunnable() {
                @Override public void run() { if (fb.isValid()) fb.remove(); }
            }.runTaskLater(plugin, lifetimeTicks);
        }
        applyTransmission(source, disease, ts);
    }

    private int resolveVomitLifetimeTicks(SymptomConfig.EventSettings es) {
        // config.yml override retains priority for backwards compatibility.
        int fromMain = plugin.configs().main()
                .getInt("disease.vomit_block_lifetime_seconds", -1);
        int seconds = fromMain > 0 ? fromMain : Math.max(1, es.blockLifetimeSeconds());
        return seconds * 20;
    }

    // --- Particle spawner -------------------------------------------------------

    private void spawnParticles(Player source, SymptomConfig.ParticleSettings ps) {
        if (ps.type() == null || ps.count() <= 0) return;
        Location head = source.getEyeLocation();
        Vector look = source.getLocation().getDirection();
        Object data = resolveParticleData(ps);
        int steps = Math.max(1, ps.coneSteps());
        if (ps.coneSteps() <= 0) {
            // Single-point spawn at forward_offset.
            Location at = head.clone().add(look.clone().multiply(ps.forwardOffset()));
            emit(source, at, ps, data);
            return;
        }
        for (int i = 0; i < steps; i++) {
            Location at = head.clone().add(look.clone()
                    .multiply(ps.forwardOffset() + i * ps.coneStepLength()));
            emit(source, at, ps, data);
        }
    }

    private void emit(Player source, Location at,
                      SymptomConfig.ParticleSettings ps, Object data) {
        if (data == null) {
            source.getWorld().spawnParticle(ps.type(), at, ps.count(),
                    ps.offsetX(), ps.offsetY(), ps.offsetZ(), ps.speed());
        } else {
            source.getWorld().spawnParticle(ps.type(), at, ps.count(),
                    ps.offsetX(), ps.offsetY(), ps.offsetZ(), ps.speed(), data);
        }
    }

    private Object resolveParticleData(SymptomConfig.ParticleSettings ps) {
        if (ps.type() == Particle.DUST) {
            List<Integer> c = ps.dustColor();
            int r = c.size() > 0 ? c.get(0) : 255;
            int g = c.size() > 1 ? c.get(1) : 0;
            int b = c.size() > 2 ? c.get(2) : 0;
            return new Particle.DustOptions(Color.fromRGB(
                    clampByte(r), clampByte(g), clampByte(b)),
                    (float) ps.dustSize());
        }
        return null;
    }

    private static int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // --- Transmission probability ----------------------------------------------

    private void applyTransmission(Player source, Disease disease, TransmissionSettings ts) {
        if (ts.radius <= 0 || (ts.chanceRadius <= 0 && ts.chanceDirect <= 0)) return;
        double directBonus = plugin.configs().main()
                .getDouble("disease.direct_hit_bonus_percent", 50.0);
        double directHitRadius = plugin.configs().main()
                .getDouble("disease.particle_hit_radius", 0.6);
        Location eye = source.getEyeLocation();
        Vector dir = source.getLocation().getDirection();

        for (Player other : source.getWorld().getPlayers()) {
            if (other.equals(source)) continue;
            if (other.getLocation().distance(source.getLocation()) > ts.radius) continue;
            if (plugin.diseases().state(other.getUniqueId()).hasInfection(disease.id())) continue;

            boolean directHit = false;
            Vector toTarget = other.getEyeLocation().toVector().subtract(eye.toVector());
            double along = toTarget.dot(dir);
            if (along > 0 && along <= ts.radius) {
                Vector projection = dir.clone().multiply(along);
                double perp = toTarget.clone().subtract(projection).length();
                if (perp <= directHitRadius) directHit = true;
            }

            double chance = directHit
                    ? Math.min(100.0, ts.chanceDirect + directBonus)
                    : ts.chanceRadius;
            if (chance <= 0) continue;
            plugin.diseases().tryInfect(other, disease, chance);
        }
    }

    private Player rayTracePlayer(Player source, double range) {
        RayTraceResult res = source.getWorld().rayTrace(
                source.getEyeLocation(),
                source.getLocation().getDirection(),
                range,
                FluidCollisionMode.NEVER,
                true,
                0.3,
                e -> e instanceof Player && !e.equals(source));
        if (res != null && res.getHitEntity() instanceof Player p) return p;
        return null;
    }

    /**
     * Plays the symptom sound for the given event. For every player within
     * earshot of {@code source} (including the source itself) we pick either
     * the {@code sound.custom} or {@code sound.vanilla} key based on whether
     * that player's client has the IHS resourcepack loaded. Falling back to
     * the vanilla key when {@code ihs:} keys are unavailable avoids the
     * "missing_sound" spam in client logs.
     */
    private void playConfigured(String event, Player source) {
        SymptomConfig.SoundSettings snd = plugin.symptoms().event(event).sound();
        String vanilla = snd.vanillaKey();
        String custom  = snd.customKey();
        float volume = snd.volume();
        float pitch  = snd.pitch();
        // Attenuation radius in vanilla is volume * 16 blocks. That's enough
        // for any reasonable symptom event. We iterate players in the same
        // world so each gets the key resolved to their client state.
        for (Player listener : source.getWorld().getPlayers()) {
            boolean hasPack = custom != null && !custom.isBlank()
                    && plugin.resourcePacks().hasPack(listener);
            String key = hasPack ? custom : vanilla;
            if (key == null || key.isBlank()) continue;
            playForListener(listener, source.getLocation(), key, volume, pitch);
        }
    }

    private void playForListener(Player listener, Location at,
                                 String key, float volume, float pitch) {
        try {
            listener.playSound(at, key, SoundCategory.PLAYERS, volume, pitch);
            return;
        } catch (Throwable ignored) {
            // Fall through to enum lookup.
        }
        try {
            Sound s = Sound.valueOf(key.toUpperCase()
                    .replace('.', '_').replace(':', '_'));
            listener.playSound(at, s, SoundCategory.PLAYERS, volume, pitch);
        } catch (Throwable ignored) {
            // Last resort: ignore — we never want a bad sound config to
            // crash the symptom event.
        }
    }

    @FunctionalInterface
    public interface Handler {
        void fire(Player source, Disease disease, TransmissionSettings ts);
    }
}
