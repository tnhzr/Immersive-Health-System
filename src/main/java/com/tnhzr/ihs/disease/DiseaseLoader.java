package com.tnhzr.ihs.disease;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.util.TimeParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiseaseLoader {

    private final ImmersiveHealthSystem plugin;

    public DiseaseLoader(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    public Map<String, Disease> load() {
        Map<String, Disease> out = new LinkedHashMap<>();
        ConfigurationSection root = plugin.configs().infections().getConfigurationSection("infections");
        if (root == null) return out;

        for (String id : root.getKeys(false)) {
            try {
                Disease d = parseOne(id, root.getConfigurationSection(id));
                if (d != null) out.put(id, d);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load infection '" + id + "': " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + out.size() + " infections.");
        return out;
    }

    private Disease parseOne(String id, ConfigurationSection sec) {
        if (sec == null) return null;
        String name = sec.getString("name", id);
        Disease.Type type = "local".equalsIgnoreCase(sec.getString("type", "global"))
                ? Disease.Type.LOCAL : Disease.Type.GLOBAL;
        String cureType = sec.getString("cure_type", "general");
        double chance = sec.getDouble("infection_chance", 0.0);
        int tremor = sec.contains("tremor_threshold")
                ? sec.getInt("tremor_threshold")
                : Disease.TREMOR_DEFAULT;

        Map<String, TransmissionSettings> trans = new LinkedHashMap<>();
        ConfigurationSection ts = sec.getConfigurationSection("transmission");
        if (ts != null) {
            for (String tid : ts.getKeys(false)) {
                ConfigurationSection t = ts.getConfigurationSection(tid);
                if (t == null) continue;
                trans.put(tid, new TransmissionSettings(
                        t.getDouble("radius", 6.0),
                        t.getDouble("chance_radius", 0.0),
                        t.getDouble("chance_direct", 0.0)));
            }
        }

        List<Stage> stages = new ArrayList<>();
        ConfigurationSection st = sec.getConfigurationSection("stages");
        if (st != null) {
            for (String range : st.getKeys(false)) {
                ConfigurationSection s = st.getConfigurationSection(range);
                if (s == null) continue;
                int[] mm = parseRange(range);
                stages.add(new Stage(
                        range, mm[0], mm[1],
                        s.getStringList("messages"),
                        s.getStringList("titles"),
                        parseEffects(s.getStringList("effects"), Integer.MAX_VALUE),
                        parseEvents(s.getStringList("events")),
                        s.getStringList("actions")
                ));
            }
        }

        return new Disease(id, name, type, cureType, chance, tremor, trans, stages);
    }

    private int[] parseRange(String s) {
        if (s.contains("-")) {
            String[] parts = s.split("-");
            return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
        }
        int v = Integer.parseInt(s);
        return new int[]{ v, v };
    }

    public List<PotionEffect> parseEffects(List<String> raw, int defaultDurationTicks) {
        List<PotionEffect> out = new ArrayList<>();
        if (raw == null) return out;
        for (String entry : raw) {
            String[] parts = entry.split(":");
            String name = parts[0].trim();
            int amp = parts.length > 1 ? safeInt(parts[1], 0) : 0;
            PotionEffectType type = matchEffect(name);
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect: " + name);
                continue;
            }
            out.add(new PotionEffect(type, defaultDurationTicks, amp, true, false, true));
        }
        return out;
    }

    public List<StageEvent> parseEvents(List<String> raw) {
        List<StageEvent> out = new ArrayList<>();
        if (raw == null) return out;
        for (String entry : raw) {
            String[] parts = entry.split(":");
            if (parts.length == 0) continue;
            String head = parts[0];
            try {
                if (head.startsWith("effect_") && parts.length >= 4) {
                    String fx = head.substring("effect_".length());
                    int amp = safeInt(parts[1], 0);
                    long dur = TimeParser.toTicks(parts[2]);
                    long ivl = TimeParser.toTicks(parts[3]);
                    out.add(StageEvent.effect(fx, amp, dur, ivl));
                } else if (head.startsWith("sound_") && parts.length >= 2) {
                    String snd = head.substring("sound_".length());
                    long ivl = TimeParser.toTicks(parts[1]);
                    out.add(StageEvent.sound(snd, ivl));
                } else if (parts.length >= 2) {
                    long ivl = TimeParser.toTicks(parts[1]);
                    out.add(StageEvent.transmission(head, ivl));
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to parse stage event '" + entry + "': " + ex.getMessage());
            }
        }
        return out;
    }

    public static PotionEffectType matchEffect(String name) {
        if (name == null) return null;
        String n = name.toUpperCase();
        // Try registry lookup via current and legacy names.
        PotionEffectType t = PotionEffectType.getByName(n);
        if (t != null) return t;
        // Common aliases.
        return switch (n) {
            case "SLOWNESS" -> PotionEffectType.getByName("SLOW");
            case "JUMP_BOOST" -> PotionEffectType.getByName("JUMP");
            case "STRENGTH" -> PotionEffectType.getByName("INCREASE_DAMAGE");
            case "RESISTANCE" -> PotionEffectType.getByName("DAMAGE_RESISTANCE");
            case "HASTE" -> PotionEffectType.getByName("FAST_DIGGING");
            case "MINING_FATIGUE" -> PotionEffectType.getByName("SLOW_DIGGING");
            case "NAUSEA" -> PotionEffectType.getByName("CONFUSION");
            case "INSTANT_HEALTH" -> PotionEffectType.getByName("HEAL");
            case "INSTANT_DAMAGE" -> PotionEffectType.getByName("HARM");
            case "REGENERATION" -> PotionEffectType.REGENERATION;
            default -> null;
        };
    }

    private static int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
