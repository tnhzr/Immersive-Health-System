package com.tnhzr.ihs.api.internal;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.api.LaboratoryService;
import com.tnhzr.ihs.lab.LabManager;
import com.tnhzr.ihs.lab.LabRecipe;
import com.tnhzr.ihs.lab.Laboratory;
import com.tnhzr.ihs.lab.RecipeBookGui;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

final class LaboratoryServiceImpl implements LaboratoryService {

    private final ImmersiveHealthSystem plugin;

    LaboratoryServiceImpl(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    private LabManager lm() { return plugin.laboratories(); }

    @Override
    public ItemStack labItem() {
        return lm().createLabItem();
    }

    @Override
    public boolean isLaboratoryAt(Location location) {
        if (location == null) return false;
        Laboratory l = lm().at(location);
        return l != null;
    }

    @Override
    public Set<String> knownRecipes() {
        return new HashSet<>(lm().recipes().recipes().keySet());
    }

    @Override
    public String recipeResult(String recipeId) {
        if (recipeId == null) return null;
        LabRecipe r = lm().recipes().recipes().get(recipeId);
        return r == null ? null : r.resultMedicineId();
    }

    @Override
    public boolean openRecipeBrowser(Player viewer) {
        if (viewer == null) return false;
        new RecipeBookGui(plugin, viewer).open();
        return true;
    }
}
