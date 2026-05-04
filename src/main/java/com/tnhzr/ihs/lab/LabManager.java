package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.module.Module;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LabManager implements Module {

    public static final String LAB_TAG_VALUE = "laboratory";

    private final ImmersiveHealthSystem plugin;
    private final LabRecipeManager recipes;
    private final Map<String, Laboratory> laboratories = new HashMap<>();
    private final Map<Location, LabGui> openGuis = new HashMap<>();
    private File storageFile;
    private BukkitTask tickTask;
    private NamespacedKey cachedLabItemKey;
    private NamespacedKey cachedLabRecipeKey;
    private Map<Material, Integer> cachedFuelMap = Collections.emptyMap();

    public LabManager(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
        this.recipes = new LabRecipeManager(plugin);
        this.cachedLabItemKey = new NamespacedKey(plugin, "lab_block_id");
        this.cachedLabRecipeKey = new NamespacedKey(plugin, "laboratory_block");
    }

    @Override public String id() { return "laboratory"; }

    @Override
    public void enable() {
        recipes.load();
        loadFromDisk();
        registerVanillaRecipe();
        Bukkit.getPluginManager().registerEvents(new LabBlockListener(plugin, this), plugin);
        // Make sure already-online players (and future joiners) see the
        // lab recipe in their vanilla recipe book.
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                e.getPlayer().discoverRecipe(cachedLabRecipeKey);
            }
        }, plugin);
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            online.discoverRecipe(cachedLabRecipeKey);
        }
        cachedFuelMap = computeFuelMap();
        startTicker();
        // Schedule a post-load migration that converts every existing
        // lab block to NOTE_BLOCK[note=0,instrument=harp,powered=false]
        // — see migrateExistingLabBlocks for the full rationale.
        Bukkit.getScheduler().runTaskLater(plugin,
                this::migrateExistingLabBlocks, 40L);
    }

    /** Re-registers the vanilla shaped recipe and refreshes cached values. */
    public void onConfigReloaded() {
        Bukkit.removeRecipe(cachedLabRecipeKey);
        registerVanillaRecipe();
        cachedFuelMap = computeFuelMap();
        migrateExistingLabBlocks();
    }

    /**
     * Walks every loaded laboratory and forces its physical block to be
     * a NOTE_BLOCK in the exact state our resourcepack overrides:
     *   note_block[note=0,instrument=harp,powered=false]
     *
     * This covers two real-world cases:
     *   1. Servers that placed labs as BLAST_FURNACE / CRAFTING_TABLE
     *      under the old block-binding system never had their existing
     *      labs converted — players couldn't see the custom texture
     *      and break sounds were vanilla wood/stone.
     *   2. Worlds where the chunk hadn't loaded when the place event
     *      fired (e.g. cross-server teleports) sometimes ended up with
     *      the lab in {@link Laboratory laboratories.yml} but the
     *      physical block reverted to the original material.
     *
     * Idempotent: if the block is already in the right state nothing
     * is sent to clients.
     */
    public void migrateExistingLabBlocks() {
        int converted = 0;
        for (Laboratory lab : laboratories.values()) {
            World world = lab.location().getWorld();
            if (world == null) continue;
            if (!world.isChunkLoaded(lab.location().getBlockX() >> 4,
                    lab.location().getBlockZ() >> 4)) continue;
            org.bukkit.block.Block block = world.getBlockAt(lab.location());
            boolean alreadyCorrect = block.getType() == Material.NOTE_BLOCK
                    && block.getBlockData() instanceof org.bukkit.block.data.type.NoteBlock nb
                    && nb.getInstrument() == org.bukkit.Instrument.PIANO
                    && nb.getNote().getId() == 0
                    && !nb.isPowered();
            if (alreadyCorrect) continue;
            LabBlockListener.applyLabBlockState(block);
            converted++;
        }
        if (converted > 0) {
            plugin.getLogger().info("Migrated " + converted
                    + " laboratory block(s) to note_block[note=0,instrument=harp,powered=false]");
        }
    }

    @Override
    public void disable() {
        if (tickTask != null) tickTask.cancel();
        // Close any open GUIs first to flush state.
        for (LabGui gui : new ArrayList<>(openGuis.values())) gui.closeAll();
        saveToDisk();
        Bukkit.removeRecipe(cachedLabRecipeKey);
    }

    public ImmersiveHealthSystem plugin() { return plugin; }
    public LabRecipeManager recipes() { return recipes; }
    public Map<String, Laboratory> laboratories() { return laboratories; }
    public Map<Location, LabGui> openGuis() { return openGuis; }

    public NamespacedKey labItemKey() {
        return cachedLabItemKey;
    }

    public Laboratory at(Location loc) {
        Location key = loc.toBlockLocation();
        for (Laboratory l : laboratories.values()) {
            Location lk = l.location().toBlockLocation();
            if (lk.getWorld().equals(key.getWorld())
                    && lk.getBlockX() == key.getBlockX()
                    && lk.getBlockY() == key.getBlockY()
                    && lk.getBlockZ() == key.getBlockZ()) {
                return l;
            }
        }
        return null;
    }

    public Laboratory createAt(Location loc) {
        Laboratory lab = new Laboratory(loc.clone());
        laboratories.put(lab.key(), lab);
        return lab;
    }

    public void remove(Laboratory lab) {
        laboratories.remove(lab.key());
        // Close any in-flight GUI for this lab and force every viewer
        // to close their inventory. Without this, a recipe-book or
        // lab-GUI viewer holds a stale Laboratory reference and could
        // re-take items that were already dropped on break (dupe).
        LabGui open = openGuis.remove(lab.location());
        if (open != null) open.closeAll();
        // Wipe transient state on the Laboratory object itself so any
        // remaining stale reference (e.g. a RecipeBookGui still pointing
        // at this lab) cannot resurrect already-dropped items.
        lab.clearAll();
    }

    public ItemStack createLabItem() {
        Material mat = Material.matchMaterial(plugin.configs().main()
                .getString("laboratory.block_material", "BLAST_FURNACE"));
        if (mat == null) mat = Material.BLAST_FURNACE;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.locale().component("lab.item_name")
                    .decoration(TextDecoration.ITALIC, false));
            int cmd = plugin.configs().main().getInt("laboratory.item_custom_model_data", 9001);
            if (cmd > 0) meta.setCustomModelData(cmd);
            // 1.21.4+ item-model component — binds the placeable lab item
            // directly to the resourcepack model without any CraftEngine
            // binding. Pack ships assets/ihs/models/block/laboratory.json.
            String itemModel = plugin.configs().main()
                    .getString("laboratory.item_model", "ihs:block/laboratory");
            if (itemModel != null && !itemModel.isBlank()) {
                NamespacedKey modelKey = NamespacedKey.fromString(itemModel);
                if (modelKey != null) {
                    try {
                        meta.setItemModel(modelKey);
                    } catch (NoSuchMethodError ignored) {
                        // Older API — silently fall back.
                    }
                }
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(labItemKey(), PersistentDataType.STRING, LAB_TAG_VALUE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Maximum energy a single laboratory can store in its reserve. Fuel
     * insertions that would exceed this cap are rejected so the player
     * never burns surplus mossblocks for nothing.
     */
    public int maxEnergyCap() {
        return Math.max(1, plugin.configs().main()
                .getInt("laboratory.fuels.max_energy", 256));
    }

    public boolean isLabItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        String v = stack.getItemMeta().getPersistentDataContainer()
                .get(labItemKey(), PersistentDataType.STRING);
        return LAB_TAG_VALUE.equals(v);
    }

    private void registerVanillaRecipe() {
        ItemStack result = createLabItem();
        ShapedRecipe recipe = new ShapedRecipe(cachedLabRecipeKey, result);
        recipe.shape("III", "FBC", "III");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('F', Material.FURNACE);
        recipe.setIngredient('B', Material.BREWING_STAND);
        recipe.setIngredient('C', Material.CAULDRON);
        try {
            Bukkit.addRecipe(recipe);
        } catch (IllegalStateException ignored) {
            // Recipe with this key already registered (likely after /reload).
        }
    }

    private void loadFromDisk() {
        storageFile = new File(plugin.getDataFolder(), "laboratories.yml");
        if (!storageFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection root = cfg.getConfigurationSection("labs");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            try {
                Laboratory lab = Laboratory.load(sec);
                if (lab != null) laboratories.put(lab.key(), lab);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load laboratory '" + key + "': " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + laboratories.size() + " laboratories.");
    }

    public void saveToDisk() {
        if (storageFile == null) {
            storageFile = new File(plugin.getDataFolder(), "laboratories.yml");
        }
        YamlConfiguration cfg = new YamlConfiguration();
        int i = 0;
        for (Laboratory lab : laboratories.values()) {
            lab.save(cfg.createSection("labs.lab_" + (i++)));
        }
        try {
            cfg.save(storageFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save laboratories: " + ex.getMessage());
        }
    }

    private void startTicker() {
        long period = plugin.configs().main().getLong("laboratory.tick_period_ticks", 20L);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> tickAll((int) period), period, period);
    }

    private void tickAll(int periodTicks) {
        for (Laboratory lab : laboratories.values()) {
            tickOne(lab, periodTicks);
        }
        for (LabGui gui : openGuis.values()) gui.refresh();
    }

    private void tickOne(Laboratory lab, int periodTicks) {
        World world = lab.location().getWorld();
        if (world == null) return;
        if (!world.isChunkLoaded(lab.location().getBlockX() >> 4,
                lab.location().getBlockZ() >> 4)) return;

        // Drain any fuel items dropped into the slot — instant burn into
        // the energy reserve, mirroring the user's spec ("сразу же
        // пропадает сгорая и уходя во внутренние системы лаборатории").
        consumeFuelSlot(lab);

        if (lab.queue() <= 0) return;
        if (lab.activeRecipeId() == null) {
            lab.setQueue(0);
            return;
        }
        if (lab.fuelEnergy() <= 0) {
            // Out of energy — stay paused until refuelled.
            return;
        }
        // Advance the synth timer (seconds approximated as 20-tick blocks).
        int newSec = Math.max(0,
                lab.activeSecondsRemaining() - Math.max(1, periodTicks / 20));
        lab.setActiveSecondsRemaining(newSec);
        if (newSec > 0) return;

        LabRecipe recipe = recipes.recipes().get(lab.activeRecipeId());
        if (recipe == null) {
            lab.setQueue(0);
            lab.setActiveRecipeId(null);
            lab.pendingCraftSnapshots().clear();
            return;
        }
        com.tnhzr.ihs.medicine.Medicine med =
                plugin.medicines().medicine(recipe.resultMedicineId());
        if (med == null) {
            plugin.getLogger().warning("Lab recipe '" + lab.activeRecipeId()
                    + "' references unknown medicine '" + recipe.resultMedicineId()
                    + "'. Aborting queue.");
            lab.setQueue(0);
            lab.setActiveRecipeId(null);
            lab.setActiveSecondsRemaining(0);
            lab.pendingCraftSnapshots().clear();
            return;
        }
        ItemStack result = plugin.medicines().factory().create(med, 1);
        if (!placeOutput(lab, result)) {
            // Result slot is full — keep the timer at 0 and try again next
            // tick. No fuel is consumed because the craft did not finish.
            lab.setActiveSecondsRemaining(0);
            return;
        }
        // One craft finished: consume one energy unit and advance queue.
        lab.addFuelEnergy(-1);
        lab.setQueue(lab.queue() - 1);
        if (!lab.pendingCraftSnapshots().isEmpty()) {
            lab.pendingCraftSnapshots().remove(0);
        }
        if (lab.queue() > 0) {
            lab.setActiveSecondsRemaining(recipe.craftTimeSeconds());
        } else {
            lab.setActiveRecipeId(null);
            lab.setActiveSecondsRemaining(0);
        }
    }

    /**
     * Drains the laboratory's fuel slot into its energy reserve. Each
     * accepted item contributes {@code energy_per_item} crafts; the
     * stack is consumed only up to the energy cap — items that would
     * push the reserve past the cap are left in the slot so the GUI can
     * eject them back into the player's inventory on close.
     */
    private void consumeFuelSlot(Laboratory lab) {
        ItemStack fuel = lab.fuel();
        if (fuel == null || fuel.getAmount() <= 0) return;
        Integer energyPerItem = cachedFuelMap.get(fuel.getType());
        if (energyPerItem == null || energyPerItem <= 0) return;
        int cap = maxEnergyCap();
        int free = Math.max(0, cap - lab.fuelEnergy());
        if (free <= 0) return;
        int affordableUnits = Math.min(fuel.getAmount(), free / energyPerItem);
        if (affordableUnits <= 0) return;
        int credited = affordableUnits * energyPerItem;
        lab.addFuelEnergy(credited);
        int leftover = fuel.getAmount() - affordableUnits;
        if (leftover <= 0) {
            lab.setFuel(null);
        } else {
            ItemStack remaining = fuel.clone();
            remaining.setAmount(leftover);
            lab.setFuel(remaining);
        }
    }

    /**
     * Public helper — used by the GUI when the player drops a fuel item
     * into the fuel slot so we can credit the energy reserve immediately
     * (without waiting for the tick loop). Returns the number of energy
     * units credited; any leftover stack stays in the lab's fuel slot.
     */
    public int consumeFuelImmediate(Laboratory lab) {
        int before = lab.fuelEnergy();
        consumeFuelSlot(lab);
        return lab.fuelEnergy() - before;
    }

    /** True when the laboratory's energy reserve is at or above its cap. */
    public boolean isEnergyFull(Laboratory lab) {
        return lab.fuelEnergy() >= maxEnergyCap();
    }

    /**
     * Cancels every queued craft, refunds the captured ingredient
     * snapshots into the laboratory's input slots (overflow drops near
     * the lab block), and clears the active recipe.
     */
    public void cancelQueueAndRefund(Laboratory lab) {
        if (lab.queue() <= 0 && lab.pendingCraftSnapshots().isEmpty()) return;
        World world = lab.location().getWorld();
        for (List<ItemStack> snapshot : lab.pendingCraftSnapshots()) {
            for (ItemStack consumed : snapshot) {
                if (consumed == null) continue;
                if (!mergeIntoInputs(lab, consumed)) {
                    if (world != null) {
                        world.dropItemNaturally(lab.location().add(0.5, 1.0, 0.5),
                                consumed.clone());
                    }
                }
            }
        }
        lab.pendingCraftSnapshots().clear();
        lab.setQueue(0);
        lab.setActiveRecipeId(null);
        lab.setActiveSecondsRemaining(0);
    }

    /** Stack {@code stack} into the lab's input list. Returns false if no space. */
    public boolean mergeIntoInputs(Laboratory lab, ItemStack stack) {
        List<ItemStack> inputs = lab.inputs();
        // Try stacking with same-typed items first.
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack here = inputs.get(i);
            if (here == null || here.getType() == Material.AIR) continue;
            if (here.isSimilar(stack)) {
                int max = here.getMaxStackSize();
                int total = here.getAmount() + stack.getAmount();
                if (total <= max) {
                    here.setAmount(total);
                    return true;
                }
                here.setAmount(max);
                stack.setAmount(total - max);
            }
        }
        // Then drop into the first empty slot.
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i) == null || inputs.get(i).getType() == Material.AIR) {
                inputs.set(i, stack.clone());
                return true;
            }
        }
        return false;
    }

    /** Places one freshly crafted item into the laboratory's result slot. */
    private boolean placeOutput(Laboratory lab, ItemStack result) {
        ItemStack current = lab.result();
        if (current == null || current.getType() == Material.AIR) {
            lab.setResult(result);
            return true;
        }
        if (current.isSimilar(result)) {
            int total = current.getAmount() + result.getAmount();
            int max = current.getMaxStackSize();
            if (total <= max) {
                current.setAmount(total);
                return true;
            }
        }
        return false;
    }

    private Map<Material, Integer> computeFuelMap() {
        Map<Material, Integer> out = new HashMap<>();
        ConfigurationSection fuels = plugin.configs().main()
                .getConfigurationSection("laboratory.fuels");
        if (fuels != null) {
            for (String k : fuels.getKeys(false)) {
                Material m = Material.matchMaterial(k);
                if (m == null) continue;
                int energy;
                if (fuels.isConfigurationSection(k)) {
                    energy = fuels.getInt(k + ".energy_per_item", 1);
                } else {
                    // Legacy: bare integer was "ticks per item". Approximate
                    // 1 craft per 200 ticks (10 seconds).
                    int legacyTicks = fuels.getInt(k, 200);
                    energy = Math.max(1, legacyTicks / 200);
                }
                if (energy > 0) out.put(m, energy);
            }
        }
        if (out.isEmpty()) {
            out.put(Material.MOSS_BLOCK, 1);
            out.put(Material.PALE_MOSS_BLOCK, 2);
        }
        return out;
    }

    /** Materials currently accepted in the fuel slot. */
    public List<Material> fuelMaterials() {
        return new ArrayList<>(cachedFuelMap.keySet());
    }

    public int fuelEnergyPerItem(Material material) {
        return cachedFuelMap.getOrDefault(material, 0);
    }
}
