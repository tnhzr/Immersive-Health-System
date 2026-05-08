package com.tnhzr.ihs.api;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/** Public surface for the laboratory module. */
public interface LaboratoryService {

    /** A fresh laboratory item, ready to be placed by the player. */
    ItemStack labItem();

    /** True if this block location currently hosts an IHS laboratory. */
    boolean isLaboratoryAt(Location location);

    /** Recipe ids known from {@code lab_recipes.yml}. */
    Set<String> knownRecipes();

    /**
     * @return the medicine id produced by the named recipe, or
     *         {@code null} if the recipe is unknown.
     */
    String recipeResult(String recipeId);

    /**
     * Open the recipe browser GUI for the given player.
     *
     * @return true if the GUI was opened
     */
    boolean openRecipeBrowser(org.bukkit.entity.Player viewer);
}
