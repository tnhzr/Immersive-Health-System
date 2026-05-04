package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.medicine.Medicine;
import com.tnhzr.ihs.medicine.MedicineItemFactory;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Double-page" recipe detail GUI à la CraftEngine. Displays the recipe
 * as it would look if laid out in a 3x3 crafting grid (left page) plus
 * the resulting medicine on the right page, with localized item names
 * instead of raw material/medicine ids.
 *
 * <p>Layout (54 slots):
 * <pre>
 *   .  .  .  .  .  .  .  .  .
 *   .  G  G  G  .  .  .  .  .
 *   .  G  G  G  .  →  .  M  .
 *   .  G  G  G  .  .  .  .  .
 *   .  .  .  .  .  .  .  .  .
 *   B  .  .  .  .  T  .  .  .
 * </pre>
 * where G = ingredient slot, → = arrow indicator, M = result medicine,
 * B = back-to-list button, T = craft-time / metadata pane.
 */
public final class RecipeDetailGui implements Listener {

    private static final int[] GRID_SLOTS = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
    };
    private static final int ARROW_SLOT = 23;
    private static final int RESULT_SLOT = 25;
    private static final int BACK_SLOT = 45;
    private static final int META_SLOT = 41;

    private final ImmersiveHealthSystem plugin;
    private final Player viewer;
    private final LabRecipe recipe;
    private final RecipeBookGui parent;
    private Inventory inventory;
    private boolean listenerRegistered;

    public RecipeDetailGui(ImmersiveHealthSystem plugin, Player viewer,
                           LabRecipe recipe, RecipeBookGui parent) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.recipe = recipe;
        this.parent = parent;
    }

    public void open() {
        Component title = plugin.locale().component("lab.recipe_detail.title",
                Map.of("medicine", componentToLegacy(medicineDisplay(recipe.resultMedicineId()))));
        inventory = Bukkit.createInventory(null, 54, title);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        listenerRegistered = true;
        render();
        viewer.openInventory(inventory);
    }

    private void render() {
        Material borderMat = Material.matchMaterial(plugin.configs().main()
                .getString("laboratory.gui.border_filler_material", "GREEN_STAINED_GLASS_PANE"));
        if (borderMat == null) borderMat = Material.GREEN_STAINED_GLASS_PANE;
        ItemStack pane = new ItemStack(borderMat);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Text.component(" "));
            pane.setItemMeta(paneMeta);
        }
        Set<Integer> reserved = new HashSet<>();
        for (int s : GRID_SLOTS) reserved.add(s);
        reserved.add(ARROW_SLOT);
        reserved.add(RESULT_SLOT);
        reserved.add(BACK_SLOT);
        reserved.add(META_SLOT);
        for (int s = 0; s < inventory.getSize(); s++) {
            if (!reserved.contains(s)) inventory.setItem(s, pane);
        }
        // Ingredient grid (place each ingredient into a 3x3 slot in
        // arrival order — recipe authors implicitly control layout).
        MedicineItemFactory mf = plugin.medicines() != null
                ? plugin.medicines().factory() : null;
        List<LabIngredient> ingredients = recipe.ingredients();
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (i >= ingredients.size()) {
                inventory.setItem(GRID_SLOTS[i], null);
                continue;
            }
            LabIngredient ing = ingredients.get(i);
            inventory.setItem(GRID_SLOTS[i], buildIngredientItem(ing, mf));
        }
        // Arrow indicator.
        ItemStack arrow = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        if (arrowMeta != null) {
            arrowMeta.displayName(plugin.locale().component("lab.recipe_detail.arrow"));
            arrow.setItemMeta(arrowMeta);
        }
        inventory.setItem(ARROW_SLOT, arrow);
        // Result medicine.
        Medicine med = plugin.medicines() == null
                ? null : plugin.medicines().medicine(recipe.resultMedicineId());
        ItemStack resultStack = (med != null && mf != null)
                ? mf.create(med, 1)
                : new ItemStack(Material.PAPER);
        ItemMeta resultMeta = resultStack.getItemMeta();
        if (resultMeta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.locale().component("lab.recipe_detail.result_header"));
            lore.add(plugin.locale().component("lab.recipe_detail.result_time",
                    Map.of("seconds", String.valueOf(recipe.craftTimeSeconds()))));
            resultMeta.lore(lore);
            resultStack.setItemMeta(resultMeta);
        }
        inventory.setItem(RESULT_SLOT, resultStack);
        // Back button.
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.locale().component("lab.recipe_detail.back"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(BACK_SLOT, back);
        // Meta / craft-time pane.
        ItemStack meta = new ItemStack(Material.CLOCK);
        ItemMeta metaMeta = meta.getItemMeta();
        if (metaMeta != null) {
            metaMeta.displayName(plugin.locale().component("lab.recipe_detail.meta_title"));
            metaMeta.lore(List.of(
                    plugin.locale().component("lab.recipe_detail.meta_time",
                            Map.of("seconds", String.valueOf(recipe.craftTimeSeconds())))));
            meta.setItemMeta(metaMeta);
        }
        inventory.setItem(META_SLOT, meta);
    }

    private ItemStack buildIngredientItem(LabIngredient ing, MedicineItemFactory mf) {
        ItemStack stack;
        Component nameComponent;
        // Prefer a vanilla material so the player sees the actual ingredient
        // texture; fall back to the first custom medicine if the ingredient
        // is exclusively custom.
        Material vanilla = ing.previewVanillaMaterial();
        if (vanilla != null) {
            stack = new ItemStack(vanilla, Math.max(1, ing.amount()));
            // Use the vanilla translation key so the client renders the
            // material name in its own language (e.g. "Сот" instead of
            // "Honey Comb").
            nameComponent = Component.translatable(vanilla.translationKey())
                    .append(Text.component(" &7×&a" + ing.amount()))
                    .decoration(TextDecoration.ITALIC, false);
        } else {
            String customId = ing.previewCustomId();
            Medicine med = customId != null && plugin.medicines() != null
                    ? plugin.medicines().medicine(customId) : null;
            if (med != null && mf != null) {
                stack = mf.create(med, Math.max(1, ing.amount()));
            } else {
                stack = new ItemStack(Material.PAPER, Math.max(1, ing.amount()));
            }
            String label = med != null ? med.name() : (customId != null ? customId : ing.describe());
            nameComponent = Text.component("&f" + label + " &7×&a" + ing.amount());
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(nameComponent);
            List<Component> lore = new ArrayList<>();
            // For ingredients that accept multiple alternatives
            // (e.g. coal | charcoal), list each alternative on its own
            // lore line using the same client-side translation trick.
            if (ing.options().size() > 1) {
                for (LabIngredient.Option opt : ing.options()) {
                    lore.add(buildOptionLine(opt));
                }
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Component buildOptionLine(LabIngredient.Option opt) {
        if (opt.source == LabIngredient.Source.VANILLA) {
            Material m = Material.matchMaterial(opt.identifier);
            if (m != null) {
                // Force WHITE on the translatable child so it doesn't
                // inherit the dark-grey color of the bullet head.
                return Text.component("&8\u2022 ")
                        .append(Component.translatable(m.translationKey())
                                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false);
            }
            return Text.component("&8\u2022 &7" + opt.identifier)
                    .decoration(TextDecoration.ITALIC, false);
        }
        Medicine med = plugin.medicines() != null
                ? plugin.medicines().medicine(opt.identifier) : null;
        return Text.component("&8\u2022 &f" + (med != null ? med.name() : opt.identifier))
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component medicineDisplay(String id) {
        Medicine med = plugin.medicines() == null ? null : plugin.medicines().medicine(id);
        if (med == null) return Text.component("&f" + id);
        return Text.component(med.name());
    }

    private static String componentToLegacy(Component component) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().serialize(component);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot == BACK_SLOT && e.getWhoClicked() instanceof Player p) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
            if (parent != null) {
                parent.reopen(p);
            } else {
                p.closeInventory();
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        if (listenerRegistered) {
            // Single-viewer GUI: deregister when closed.
            for (HumanEntity v : new ArrayList<>(inventory.getViewers())) {
                if (v != e.getPlayer()) return;
            }
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
    }
}
