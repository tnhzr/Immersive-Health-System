package com.tnhzr.ihs.disease;

/**
 * Parsed entry from a stage's "events" list. Supported formats:
 *  - "cough:10m"
 *  - "sneeze:6m"
 *  - "vomit:30m"
 *  - "cough_blood:5m"
 *  - "effect_blindness:0:10s:5m"  (effect:amplifier:duration:interval)
 *  - "effect_nausea:0:10s:5m"
 *  - "sound_entity.warden.heartbeat:1m"
 */
public final class StageEvent {

    public enum Kind { TRANSMISSION, EFFECT_PULSE, SOUND_PULSE }

    private final Kind kind;
    private final String name;       // transmission id / effect id / sound id
    private final int amplifier;     // for EFFECT_PULSE
    private final long durationTicks; // for EFFECT_PULSE
    private final long intervalTicks;

    private StageEvent(Kind kind, String name, int amplifier, long durationTicks, long intervalTicks) {
        this.kind = kind;
        this.name = name;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.intervalTicks = intervalTicks;
    }

    public static StageEvent transmission(String id, long intervalTicks) {
        return new StageEvent(Kind.TRANSMISSION, id, 0, 0, intervalTicks);
    }

    public static StageEvent effect(String id, int amp, long dur, long interval) {
        return new StageEvent(Kind.EFFECT_PULSE, id, amp, dur, interval);
    }

    public static StageEvent sound(String id, long interval) {
        return new StageEvent(Kind.SOUND_PULSE, id, 0, 0, interval);
    }

    public Kind kind() { return kind; }
    public String name() { return name; }
    public int amplifier() { return amplifier; }
    public long durationTicks() { return durationTicks; }
    public long intervalTicks() { return intervalTicks; }
}
