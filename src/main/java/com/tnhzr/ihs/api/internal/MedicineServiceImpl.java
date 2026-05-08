package com.tnhzr.ihs.api.internal;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.api.MedicineService;
import com.tnhzr.ihs.medicine.Medicine;
import com.tnhzr.ihs.medicine.MedicineManager;
import com.tnhzr.ihs.medicine.TranquilizerListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

final class MedicineServiceImpl implements MedicineService {

    private final ImmersiveHealthSystem plugin;

    MedicineServiceImpl(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    private MedicineManager mm() { return plugin.medicines(); }

    @Override
    public ItemStack itemOf(String medicineId, int amount) {
        if (medicineId == null) return null;
        Medicine m = mm().medicine(medicineId);
        if (m == null) return null;
        return mm().factory().create(m, Math.max(1, amount));
    }

    @Override
    public void give(Player player, String medicineId, int amount) {
        if (player == null) return;
        ItemStack stack = itemOf(medicineId, amount);
        if (stack == null) return;
        player.getInventory().addItem(stack)
                .forEach((idx, leftover) ->
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    @Override
    public String idOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        org.bukkit.persistence.PersistentDataContainer pdc =
                stack.getItemMeta().getPersistentDataContainer();
        return pdc.get(mm().factory().medicineKey(),
                org.bukkit.persistence.PersistentDataType.STRING);
    }

    @Override
    public boolean isMedicine(ItemStack stack) {
        return idOf(stack) != null;
    }

    @Override
    public Set<String> knownMedicines() {
        return new HashSet<>(mm().medicines().keySet());
    }

    @Override
    public void applyTranquilizerSleep(Player player, int onsetSeconds, int sleepSeconds) {
        if (player == null) return;
        TranquilizerListener listener = mm().tranquilizer();
        if (listener == null) {
            // Module disabled -> minimal fallback so the contract still
            // does *something* without crashing.
            try {
                player.sleep(player.getLocation(), true);
            } catch (Exception ignored) {}
            return;
        }
        listener.applySleepRoutine(player,
                Math.max(0, onsetSeconds), Math.max(1, sleepSeconds));
    }
}
