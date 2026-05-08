package com.tnhzr.ihs.medicine;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

public final class Medicine {

    public enum Type { CURE, EFFECT_CLEAR, BUFF, TRANQUILIZER }

    private final String id;
    private final Material material;
    private final int customModelData;
    private final String itemModel;
    private final String name;
    private final List<String> lore;

    private final Type type;
    private final List<String> curesInfections;
    private final int healPoints;
    private final int dailyLimit;
    private final List<String> clearsPotionEffects;
    private final List<String> applyPotionEffects;

    private final int sleepSeconds;
    private final int onsetSeconds;
    private final boolean revealInLore;

    public Medicine(String id, Material material, int customModelData, String itemModel,
                    String name, List<String> lore,
                    Type type, List<String> curesInfections, int healPoints, int dailyLimit,
                    List<String> clearsPotionEffects, List<String> applyPotionEffects,
                    int sleepSeconds, int onsetSeconds, boolean revealInLore) {
        this.id = id;
        this.material = material;
        this.customModelData = customModelData;
        this.itemModel = itemModel;
        this.name = name;
        this.lore = lore == null ? Collections.emptyList() : lore;
        this.type = type;
        this.curesInfections = curesInfections == null ? Collections.emptyList() : curesInfections;
        this.healPoints = healPoints;
        this.dailyLimit = dailyLimit;
        this.clearsPotionEffects = clearsPotionEffects == null ? Collections.emptyList() : clearsPotionEffects;
        this.applyPotionEffects = applyPotionEffects == null ? Collections.emptyList() : applyPotionEffects;
        this.sleepSeconds = sleepSeconds;
        this.onsetSeconds = onsetSeconds;
        this.revealInLore = revealInLore;
    }

    public String id() { return id; }
    public Material material() { return material; }
    public int customModelData() { return customModelData; }
    /** Vanilla 1.21.4+ item-model component (namespaced key). May be null/empty. */
    public String itemModel() { return itemModel; }
    public String name() { return name; }
    public List<String> lore() { return lore; }
    public Type type() { return type; }
    public List<String> curesInfections() { return curesInfections; }
    public int healPoints() { return healPoints; }
    public int dailyLimit() { return dailyLimit; }
    public List<String> clearsPotionEffects() { return clearsPotionEffects; }
    public List<String> applyPotionEffects() { return applyPotionEffects; }
    /** Forced-sleep duration in seconds. Used by the tranquilizer type. */
    public int sleepSeconds() { return sleepSeconds; }
    /** Onset delay (blindness window) in seconds before forced sleep. */
    public int onsetSeconds() { return onsetSeconds; }
    /** Whether laced food / coated arrows should announce themselves in item lore. */
    public boolean revealInLore() { return revealInLore; }
}
