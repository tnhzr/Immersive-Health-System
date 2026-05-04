package com.tnhzr.ihs.medicine;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class MedicineItemFactory {

    private final ImmersiveHealthSystem plugin;
    private final NamespacedKey cachedMedicineKey;

    public MedicineItemFactory(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
        this.cachedMedicineKey = plugin.key(plugin.configs().main()
                .getString("medicine.pdc_medicine_key", "medicine_id"));
    }

    public NamespacedKey medicineKey() {
        return cachedMedicineKey;
    }

    public ItemStack create(Medicine m, int amount) {
        ItemStack stack = new ItemStack(m.material(), amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(Text.component(m.name()).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        if (!m.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : m.lore()) {
                lore.add(Text.component(line)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }

        if (m.customModelData() > 0) meta.setCustomModelData(m.customModelData());
        // 1.21.4+ vanilla item-model component. Setting this binds the
        // item to a model file at assets/<ns>/models/<path>.json on the
        // client (no CraftEngine binding required).
        if (m.itemModel() != null && !m.itemModel().isBlank()) {
            NamespacedKey modelKey = NamespacedKey.fromString(m.itemModel());
            if (modelKey != null) {
                try {
                    meta.setItemModel(modelKey);
                } catch (NoSuchMethodError ignored) {
                    // Older API — fall back to custom_model_data.
                }
            }
        }

        // Strip vanilla food behaviour. Effects from the new ConsumableComponent
        // are not configured here — we cancel PlayerItemConsumeEvent and apply
        // our own medicine logic in MedicineConsumeListener.
        try {
            org.bukkit.inventory.meta.components.FoodComponent food = meta.getFood();
            if (food != null) {
                food.setCanAlwaysEat(true);
                food.setNutrition(0);
                food.setSaturation(0.0F);
                meta.setFood(food);
            }
        } catch (NoSuchMethodError ignored) {
            // Older API — fall back silently.
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(medicineKey(), PersistentDataType.STRING, m.id());

        stack.setItemMeta(meta);
        return stack;
    }

    public String medicineIdOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.get(medicineKey(), PersistentDataType.STRING);
    }
}
