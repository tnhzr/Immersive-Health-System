package com.tnhzr.ihs.disease;

import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

public final class Stage {

    private final String key;
    private final int min;
    private final int max;
    private final List<String> messages;
    private final List<String> titles;
    private final List<PotionEffect> effects;
    private final List<StageEvent> events;
    private final List<String> actions;

    public Stage(String key, int min, int max,
                 List<String> messages, List<String> titles,
                 List<PotionEffect> effects, List<StageEvent> events,
                 List<String> actions) {
        this.key = key;
        this.min = min;
        this.max = max;
        this.messages = messages == null ? Collections.emptyList() : messages;
        this.titles   = titles   == null ? Collections.emptyList() : titles;
        this.effects  = effects  == null ? Collections.emptyList() : effects;
        this.events   = events   == null ? Collections.emptyList() : events;
        this.actions  = actions  == null ? Collections.emptyList() : actions;
    }

    public boolean contains(int scale) {
        return scale >= min && scale <= max;
    }

    public String key() { return key; }
    public int min() { return min; }
    public int max() { return max; }
    public List<String> messages() { return messages; }
    public List<String> titles() { return titles; }
    public List<PotionEffect> effects() { return effects; }
    public List<StageEvent> events() { return events; }
    public List<String> actions() { return actions; }
}
