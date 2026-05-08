package com.tnhzr.ihs.api;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

/** Public surface for the disease module. */
public interface DiseaseService {

    /**
     * Force-infect {@code player} with the disease whose id matches
     * {@code diseaseId} from {@code infections.yml}. {@code chance} is
     * the probability roll (0.0 .. 1.0); {@code 1.0} guarantees infection.
     *
     * @return {@code true} if the player was newly infected, {@code false}
     *         if they were already infected or the roll missed.
     */
    boolean infect(Player player, String diseaseId, double chance);

    /** Reduce the disease scale by {@code amount} (clamped to 0..100). */
    void heal(Player player, String diseaseId, int amount);

    /** Cure the player of every active infection. */
    void healAll(Player player);

    /** Current disease scale 0..100 for the named infection, or 0 if absent. */
    int scale(Player player, String diseaseId);

    /** Set the disease scale to an exact value (clamped 0..100). */
    void setScale(Player player, String diseaseId, int value);

    /** Disease ids the player currently has at scale > 0. */
    Set<String> activeInfections(Player player);

    /** Snapshot of the player's full infection map (id -> scale). */
    Map<String, Integer> infectionMap(Player player);

    /** Disease ids known from {@code infections.yml}. */
    Set<String> knownDiseases();
}
