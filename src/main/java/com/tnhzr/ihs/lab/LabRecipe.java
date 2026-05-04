package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.medicine.MedicineItemFactory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class LabRecipe {

    private final String resultMedicineId;
    private final List<LabIngredient> ingredients;
    private final int craftTimeSeconds;
    private final boolean visibleByDefault;

    public LabRecipe(String resultMedicineId, List<LabIngredient> ingredients,
                     int craftTimeSeconds, boolean visibleByDefault) {
        this.resultMedicineId = resultMedicineId;
        this.ingredients = ingredients;
        this.craftTimeSeconds = craftTimeSeconds;
        this.visibleByDefault = visibleByDefault;
    }

    public String resultMedicineId() { return resultMedicineId; }
    public List<LabIngredient> ingredients() { return ingredients; }
    public int craftTimeSeconds() { return craftTimeSeconds; }
    public boolean visibleByDefault() { return visibleByDefault; }

    /**
     * Walk the input slots and try to match every ingredient at least once.
     * Each input slot is consumed by at most one ingredient match per call.
     */
    public boolean matches(ItemStack[] inputs, MedicineItemFactory factory) {
        boolean[] consumed = new boolean[inputs.length];
        for (LabIngredient ing : ingredients) {
            int found = -1;
            for (int i = 0; i < inputs.length; i++) {
                if (consumed[i]) continue;
                if (ing.matches(inputs[i], factory)) { found = i; break; }
            }
            if (found < 0) return false;
            consumed[found] = true;
        }
        return true;
    }

    /**
     * Decrement input items by the recipe amounts. Returns false if it could not consume.
     */
    public boolean consume(ItemStack[] inputs, MedicineItemFactory factory) {
        return consumeAndSnapshot(inputs, factory, null);
    }

    /**
     * Decrement input items by the recipe amounts and, when
     * {@code snapshot} is non-null, append a clone of every consumed
     * stack (sized to exactly the consumed amount) so the caller can
     * later refund the laboratory if the player cancels the queue.
     */
    public boolean consumeAndSnapshot(ItemStack[] inputs,
                                      MedicineItemFactory factory,
                                      List<ItemStack> snapshot) {
        boolean[] consumed = new boolean[inputs.length];
        List<int[]> bindings = new ArrayList<>();
        for (LabIngredient ing : ingredients) {
            int found = -1;
            for (int i = 0; i < inputs.length; i++) {
                if (consumed[i]) continue;
                if (ing.matches(inputs[i], factory)) { found = i; break; }
            }
            if (found < 0) return false;
            consumed[found] = true;
            bindings.add(new int[]{ found, ing.amount() });
        }
        for (int[] binding : bindings) {
            ItemStack item = inputs[binding[0]];
            int amount = binding[1];
            if (snapshot != null && item != null) {
                ItemStack copy = item.clone();
                copy.setAmount(amount);
                snapshot.add(copy);
            }
            int newAmount = item.getAmount() - amount;
            if (newAmount <= 0) inputs[binding[0]] = null;
            else item.setAmount(newAmount);
        }
        return true;
    }
}
