package com.tnhzr.ihs.disease;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player runtime state. Persisted via {@link DiseaseDataStore}.
 */
public final class PlayerDiseaseState {

    private final UUID uuid;
    /** Active infections -> current scale value (1..100). */
    private final Map<String, Integer> infections = new HashMap<>();
    /** Per-tick counters for stage events (eventKey -> next-fire tick). */
    private final Map<String, Long> nextFireTick = new HashMap<>();
    /** Daily medicine usage counters: medicineId -> used today. */
    private final Map<String, Integer> medicineUsage = new HashMap<>();
    /** Whether daily growth is suppressed for the current Minecraft day. */
    private final Map<String, Boolean> growthSuppressed = new HashMap<>();
    /** Infections whose terminal-stage actions have already been fired. */
    private final java.util.Set<String> deathActionsFired = new java.util.HashSet<>();
    /** Cumulative scale-decrease from medicine since the last relief message. */
    private final Map<String, Integer> reliefProgress = new HashMap<>();
    /** Last Minecraft day (in 24000-tick cycles) the daily counters were rolled. */
    private long lastDay = -1L;

    public PlayerDiseaseState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() { return uuid; }
    public Map<String, Integer> infections() { return infections; }
    public Map<String, Long> nextFireTick() { return nextFireTick; }
    public Map<String, Integer> medicineUsage() { return medicineUsage; }
    public Map<String, Boolean> growthSuppressed() { return growthSuppressed; }
    public java.util.Set<String> deathActionsFired() { return deathActionsFired; }
    public Map<String, Integer> reliefProgress() { return reliefProgress; }
    public long lastDay() { return lastDay; }
    public void setLastDay(long day) { this.lastDay = day; }

    public boolean hasInfection(String id) { return infections.containsKey(id); }
    public int scale(String id) { return infections.getOrDefault(id, 0); }

    public void setScale(String id, int value) {
        if (value <= 0) {
            infections.remove(id);
            deathActionsFired.remove(id);
            reliefProgress.remove(id);
        } else {
            infections.put(id, Math.min(100, value));
            if (value < 100) deathActionsFired.remove(id);
        }
    }

    public void clearAll() {
        infections.clear();
        nextFireTick.clear();
        growthSuppressed.clear();
        deathActionsFired.clear();
        reliefProgress.clear();
    }

    public void rolloverDayIfNeeded(long currentDay) {
        if (lastDay != currentDay) {
            medicineUsage.clear();
            growthSuppressed.clear();
            lastDay = currentDay;
        }
    }

    public void load(ConfigurationSection sec) {
        infections.clear();
        ConfigurationSection inf = sec.getConfigurationSection("infections");
        if (inf != null) for (String k : inf.getKeys(false)) infections.put(k, inf.getInt(k));
        ConfigurationSection use = sec.getConfigurationSection("medicine_usage");
        if (use != null) for (String k : use.getKeys(false)) medicineUsage.put(k, use.getInt(k));
        lastDay = sec.getLong("last_day", -1L);
    }

    public void save(ConfigurationSection sec) {
        sec.set("infections", null);
        for (Map.Entry<String, Integer> e : infections.entrySet()) {
            sec.set("infections." + e.getKey(), e.getValue());
        }
        sec.set("medicine_usage", null);
        for (Map.Entry<String, Integer> e : medicineUsage.entrySet()) {
            sec.set("medicine_usage." + e.getKey(), e.getValue());
        }
        sec.set("last_day", lastDay);
    }
}
