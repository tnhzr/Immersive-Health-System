package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LabRecipeManager {

    private final ImmersiveHealthSystem plugin;
    private final Map<String, LabRecipe> recipes = new LinkedHashMap<>();

    public LabRecipeManager(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    public void load() {
        recipes.clear();
        ConfigurationSection root = plugin.configs().labRecipes()
                .getConfigurationSection("laboratory_recipes");
        if (root == null) return;
        for (String resultId : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(resultId);
            if (s == null) continue;
            List<LabIngredient> parsed = new ArrayList<>();
            for (String entry : s.getStringList("ingredients")) {
                LabIngredient ing = LabIngredient.parse(entry);
                if (ing != null) parsed.add(ing);
            }
            if (parsed.isEmpty()) continue;
            int time = s.getInt("craft_time", 10);
            boolean visible = s.getBoolean("visible_by_default", true);
            recipes.put(resultId, new LabRecipe(resultId, parsed, time, visible));
        }
        plugin.getLogger().info("Loaded " + recipes.size() + " laboratory recipes.");
    }

    public LabRecipe match(org.bukkit.inventory.ItemStack[] inputs,
                           com.tnhzr.ihs.medicine.MedicineItemFactory factory) {
        for (LabRecipe recipe : recipes.values()) {
            if (recipe.matches(inputs, factory)) return recipe;
        }
        return null;
    }

    public Map<String, LabRecipe> recipes() { return recipes; }
}
