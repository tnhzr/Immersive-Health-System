package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.medicine.Medicine;
import com.tnhzr.ihs.medicine.MedicineItemFactory;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recipe browser. Shown in the laboratory GUI when the recipe-book button
 * is clicked. Lists every {@link LabRecipe} whose
 * {@code visible_by_default} flag is {@code true} (or all recipes for
 * admins). Each entry shows the result item with the ingredient list +
 * craft time as lore. Pagination is handled via prev/next slots.
 */
public final class RecipeBookGui implements Listener {

    private static final String NAV_TAG = "recipe_book_nav";

    private final ImmersiveHealthSystem plugin;
    private final NamespacedKey navKey;
    private final List<LabRecipe> entries;
    private Inventory inventory;
    private List<Integer> listSlots;
    private int prevSlot;
    private int nextSlot;
    private int closeSlot;
    private int pageSize;
    private int page;
    private final Player viewer;
    private final boolean showLockedAsBarriers;
    private final Laboratory parentLab;

    public RecipeBookGui(ImmersiveHealthSystem plugin, Player viewer) {
        this(plugin, viewer, null);
    }

    public RecipeBookGui(ImmersiveHealthSystem plugin, Player viewer, Laboratory parentLab) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.parentLab = parentLab;
        this.navKey = new NamespacedKey(plugin, "recipe_book_nav");
        boolean isAdmin = viewer.hasPermission("ihs.admin");
        // When false, hidden recipes are completely omitted from the
        // catalogue (the player cannot infer how many secret recipes
        // exist). When true, they are listed as "❓" stubs.
        boolean showLocked = plugin.configs().main()
                .getBoolean("laboratory.recipe_book.show_locked_entries", true);
        this.showLockedAsBarriers = !isAdmin && showLocked;
        this.entries = new ArrayList<>();
        for (LabRecipe r : plugin.laboratories().recipes().recipes().values()) {
            if (isAdmin || r.visibleByDefault()) {
                entries.add(r);
            } else if (showLockedAsBarriers) {
                entries.add(r); // rendered as a locked stub by buildEntry()
            }
        }
    }

    public void open() {
        ConfigurationSection cfg = plugin.configs().main()
                .getConfigurationSection("laboratory.recipe_book");
        if (cfg == null) cfg = plugin.configs().main().createSection("laboratory.recipe_book");
        int size = cfg.getInt("size", 54);
        if (size % 9 != 0 || size < 27 || size > 54) size = 54;
        pageSize = cfg.getInt("page_size", 28);
        listSlots = cfg.getIntegerList("list_slots");
        if (listSlots.isEmpty()) listSlots = defaultListSlots();
        prevSlot = cfg.getInt("prev_slot", 45);
        nextSlot = cfg.getInt("next_slot", 53);
        closeSlot = cfg.getInt("close_slot", 49);

        Component title = plugin.locale().component("lab.recipe_book.title");
        if (title.equals(Component.empty())) {
            title = Text.component("&2Каталог рецептов");
        }
        inventory = Bukkit.createInventory(null, size, title);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        render();
        viewer.openInventory(inventory);
    }

    private static List<Integer> defaultListSlots() {
        List<Integer> out = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                out.add(row * 9 + col);
            }
        }
        return out;
    }

    private void render() {
        for (int s = 0; s < inventory.getSize(); s++) inventory.setItem(s, null);
        // Navigation panes.
        ItemStack prev = navItem(Material.ARROW, plugin.locale().component("lab.recipe_book.prev"));
        ItemStack next = navItem(Material.ARROW, plugin.locale().component("lab.recipe_book.next"));
        ItemStack close = navItem(Material.BARRIER, plugin.locale().component("lab.recipe_book.close"));
        inventory.setItem(prevSlot, prev);
        inventory.setItem(nextSlot, next);
        inventory.setItem(closeSlot, close);
        // Border.
        Material borderMat = Material.matchMaterial(plugin.configs().main()
                .getString("laboratory.gui.border_filler_material", "GREEN_STAINED_GLASS_PANE"));
        if (borderMat == null) borderMat = Material.GREEN_STAINED_GLASS_PANE;
        ItemStack pane = new ItemStack(borderMat);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Text.component(" "));
            pane.setItemMeta(paneMeta);
        }
        Set<Integer> reserved = new HashSet<>(listSlots);
        reserved.add(prevSlot); reserved.add(nextSlot); reserved.add(closeSlot);
        for (int s = 0; s < inventory.getSize(); s++) {
            if (!reserved.contains(s)) inventory.setItem(s, pane);
        }
        // Page entries.
        int from = page * pageSize;
        int to = Math.min(entries.size(), from + pageSize);
        Material lockedMat = Material.matchMaterial(plugin.configs().main()
                .getString("laboratory.recipe_book.locked_material", "BARRIER"));
        if (lockedMat == null) lockedMat = Material.BARRIER;
        MedicineItemFactory mf = plugin.medicines() != null ? plugin.medicines().factory() : null;
        for (int i = from; i < to; i++) {
            int slot = listSlots.get(i - from);
            LabRecipe recipe = entries.get(i);
            inventory.setItem(slot, buildEntry(recipe, mf, lockedMat));
        }
    }

    private ItemStack buildEntry(LabRecipe recipe, MedicineItemFactory mf, Material lockedMat) {
        boolean locked = !recipe.visibleByDefault() && !viewer.hasPermission("ihs.admin");
        if (locked) {
            ItemStack stack = new ItemStack(lockedMat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.locale().component("lab.recipe_book.locked_name"));
                meta.lore(List.of(
                        plugin.locale().component("lab.recipe_book.locked_lore")
                ));
                tagNav(meta);
                stack.setItemMeta(meta);
            }
            return stack;
        }
        Medicine med = plugin.medicines() == null ? null
                : plugin.medicines().medicine(recipe.resultMedicineId());
        ItemStack stack = (med != null && mf != null)
                ? mf.create(med, 1)
                : new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.locale().component("lab.recipe_book.entry_header"));
            for (LabIngredient ing : recipe.ingredients()) {
                lore.add(buildIngredientLore(ing));
            }
            lore.add(Text.component("&7"));
            lore.add(plugin.locale().component("lab.recipe_book.entry_time",
                    Map.of("seconds", String.valueOf(recipe.craftTimeSeconds()))));
            meta.lore(lore);
            tagNav(meta);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Renders an ingredient as a single localized lore line, e.g.
     * "  • Красный гриб ×1". Vanilla materials use the client-side
     * translation key so the player sees their own language; custom
     * medicines use the configured display name.
     */
    private Component buildIngredientLore(LabIngredient ing) {
        // Build the option list as a chain of localized name components.
        // Each name is forced to white because Adventure children
        // inherit their parent's color, not a preceding sibling's \u2014
        // without this the translatable would render in dark-grey
        // (the color of the "  \u2022 " bullet head we attach them to).
        List<Component> names = new ArrayList<>();
        for (LabIngredient.Option opt : ing.options()) {
            if (opt.source == LabIngredient.Source.VANILLA) {
                Material m = Material.matchMaterial(opt.identifier);
                if (m != null) {
                    names.add(Component.translatable(m.translationKey())
                            .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
                } else {
                    names.add(Text.component("&7" + opt.identifier));
                }
            } else {
                Medicine med = plugin.medicines() != null
                        ? plugin.medicines().medicine(opt.identifier) : null;
                names.add(Text.component(med != null ? med.name() : "&7" + opt.identifier));
            }
        }
        Component joined = Component.empty();
        Component sep = Text.component("&8 | ");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) joined = joined.append(sep);
            joined = joined.append(names.get(i));
        }
        return Text.component("  &8\u2022 ")
                .append(joined)
                .append(Text.component(" &7\u00d7&a" + ing.amount()))
                .decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack navItem(Material mat, Component name) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            tagNav(meta);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void tagNav(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(navKey, PersistentDataType.STRING, NAV_TAG);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot == prevSlot) {
            if (page > 0) { page--; render(); }
            return;
        }
        if (slot == nextSlot) {
            if ((page + 1) * pageSize < entries.size()) { page++; render(); }
            return;
        }
        if (slot == closeSlot) {
            // Close button — return to the parent laboratory GUI if we were
            // opened from one, otherwise just close.
            if (e.getWhoClicked() instanceof Player p) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    HandlerList.unregisterAll(this);
                    p.closeInventory();
                    if (parentLab != null && plugin.laboratories() != null) {
                        var manager = plugin.laboratories();
                        // Re-resolve the lab by location so that a lab
                        // broken while the recipe book was open does not
                        // resurrect a stale Laboratory reference (item
                        // dupe vector). If the lab no longer exists we
                        // just leave the inventory closed.
                        Laboratory live = manager.at(parentLab.location());
                        if (live != null) {
                            LabGui gui = manager.openGuis().computeIfAbsent(
                                    live.location(),
                                    k -> new LabGui(plugin, manager, live));
                            gui.open(p);
                        }
                    }
                });
            }
            return;
        }
        // Recipe entry click — open the detail page (CraftEngine-style
        // visual layout) for the corresponding recipe.
        int slotIdxInPage = listSlots.indexOf(slot);
        if (slotIdxInPage < 0) return;
        int recipeIdx = page * pageSize + slotIdxInPage;
        if (recipeIdx >= entries.size()) return;
        LabRecipe recipe = entries.get(recipeIdx);
        boolean locked = !recipe.visibleByDefault() && !viewer.hasPermission("ihs.admin");
        if (locked) return; // Don't reveal anything about hidden recipes.
        if (e.getWhoClicked() instanceof Player p) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.closeInventory();
                new RecipeDetailGui(plugin, p, recipe, this).open();
            });
        }
    }

    /** Reopens this recipe browser at its current page (called by the detail GUI's back button). */
    public void reopen(Player viewerPlayer) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Re-register the listener and rebuild the inventory so the
            // viewer sees a fresh state.
            HandlerList.unregisterAll(this);
            new RecipeBookGui(plugin, viewerPlayer, parentLab).open();
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
            if (viewer != e.getPlayer()) return;
        }
        HandlerList.unregisterAll(this);
    }

    /** Returns the locale string list, or empty if missing. */
    @SuppressWarnings("unused")
    private List<String> stringList(String key) {
        List<String> ls = plugin.locale().rawList(key);
        return ls == null ? Collections.emptyList() : ls;
    }
}
