package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.medicine.Medicine;
import com.tnhzr.ihs.medicine.MedicineItemFactory;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Laboratory inventory GUI. Built on a 54-slot double-chest layout
 * matching the user's blueprint:
 * <pre>
 *   .  .  .  .  .  .  .  .  .
 *   .  I  I  I  .  .  .  .  .
 *   .  I  I  I  .  P  .  R  .
 *   .  I  I  I  .  .  .  C  .
 *   .  .  .  .  .  F  .  .  .
 *   B  .  .  .  .  .  .  .  .
 * </pre>
 * Where I = ingredient inputs (3x3), P = progress indicator (with hover
 * info + Shift+RC cancel), R = result preview/output, C = confirm
 * button, F = fuel slot, B = recipe-book button.
 */
public final class LabGui implements Listener {

    private static final String PREVIEW_TAG = "preview_marker";

    private final ImmersiveHealthSystem plugin;
    private final LabManager manager;
    private final Laboratory lab;
    private final NamespacedKey previewKey;
    private Inventory inventory;
    private List<Integer> inputSlots;
    private int progressSlot;
    private int resultSlot;
    private int confirmSlot;
    private int fuelSlot;
    private int recipeBookSlot;
    private final Set<Integer> filledFiller = new HashSet<>();
    private boolean listenerRegistered;

    public LabGui(ImmersiveHealthSystem plugin, LabManager manager, Laboratory lab) {
        this.plugin = plugin;
        this.manager = manager;
        this.lab = lab;
        this.previewKey = new NamespacedKey(plugin, "lab_preview");
    }

    public void open(Player player) {
        if (inventory == null) build();
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
        player.openInventory(inventory);
    }

    private void build() {
        ConfigurationSection gui = plugin.configs().main().getConfigurationSection("laboratory.gui");
        if (gui == null) gui = plugin.configs().main().createSection("laboratory.gui");
        int size = gui.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) size = 54;

        inventory = Bukkit.createInventory(null, size, plugin.locale().component("lab.gui.title"));

        inputSlots = gui.getIntegerList("input_slots");
        if (inputSlots.isEmpty()) inputSlots = List.of(10, 11, 12, 19, 20, 21, 28, 29, 30);
        progressSlot = gui.getInt("progress_slot", 23);
        resultSlot = gui.getInt("result_slot", 25);
        confirmSlot = gui.getInt("confirm_slot", 34);
        fuelSlot = gui.getInt("fuel_slot", 41);
        recipeBookSlot = gui.getInt("recipe_book_slot", 45);

        // Make sure the lab's persisted inputs list is sized to match the
        // GUI's input slots so we never lose items on save/load.
        while (lab.inputs().size() < inputSlots.size()) lab.inputs().add(null);
        while (lab.inputs().size() > inputSlots.size()) {
            ItemStack overflow = lab.inputs().remove(lab.inputs().size() - 1);
            if (overflow != null && overflow.getType() != Material.AIR
                    && lab.location().getWorld() != null) {
                lab.location().getWorld().dropItemNaturally(
                        lab.location().add(0.5, 1.0, 0.5), overflow);
            }
        }

        renderState();
        fillBorder(gui);
    }

    private void fillBorder(ConfigurationSection gui) {
        Material filler = Material.matchMaterial(gui.getString("border_filler_material",
                "GREEN_STAINED_GLASS_PANE"));
        if (filler == null) return;
        Set<Integer> reserved = new HashSet<>();
        reserved.addAll(inputSlots);
        reserved.add(progressSlot);
        reserved.add(resultSlot);
        reserved.add(confirmSlot);
        reserved.add(fuelSlot);
        reserved.add(recipeBookSlot);

        ItemStack pane = new ItemStack(filler);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.component(" "));
            tagPreview(meta);
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (reserved.contains(i)) continue;
            inventory.setItem(i, pane);
            filledFiller.add(i);
        }
    }

    public void renderState() {
        // Inputs (preserve player-placed items).
        for (int i = 0; i < inputSlots.size(); i++) {
            int slot = inputSlots.get(i);
            ItemStack stack = i < lab.inputs().size() ? lab.inputs().get(i) : null;
            inventory.setItem(slot, stack);
        }
        // Fuel slot — show the real fuel item if present; otherwise leave
        // the slot completely empty (vanilla furnace UX, no placeholder).
        ItemStack fuel = lab.fuel();
        if (fuel != null && fuel.getType() != Material.AIR) {
            inventory.setItem(fuelSlot, fuel);
        } else {
            inventory.setItem(fuelSlot, null);
        }
        // Result: real item if we have one queued, otherwise ghost preview.
        ItemStack result = lab.result();
        if (result != null && result.getType() != Material.AIR) {
            inventory.setItem(resultSlot, result);
        } else {
            inventory.setItem(resultSlot, buildResultPreview());
        }
        // Confirm + progress + recipe book.
        inventory.setItem(confirmSlot, buildConfirmButton());
        inventory.setItem(progressSlot, buildProgressIndicator());
        inventory.setItem(recipeBookSlot, buildRecipeBookButton());
    }

    public void refresh() {
        if (inventory == null) return;
        renderState();
    }

    private ItemStack buildResultPreview() {
        MedicineItemFactory mf = plugin.medicines().factory();
        ItemStack[] inputs = currentInputArray();
        LabRecipe recipe = manager.recipes().match(inputs, mf);
        Material fallback = paneMaterial("laboratory.gui.progress_idle_material",
                "GRAY_STAINED_GLASS_PANE");
        if (recipe == null) {
            ItemStack none = new ItemStack(fallback);
            ItemMeta meta = none.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.locale().component("lab.gui.preview_empty"));
                tagPreview(meta);
                none.setItemMeta(meta);
            }
            return none;
        }
        Medicine med = plugin.medicines().medicine(recipe.resultMedicineId());
        if (med == null) {
            ItemStack none = new ItemStack(fallback);
            ItemMeta meta = none.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.locale().component("lab.gui.preview_unknown",
                        Map.of("id", recipe.resultMedicineId())));
                tagPreview(meta);
                none.setItemMeta(meta);
            }
            return none;
        }
        ItemStack ghost = mf.create(med, 1);
        ItemMeta meta = ghost.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.locale().component("lab.gui.preview_header"));
            lore.add(plugin.locale().component("lab.gui.preview_time",
                    Map.of("seconds", String.valueOf(recipe.craftTimeSeconds()))));
            meta.lore(lore);
            tagPreview(meta);
            ghost.setItemMeta(meta);
        }
        return ghost;
    }

    private ItemStack buildConfirmButton() {
        boolean active = lab.activeRecipeId() != null;
        ItemStack[] inputs = currentInputArray();
        LabRecipe recipe = manager.recipes().match(inputs, plugin.medicines().factory());
        boolean enabled = recipe != null && (lab.activeRecipeId() == null
                || lab.activeRecipeId().equals(recipe.resultMedicineId()));
        Material mat = enabled
                ? paneMaterial("laboratory.gui.confirm_button_material", "LIME_STAINED_GLASS_PANE")
                : paneMaterial("laboratory.gui.confirm_disabled_material", "GRAY_STAINED_GLASS_PANE");
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (enabled) {
                meta.displayName(plugin.locale().component("lab.gui.confirm_name"));
                List<Component> lore = new ArrayList<>();
                lore.add(plugin.locale().component("lab.gui.confirm_lore"));
                if (active) {
                    lore.add(plugin.locale().component("lab.gui.queue_label",
                            Map.of("count", String.valueOf(lab.queue()))));
                }
                meta.lore(lore);
            } else {
                meta.displayName(plugin.locale().component("lab.gui.confirm_disabled_name"));
                meta.lore(List.of(plugin.locale().component("lab.gui.confirm_disabled_lore")));
            }
            tagPreview(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildProgressIndicator() {
        Material mat = lab.activeRecipeId() != null
                ? paneMaterial("laboratory.gui.progress_active_material", "LIME_STAINED_GLASS_PANE")
                : paneMaterial("laboratory.gui.progress_idle_material", "GRAY_STAINED_GLASS_PANE");
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.locale().component("lab.gui.progress_title"));
            List<Component> lore = new ArrayList<>();
            int energy = lab.fuelEnergy();
            lore.add(plugin.locale().component("lab.gui.progress_energy",
                    Map.of("energy", String.valueOf(energy))));
            lore.add(plugin.locale().component("lab.gui.progress_crafts_possible",
                    Map.of("count", String.valueOf(energy))));
            // Fuel-cost cheatsheet — moved here so the fuel slot itself
            // stays empty (matches the vanilla furnace UX).
            lore.add(Text.component(""));
            lore.add(plugin.locale().component("lab.gui.progress_fuel_header"));
            // Build the fuel-line lore as a true Component so the
            // material name is rendered through the client's vanilla
            // language file (e.g. ru_ru \u2192 "\u041c\u0445\u043e\u0432\u043e\u0439 \u0431\u043b\u043e\u043a") rather than
            // raw "MOSS_BLOCK".
            String fuelLineRaw = plugin.locale().raw("lab.gui.progress_fuel_line");
            for (Material fuel : manager.fuelMaterials()) {
                lore.add(buildFuelLine(fuelLineRaw,
                        Component.translatable(fuel.translationKey()),
                        String.valueOf(manager.fuelEnergyPerItem(fuel))));
            }
            if (lab.activeRecipeId() != null) {
                String medId = lab.activeRecipeId();
                Component medName = medicineDisplay(medId);
                lore.add(Text.component(""));
                lore.add(plugin.locale().component("lab.gui.progress_active",
                        Map.of("recipe", componentToLegacy(medName))));
                lore.add(plugin.locale().component("lab.gui.progress_remaining",
                        Map.of("seconds", String.valueOf(lab.activeSecondsRemaining()))));
                lore.add(plugin.locale().component("lab.gui.progress_queue",
                        Map.of("count", String.valueOf(lab.queue()))));
                lore.add(Text.component(""));
                lore.add(plugin.locale().component("lab.gui.progress_cancel_hint"));
            } else {
                lore.add(Text.component(""));
                lore.add(plugin.locale().component("lab.gui.progress_idle_hint"));
            }
            meta.lore(lore);
            tagPreview(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildRecipeBookButton() {
        Material mat = paneMaterial("laboratory.gui.recipe_book_material", "KNOWLEDGE_BOOK");
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.locale().component("lab.gui.recipe_book_name"));
            List<Component> lore = new ArrayList<>();
            for (String line : plugin.locale().rawList("lab.gui.recipe_book_lore")) {
                lore.add(Text.component(line));
            }
            meta.lore(lore);
            tagPreview(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component medicineDisplay(String id) {
        Medicine med = plugin.medicines().medicine(id);
        if (med == null) return Text.component("&f" + id);
        // Medicine#name() is the localized display name set at load time.
        return Text.component(med.name());
    }

    private String componentToLegacy(Component component) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().serialize(component);
    }

    /**
     * Stitches a localised template like {@code "&8\u2022 &f{material} &7\u2192 &a{energy}&7 craft(s)"}
     * into a real {@link Component} where {@code {material}} is replaced
     * by a {@link Component#translatable translatable component} (so
     * vanilla materials render in the client's language) and
     * {@code {energy}} is interpolated as plain text.
     */
    private Component buildFuelLine(String template,
                                    Component material,
                                    String energy) {
        // Split the template at {material}; the left half is rendered as
        // legacy text, then the translatable material component is
        // appended, then the right half (with {energy} substituted)
        // becomes plain Component again.
        int idx = template.indexOf("{material}");
        if (idx < 0) {
            return Text.component(template.replace("{energy}", energy));
        }
        Component left = Text.component(template.substring(0, idx)
                .replace("{energy}", energy))
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        Component right = Text.component(template.substring(idx + "{material}".length())
                .replace("{energy}", energy))
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        // Force the material name to render in white. Otherwise the
        // translatable component inherits the dark-grey color from the
        // root parent component (Adventure children inherit parent
        // style, not preceding-sibling style).
        return left.append(material
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                .append(right);
    }

    private Material paneMaterial(String configKey, String def) {
        Material m = Material.matchMaterial(plugin.configs().main()
                .getString(configKey, def));
        return m != null ? m : Material.matchMaterial(def);
    }

    private void tagPreview(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(previewKey, PersistentDataType.STRING, PREVIEW_TAG);
    }

    private boolean isPreview(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return PREVIEW_TAG.equals(item.getItemMeta().getPersistentDataContainer()
                .get(previewKey, PersistentDataType.STRING));
    }

    private ItemStack[] currentInputArray() {
        ItemStack[] arr = new ItemStack[inputSlots.size()];
        for (int i = 0; i < inputSlots.size(); i++) arr[i] = inventory.getItem(inputSlots.get(i));
        return arr;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            // Player-inventory side. Restrict shift-click destinations
            // so players can only push items into ingredient or fuel slots.
            InventoryAction action = e.getAction();
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || action == InventoryAction.COLLECT_TO_CURSOR) {
                ItemStack moved = e.getCurrentItem();
                if (moved != null && moved.getType() != Material.AIR) {
                    if (manager.isLabItem(moved)) {
                        e.setCancelled(true);
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ejectFromForbiddenSlots();
                        syncFromInventory();
                        renderState();
                    });
                }
            }
            return;
        }
        if (filledFiller.contains(slot)) {
            e.setCancelled(true);
            return;
        }
        if (slot == progressSlot) {
            e.setCancelled(true);
            // Shift+RC cancels the queue and refunds ingredients.
            if (e.getClick() == ClickType.SHIFT_RIGHT && lab.activeRecipeId() != null) {
                manager.cancelQueueAndRefund(lab);
                plugin.locale().send(e.getWhoClicked(), "lab.queue_cancelled");
                renderState();
            }
            return;
        }
        if (slot == recipeBookSlot) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player p) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    p.closeInventory();
                    // Pass the lab so the catalogue's "Close" button can
                    // navigate back to this laboratory rather than just
                    // dismissing the menu.
                    new RecipeBookGui(plugin, p, lab).open();
                });
            }
            return;
        }
        if (slot == confirmSlot) {
            e.setCancelled(true);
            handleCraftClick(e);
            return;
        }
        if (slot == resultSlot) {
            // Take crafted items out; reject placing in.
            ItemStack here = inventory.getItem(slot);
            if (isPreview(here)) {
                e.setCancelled(true);
                return;
            }
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                e.setCancelled(true);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                syncFromInventory();
                renderState();
            });
            return;
        }
        if (slot == fuelSlot) {
            ItemStack cursor = e.getCursor();
            // Reject any non-fuel cursor input.
            if (cursor != null && cursor.getType() != Material.AIR
                    && !manager.fuelMaterials().contains(cursor.getType())) {
                e.setCancelled(true);
                return;
            }
            // Reject incoming fuel when the energy reserve is already at
            // its configured cap — otherwise the player would burn fuel
            // for nothing.
            if (cursor != null && cursor.getType() != Material.AIR
                    && manager.isEnergyFull(lab)) {
                e.setCancelled(true);
                if (e.getWhoClicked() instanceof Player p) {
                    plugin.locale().send(p, "lab.energy_full",
                            Map.of("max", String.valueOf(manager.maxEnergyCap())));
                }
                return;
            }
            // Real fuel slot — accept any movement, then sync + burn the
            // item immediately into the energy reserve.
            Bukkit.getScheduler().runTask(plugin, () -> {
                syncFromInventory();
                int credited = manager.consumeFuelImmediate(lab);
                if (credited > 0 && e.getWhoClicked() instanceof Player p) {
                    plugin.locale().send(p, "lab.fuel_credited",
                            Map.of("count", String.valueOf(credited)));
                }
                // Anything left in the fuel slot after the cap was hit
                // is bounced back into the player's inventory.
                ItemStack leftover = inventory.getItem(fuelSlot);
                if (leftover != null && leftover.getType() != Material.AIR
                        && e.getWhoClicked() instanceof Player p) {
                    Map<Integer, ItemStack> overflow =
                            p.getInventory().addItem(leftover.clone());
                    inventory.setItem(fuelSlot, null);
                    lab.setFuel(null);
                    for (ItemStack drop : overflow.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                    plugin.locale().send(p, "lab.energy_full",
                            Map.of("max", String.valueOf(manager.maxEnergyCap())));
                }
                renderState();
            });
            return;
        }
        if (inputSlots.contains(slot)) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && isPreview(cursor)) {
                e.setCancelled(true);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                syncFromInventory();
                renderState();
            });
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        for (int slot : e.getRawSlots()) {
            if (slot >= inventory.getSize()) continue;
            if (slot == progressSlot || slot == confirmSlot || slot == resultSlot
                    || slot == recipeBookSlot || filledFiller.contains(slot)) {
                e.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            syncFromInventory();
            renderState();
        });
    }

    private void handleCraftClick(InventoryClickEvent e) {
        ItemStack[] inputs = currentInputArray();
        LabRecipe recipe = manager.recipes().match(inputs, plugin.medicines().factory());
        if (recipe == null) {
            plugin.locale().send(e.getWhoClicked(), "lab.no_recipe");
            return;
        }
        if (lab.activeRecipeId() != null
                && !recipe.resultMedicineId().equals(lab.activeRecipeId())) {
            plugin.locale().send(e.getWhoClicked(), "lab.queue_full");
            return;
        }
        List<ItemStack> snapshot = new ArrayList<>();
        if (!recipe.consumeAndSnapshot(inputs, plugin.medicines().factory(), snapshot)) {
            plugin.locale().send(e.getWhoClicked(), "lab.no_recipe");
            return;
        }
        for (int i = 0; i < inputSlots.size(); i++) {
            inventory.setItem(inputSlots.get(i), inputs[i]);
        }
        if (lab.activeRecipeId() == null) {
            lab.setActiveRecipeId(recipe.resultMedicineId());
            lab.setActiveSecondsRemaining(recipe.craftTimeSeconds());
        }
        lab.setQueue(lab.queue() + 1);
        lab.pendingCraftSnapshots().add(snapshot);
        syncFromInventory();
        renderState();
    }

    private void ejectFromForbiddenSlots() {
        if (inventory == null) return;
        Set<Integer> forbidden = new HashSet<>(filledFiller);
        forbidden.add(progressSlot);
        forbidden.add(confirmSlot);
        forbidden.add(recipeBookSlot);
        for (int slot : forbidden) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null) continue;
            if (isPreview(stack)) continue;
            inventory.setItem(slot, null);
            for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
                if (viewer instanceof Player vp) {
                    Map<Integer, ItemStack> leftover = vp.getInventory().addItem(stack);
                    for (ItemStack drop : leftover.values()) {
                        vp.getWorld().dropItemNaturally(vp.getLocation(), drop);
                    }
                    break;
                }
            }
        }
        // Reject non-fuel items that ended up in the fuel slot.
        ItemStack fuel = inventory.getItem(fuelSlot);
        if (fuel != null && !isPreview(fuel)
                && !manager.fuelMaterials().contains(fuel.getType())) {
            inventory.setItem(fuelSlot, null);
            for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
                if (viewer instanceof Player vp) {
                    Map<Integer, ItemStack> leftover = vp.getInventory().addItem(fuel);
                    for (ItemStack drop : leftover.values()) {
                        vp.getWorld().dropItemNaturally(vp.getLocation(), drop);
                    }
                    break;
                }
            }
        }
    }

    /** Pull current GUI state into the laboratory model. */
    public void syncFromInventory() {
        if (inventory == null) return;
        // Inputs.
        lab.inputs().clear();
        for (int i = 0; i < inputSlots.size(); i++) {
            lab.inputs().add(inventory.getItem(inputSlots.get(i)));
        }
        // Result.
        ItemStack result = inventory.getItem(resultSlot);
        if (result == null || isPreview(result)) {
            lab.setResult(null);
        } else {
            lab.setResult(result);
        }
        // Fuel slot — items here are immediately burnt into energy.
        ItemStack fuel = inventory.getItem(fuelSlot);
        if (fuel == null || isPreview(fuel)) {
            lab.setFuel(null);
        } else if (!manager.fuelMaterials().contains(fuel.getType())) {
            // Not a fuel material — kick it back to the player.
            inventory.setItem(fuelSlot, null);
            for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
                if (viewer instanceof Player vp) {
                    Map<Integer, ItemStack> leftover = vp.getInventory().addItem(fuel);
                    for (ItemStack drop : leftover.values()) {
                        vp.getWorld().dropItemNaturally(vp.getLocation(), drop);
                    }
                    break;
                }
            }
            lab.setFuel(null);
        } else {
            lab.setFuel(fuel);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        syncFromInventory();
        // Ensure any fuel that the player closed-with gets consumed too.
        manager.consumeFuelImmediate(lab);
        if (inventory.getViewers().size() <= 1) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
            manager.openGuis().remove(lab.location());
        }
    }

    public void closeAll() {
        if (inventory == null) return;
        syncFromInventory();
        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
            if (viewer instanceof Player p) p.closeInventory();
        }
        HandlerList.unregisterAll(this);
        listenerRegistered = false;
    }
}
