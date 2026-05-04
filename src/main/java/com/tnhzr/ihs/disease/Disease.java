package com.tnhzr.ihs.disease;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Disease {

    public enum Type { GLOBAL, LOCAL }

    /** Sentinel for "use the global default from config.yml". */
    public static final int TREMOR_DEFAULT = Integer.MIN_VALUE;

    private final String id;
    private final String name;
    private final Type type;
    private final String cureType;
    private final double infectionChance;
    private final int tremorThreshold;
    private final Map<String, TransmissionSettings> transmissions;
    private final List<Stage> stages;

    public Disease(String id, String name, Type type, String cureType,
                   double infectionChance, int tremorThreshold,
                   Map<String, TransmissionSettings> transmissions,
                   List<Stage> stages) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.cureType = cureType;
        this.infectionChance = infectionChance;
        this.tremorThreshold = tremorThreshold;
        this.transmissions = transmissions == null ? Collections.emptyMap() : transmissions;
        this.stages = stages == null ? Collections.emptyList() : stages;
    }

    public String id() { return id; }
    public String name() { return name; }
    public Type type() { return type; }
    public String cureType() { return cureType; }
    public double infectionChance() { return infectionChance; }
    public int tremorThreshold() { return tremorThreshold; }
    public Map<String, TransmissionSettings> transmissions() { return transmissions; }
    public List<Stage> stages() { return stages; }

    public Stage stageFor(int scale) {
        for (Stage s : stages) if (s.contains(scale)) return s;
        return null;
    }
}
