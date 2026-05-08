package com.tnhzr.ihs.disease;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.disease.commands.IhsCommand;
import com.tnhzr.ihs.disease.commands.SpitCommand;
import com.tnhzr.ihs.disease.commands.SymptomCommand;
import com.tnhzr.ihs.disease.listeners.PlayerLifecycleListener;
import com.tnhzr.ihs.disease.transmission.TransmissionEvents;
import com.tnhzr.ihs.module.Module;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DiseaseManager implements Module {

    private final ImmersiveHealthSystem plugin;
    private final DiseaseDataStore store;
    private final DiseaseLoader loader;
    private final TransmissionEvents transmissionEvents;
    private final Map<UUID, PlayerDiseaseState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Long> spitCooldown = new ConcurrentHashMap<>();
    /** Latest disease id that triggered a stage "kill" action for a player.
     *  Read by the death listener so we can swap the vanilla death message
     *  for a disease-specific one (e.g. "Tannhauser умер от туберкулёза"). */
    private final Map<UUID, String> lastKillingDisease = new ConcurrentHashMap<>();
    /** Players currently being shown the vanilla freezing visual as a
     *  tremor symptom. Tracked so we can clear {@link Player#setFreezeTicks(int)}
     *  the moment the symptom stops or the player quits. */
    private final Set<UUID> tremorActive = ConcurrentHashMap.newKeySet();
    /** Forced-tremor expirations (server tick deadline) keyed by UUID.
     *  Set by the {@code /ihs tremor} debug command so admins can verify
     *  the visual without needing a real disease scale > threshold. */
    private final Map<UUID, Long> tremorOverride = new ConcurrentHashMap<>();
    private Map<String, Disease> diseases = new HashMap<>();
    private BukkitTask tickTask;
    private BukkitTask tremorTask;
    /** Cached default tremor threshold from config; per-disease overrides win. */
    private int defaultTremorThreshold;

    public DiseaseManager(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
        this.store = new DiseaseDataStore(plugin);
        this.loader = new DiseaseLoader(plugin);
        this.transmissionEvents = new TransmissionEvents(plugin);
    }

    @Override public String id() { return "disease"; }

    @Override
    public void enable() {
        this.diseases = loader.load();
        this.defaultTremorThreshold = plugin.configs().main()
                .getInt("disease.tremor_threshold", 50);

        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(
                new com.tnhzr.ihs.disease.listeners.DiseaseDeathListener(plugin), plugin);

        IhsCommand exec = new IhsCommand(plugin);
        if (plugin.getCommand("ihs") != null) {
            plugin.getCommand("ihs").setExecutor(exec);
            plugin.getCommand("ihs").setTabCompleter(exec);
        }
        SpitCommand spit = new SpitCommand(this);
        if (plugin.getCommand("spit") != null) {
            plugin.getCommand("spit").setExecutor(spit);
        }
        SymptomCommand symptom = new SymptomCommand(plugin);
        for (String name : new String[]{ "cough", "sneeze", "vomit" }) {
            if (plugin.getCommand(name) != null) {
                plugin.getCommand(name).setExecutor(symptom);
                plugin.getCommand(name).setTabCompleter(symptom);
            }
        }

        // Load any currently online players (e.g. /reload).
        for (Player p : Bukkit.getOnlinePlayers()) loadPlayer(p.getUniqueId());

        startTicker();
        startTremorTicker();
    }

    @Override
    public void disable() {
        if (tickTask != null) tickTask.cancel();
        if (tremorTask != null) tremorTask.cancel();
        // Clear the freezing visual on every player so a server reload
        // doesn't leave a frozen-screen overlay stuck on anyone's client.
        for (Player online : Bukkit.getOnlinePlayers()) revertTremor(online);
        tremorActive.clear();
        for (UUID id : states.keySet()) saveAndForget(id);
    }

    /**
     * Re-parses the disease catalog from disk. Active player states keep
     * their numeric scale; any infections referencing diseases that have
     * been removed by the config edit are dropped automatically the next
     * time they're inspected.
     */
    public void reload() {
        this.diseases = loader.load();
        this.defaultTremorThreshold = plugin.configs().main()
                .getInt("disease.tremor_threshold", 50);
    }

    public ImmersiveHealthSystem plugin() { return plugin; }
    public Map<String, Disease> diseases() { return diseases; }
    public Disease disease(String id) { return diseases.get(id); }
    public TransmissionEvents transmissionEvents() { return transmissionEvents; }

    public PlayerDiseaseState state(UUID id) {
        return states.computeIfAbsent(id, store::load);
    }

    public void loadPlayer(UUID id) {
        states.computeIfAbsent(id, store::load);
    }

    public void saveAndForget(UUID id) {
        PlayerDiseaseState s = states.remove(id);
        if (s != null) store.save(s);
    }

    public void resetOnDeath(UUID id) {
        if (!plugin.configs().main().getBoolean("disease.reset_on_death", true)) return;
        PlayerDiseaseState state = state(id);
        state.clearAll();
    }

    private void startTicker() {
        long checkPeriod = plugin.configs().main()
                .getLong("disease.global_infection_check_period_ticks", 24000L);
        // We tick every second; per-player day rollover and per-event timers run from this loop.
        tickTask = new BukkitRunnable() {
            long ticks = 0L;
            @Override
            public void run() {
                ticks += 20L;
                long worldTicks = ticks; // logical clock
                tickPlayers(worldTicks, checkPeriod);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Player tremor: every tick, scan online players whose worst infection
     * scale exceeds the (per-disease or global) tremor threshold and emit
     * a small particle burst around them as a purely cosmetic shaking
     * effect.
     *
     * <p>This implementation deliberately avoids {@code setFreezeTicks}:
     * the vanilla freezing state pulls in the icy-vignette overlay and
     * the slowdown applied while {@code isFullyFrozen()}, both of which
     * we want gone. Particles are 100% client-side render with zero
     * gameplay side effects — the affected player is otherwise
     * indistinguishable from a healthy one mechanically.</p>
     */
    private void startTremorTicker() {
        tremorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!shouldTremorAnyone()) return;
                for (Player p : Bukkit.getOnlinePlayers()) applyTremor(p);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean shouldTremorAnyone() {
        return !states.isEmpty() || !tremorActive.isEmpty() || !tremorOverride.isEmpty();
    }

    private void applyTremor(Player p) {
        boolean shake = false;
        // Debug override from /ihs tremor — wins over real disease scale.
        Long until = tremorOverride.get(p.getUniqueId());
        long now = Bukkit.getCurrentTick();
        if (until != null) {
            if (now >= until) {
                tremorOverride.remove(p.getUniqueId());
            } else {
                shake = true;
            }
        }
        if (!shake) {
            PlayerDiseaseState state = states.get(p.getUniqueId());
            if (state == null || state.infections().isEmpty()) {
                revertTremor(p);
                return;
            }
            for (Map.Entry<String, Integer> e : state.infections().entrySet()) {
                Disease d = disease(e.getKey());
                if (d == null) continue;
                int threshold = d.tremorThreshold() == Disease.TREMOR_DEFAULT
                        ? defaultTremorThreshold
                        : d.tremorThreshold();
                if (threshold < 0 || threshold > 100) continue;
                if (e.getValue() > threshold) { shake = true; break; }
            }
        }
        if (!shake) {
            revertTremor(p);
            return;
        }
        // Pure-visual tremor. Spawn a tiny WHITE_ASH burst around the
        // player's torso every tick — visible to everyone, no movement
        // slowdown, no vignette, no damage. We deliberately do NOT touch
        // setFreezeTicks here so vanilla powder-snow mechanics keep
        // working normally for everyone (including the diseased player).
        Location at = p.getLocation().add(0.0, 1.0, 0.0);
        try {
            p.getWorld().spawnParticle(Particle.WHITE_ASH, at,
                    4, 0.35, 0.6, 0.35, 0.0);
        } catch (Throwable ignored) {
            // Older clients may not have WHITE_ASH — fall back to SMOKE.
            try {
                p.getWorld().spawnParticle(Particle.SMOKE, at,
                        2, 0.25, 0.4, 0.25, 0.0);
            } catch (Throwable ignored2) { /* give up silently */ }
        }
        tremorActive.add(p.getUniqueId());
    }

    /**
     * Force the tremor visual on a player for {@code seconds} regardless
     * of their actual disease state. Used by the {@code /ihs tremor}
     * debug command so admins can verify the visual quickly.
     *
     * @param seconds duration in seconds; 0 clears any existing override.
     */
    public void forceTremor(Player p, int seconds) {
        if (seconds <= 0) {
            tremorOverride.remove(p.getUniqueId());
            return;
        }
        long deadline = Bukkit.getCurrentTick() + (long) seconds * 20L;
        tremorOverride.put(p.getUniqueId(), deadline);
    }

    /** Whether the given player has a debug tremor override active. */
    public boolean hasForcedTremor(java.util.UUID id) {
        Long until = tremorOverride.get(id);
        return until != null && until > Bukkit.getCurrentTick();
    }

    /**
     * Clears any tremor flag for the given player. The current
     * implementation only emits transient particles, so there's no
     * client-side state to undo — we just drop the bookkeeping entry.
     * Kept public for the lifecycle listener and for forwards-compat
     * with future visual approaches that may need cleanup.
     */
    public void revertTremor(Player p) {
        if (!tremorActive.remove(p.getUniqueId())) return;
        // Defensive: legacy installs may have left freezeTicks pinned
        // at the cap from the previous freeze-based implementation.
        // Reset once so upgraded servers don't see lingering vignettes.
        try {
            if (p.getFreezeTicks() > 0) p.setFreezeTicks(0);
        } catch (Throwable ignored) { /* older API — no-op */ }
    }

    /**
     * Records that {@code points} of disease {@code diseaseId} were healed
     * by medicine. Whenever cumulative healing crosses
     * {@code disease.relief_message_step}, the player is told they're
     * feeling better. Counter resets when the infection ends.
     */
    public void recordReliefProgress(Player target, String diseaseId, int points) {
        if (target == null || diseaseId == null || points <= 0) return;
        PlayerDiseaseState state = state(target.getUniqueId());
        int step = plugin.configs().main().getInt("disease.relief_message_step", 10);
        if (step <= 0) return;
        int cumulative = state.reliefProgress().getOrDefault(diseaseId, 0) + points;
        while (cumulative >= step) {
            cumulative -= step;
            Disease d = disease(diseaseId);
            String name = d != null ? d.name() : diseaseId;
            plugin.locale().send(target, "medicine.relief", java.util.Map.of("infection", name));
        }
        state.reliefProgress().put(diseaseId, cumulative);
    }

    private void tickPlayers(long worldTicks, long globalCheckPeriod) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            tickOne(p, worldTicks, globalCheckPeriod);
        }
    }

    private void tickOne(Player p, long worldTicks, long globalCheckPeriod) {
        PlayerDiseaseState state = state(p.getUniqueId());
        long currentDay = p.getWorld().getFullTime() / 24000L;
        boolean newDay = state.lastDay() != currentDay;
        state.rolloverDayIfNeeded(currentDay);

        if (newDay) {
            applyDailyGrowth(p, state);
            rollGlobalInfection(p, state);
        }

        applyStageEffects(p, state);
        runStageEvents(p, state, worldTicks);
        checkDeaths(p, state);
    }

    public void applyDailyGrowth(Player p, PlayerDiseaseState state) {
        double base = plugin.configs().main().getDouble("disease.base_growth_per_day", 1.0);

        double extra = 0;
        double hpThreshold = plugin.configs().main().getDouble("disease.growth_modifiers.low_health.threshold", 0.5);
        double hpAdd = plugin.configs().main().getDouble("disease.growth_modifiers.low_health.add", 0.5);
        double maxHealth = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                : 20.0;
        if (maxHealth > 0 && p.getHealth() / maxHealth < hpThreshold) extra += hpAdd;

        int hungerThreshold = plugin.configs().main().getInt("disease.growth_modifiers.low_hunger.threshold", 6);
        double hungerAdd = plugin.configs().main().getDouble("disease.growth_modifiers.low_hunger.add", 0.5);
        if (p.getFoodLevel() <= hungerThreshold) extra += hungerAdd;

        double total = base + extra;

        for (Map.Entry<String, Integer> e : new HashMap<>(state.infections()).entrySet()) {
            String id = e.getKey();
            if (Boolean.TRUE.equals(state.growthSuppressed().get(id))) continue;
            int newScale = e.getValue() + (int) Math.ceil(total);
            state.setScale(id, Math.min(100, newScale));
        }
    }

    public void rollGlobalInfection(Player p, PlayerDiseaseState state) {
        if (!state.infections().isEmpty()) return; // Already sick.
        for (Disease d : diseases.values()) {
            if (d.type() != Disease.Type.GLOBAL) continue;
            if (d.infectionChance() <= 0) continue;
            if (Math.random() * 100.0 < d.infectionChance()) {
                infect(p, d.id(), 1);
                return;
            }
        }
    }

    public void applyStageEffects(Player p, PlayerDiseaseState state) {
        for (Map.Entry<String, Integer> e : state.infections().entrySet()) {
            Disease d = disease(e.getKey());
            if (d == null) continue;
            Stage st = d.stageFor(e.getValue());
            if (st == null) continue;
            for (PotionEffect base : st.effects()) {
                p.addPotionEffect(new PotionEffect(base.getType(), 60, base.getAmplifier(), true, false, true), true);
            }
        }
    }

    public void runStageEvents(Player p, PlayerDiseaseState state, long now) {
        for (Map.Entry<String, Integer> e : new HashMap<>(state.infections()).entrySet()) {
            Disease d = disease(e.getKey());
            if (d == null) continue;
            Stage st = d.stageFor(e.getValue());
            if (st == null) continue;

            // Stage transition message (fire once per stage).
            String stageKey = e.getKey() + ":lastStage";
            String prev = state.growthSuppressed().getOrDefault(stageKey, false) ? "" : "";
            // Send periodic message every minute while in stage.
            String msgKey = e.getKey() + ":msg";
            long nextMsg = state.nextFireTick().getOrDefault(msgKey, 0L);
            if (now >= nextMsg) {
                String prefix = plugin.locale().raw("prefix");
                for (String line : st.messages()) {
                    p.sendMessage(Text.component(prefix + line));
                }
                for (String t : st.titles()) p.showTitle(net.kyori.adventure.title.Title.title(
                        Text.component(t), Component.empty()));
                state.nextFireTick().put(msgKey, now + 60L * 20L); // every minute
            }

            for (StageEvent ev : st.events()) {
                String key = e.getKey() + ":" + ev.kind() + ":" + ev.name();
                long next = state.nextFireTick().getOrDefault(key, 0L);
                if (now < next) continue;
                state.nextFireTick().put(key, now + Math.max(20L, ev.intervalTicks()));

                switch (ev.kind()) {
                    case TRANSMISSION -> transmissionEvents.fire(p, ev.name(), d);
                    case EFFECT_PULSE -> {
                        var type = DiseaseLoader.matchEffect(ev.name().toUpperCase());
                        if (type != null) {
                            p.addPotionEffect(new PotionEffect(
                                    type, (int) Math.max(1L, ev.durationTicks()),
                                    ev.amplifier(), true, false, true), true);
                        }
                    }
                    case SOUND_PULSE -> {
                        try {
                            org.bukkit.Sound s = org.bukkit.Sound.valueOf(
                                    ev.name().toUpperCase().replace('.', '_'));
                            p.playSound(p.getLocation(), s, 1.0F, 1.0F);
                        } catch (IllegalArgumentException ex) {
                            p.getWorld().playSound(p.getLocation(),
                                    ev.name().toLowerCase(), 1.0F, 1.0F);
                        }
                    }
                }
            }
        }
    }

    public void checkDeaths(Player p, PlayerDiseaseState state) {
        for (Map.Entry<String, Integer> e : new HashMap<>(state.infections()).entrySet()) {
            if (e.getValue() < 100) continue;
            // Death actions for this infection have already fired — don't repeat them
            // every tick while the player is dead/respawning.
            if (state.deathActionsFired().contains(e.getKey())) continue;
            Disease d = disease(e.getKey());
            if (d == null) continue;
            Stage death = d.stageFor(100);
            if (death != null) {
                for (String t : death.titles()) p.showTitle(net.kyori.adventure.title.Title.title(
                        Text.component(t), Component.empty()));
                executeActions(p, death.actions(), e.getKey());
            }
            state.deathActionsFired().add(e.getKey());
        }
    }

    private void executeActions(Player p, List<String> actions, String diseaseId) {
        if (actions == null) return;
        for (String action : actions) {
            switch (action.toLowerCase()) {
                case "kill" -> {
                    // Remember which disease pushed this player into death
                    // so the death listener can pick the configured
                    // disease.death_message (if any) for chat broadcast.
                    if (diseaseId != null) {
                        lastKillingDisease.put(p.getUniqueId(), diseaseId);
                    }
                    p.setHealth(0.0);
                }
                case "spawn_zombie" -> {
                    p.getWorld().spawnEntity(p.getLocation(), org.bukkit.entity.EntityType.ZOMBIE);
                }
                default -> plugin.getLogger().warning("Unknown stage action: " + action);
            }
        }
    }

    /** Removes (and returns) the disease that last triggered a kill action
     *  for the given player. Used by the death listener exactly once per
     *  death event — returning to {@code null} on subsequent calls. */
    public String consumeLastKillingDisease(UUID id) {
        return lastKillingDisease.remove(id);
    }

    public boolean tryInfect(Player target, Disease disease, double chancePercent) {
        if (target == null || disease == null) return false;
        if (Math.random() * 100.0 >= chancePercent) return false;
        infect(target, disease.id(), 1);
        return true;
    }

    public void infect(Player target, String diseaseId, int initialScale) {
        Disease d = disease(diseaseId);
        if (d == null) return;
        PlayerDiseaseState st = state(target.getUniqueId());
        st.setScale(diseaseId, Math.max(1, initialScale));
    }

    public void heal(Player target, String diseaseId) {
        PlayerDiseaseState st = state(target.getUniqueId());
        if (diseaseId == null) {
            st.clearAll();
            // Full reset wipes any lingering disease-applied effects too.
            for (PotionEffect e : new java.util.ArrayList<>(target.getActivePotionEffects())) {
                target.removePotionEffect(e.getType());
            }
        } else {
            st.setScale(diseaseId, 0);
            // Don't strip unrelated potion effects (other diseases, beacons,
            // buff medicines). The disease's own per-stage effects stop being
            // re-applied automatically once the infection is gone.
        }
    }

    public void modifyScale(Player target, int value) {
        PlayerDiseaseState st = state(target.getUniqueId());
        if (st.infections().isEmpty()) return;
        // Apply to all currently active infections.
        for (String id : new java.util.ArrayList<>(st.infections().keySet())) {
            st.setScale(id, value);
        }
    }

    public Map<UUID, Long> spitCooldown() { return spitCooldown; }

    /**
     * Whether the tremor symptom is currently driving the vanilla
     * freezing visual on the given player. Used by the lifecycle
     * listener to suppress {@code DamageCause.FREEZE} so the symptom
     * stays purely cosmetic.
     */
    public boolean isTremorActive(UUID id) { return tremorActive.contains(id); }
}
