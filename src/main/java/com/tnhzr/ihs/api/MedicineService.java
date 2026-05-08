package com.tnhzr.ihs.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/** Public surface for the medicine module. */
public interface MedicineService {

    /**
     * Build a fresh ItemStack for the medicine with the given id (as
     * declared in {@code medicines.yml}). Returns {@code null} if no
     * such medicine exists.
     */
    ItemStack itemOf(String medicineId, int amount);

    /** Convenience: insert {@code amount} of the medicine into the player. */
    void give(Player player, String medicineId, int amount);

    /**
     * @return the medicine id stored in the item's PDC, or {@code null}
     *         if it isn't an IHS medicine.
     */
    String idOf(ItemStack stack);

    /** True if the item is any IHS medicine. */
    boolean isMedicine(ItemStack stack);

    /** All medicine ids registered in {@code medicines.yml}. */
    Set<String> knownMedicines();

    /**
     * Force the tranquilizer sleep routine on a player without
     * requiring an actual tranquilizer item.
     *
     * @param onsetSeconds blindness phase length before sleep starts
     * @param sleepSeconds total sleep duration (clamped to a minimum 1s)
     */
    void applyTranquilizerSleep(Player player, int onsetSeconds, int sleepSeconds);
}
