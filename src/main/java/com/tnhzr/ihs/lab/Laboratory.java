package com.tnhzr.ihs.lab;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Laboratory {

    private final Location location;

    /** Live ingredient slots (size matches GUI input_slots). */
    private List<ItemStack> inputs = new ArrayList<>();
    /** Single result/output slot. */
    private ItemStack result;
    /** Single live fuel slot (consumed instantly into {@link #fuelEnergy}). */
    private ItemStack fuel;
    /** Stored "energy units": 1 unit = 1 crafted medicine. */
    private int fuelEnergy;

    /** Recipe currently in production (or {@code null} when idle). */
    private String activeRecipeId;
    /** Number of crafts still pending (including the active one). */
    private int queue;
    /** Seconds remaining for the currently-being-crafted item. */
    private int activeSecondsRemaining;
    /**
     * Snapshot of the ingredients consumed by every queued craft, in
     * arrival order. Used by {@code Shift+RC} on the progress slot to
     * refund the lab back to the player when a synthesis is cancelled.
     * Each entry is the recipe's flat ingredient list (one ItemStack per
     * recipe ingredient, already amount-sized).
     */
    private final List<List<ItemStack>> pendingCraftSnapshots = new ArrayList<>();

    public Laboratory(Location location) {
        this.location = location;
    }

    public Location location() { return location; }
    public List<ItemStack> inputs() { return inputs; }
    public ItemStack result() { return result; }
    public void setResult(ItemStack result) { this.result = result; }
    public ItemStack fuel() { return fuel; }
    public void setFuel(ItemStack fuel) { this.fuel = fuel; }
    public int fuelEnergy() { return fuelEnergy; }
    public void setFuelEnergy(int v) { this.fuelEnergy = Math.max(0, v); }
    public void addFuelEnergy(int delta) { this.fuelEnergy = Math.max(0, fuelEnergy + delta); }
    public String activeRecipeId() { return activeRecipeId; }
    public void setActiveRecipeId(String id) { this.activeRecipeId = id; }
    public int queue() { return queue; }
    public void setQueue(int q) { this.queue = q; }
    public int activeSecondsRemaining() { return activeSecondsRemaining; }
    public void setActiveSecondsRemaining(int v) { this.activeSecondsRemaining = v; }
    public List<List<ItemStack>> pendingCraftSnapshots() { return pendingCraftSnapshots; }

    /**
     * Wipes every transient slot/state on this laboratory. Used by
     * {@link LabManager#remove(Laboratory)} after a lab block is broken
     * so any still-open GUI references to this object cannot pull
     * already-dropped items back into a player's inventory (item dupe).
     */
    public void clearAll() {
        for (int i = 0; i < inputs.size(); i++) inputs.set(i, null);
        result = null;
        fuel = null;
        fuelEnergy = 0;
        activeRecipeId = null;
        queue = 0;
        activeSecondsRemaining = 0;
        pendingCraftSnapshots.clear();
    }

    public String key() {
        return location.getWorld().getUID() + "/" + location.getBlockX()
                + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public void save(ConfigurationSection sec) {
        sec.set("world", location.getWorld().getUID().toString());
        sec.set("x", location.getBlockX());
        sec.set("y", location.getBlockY());
        sec.set("z", location.getBlockZ());
        sec.set("inputs", inputs);
        sec.set("result", result);
        sec.set("fuel", fuel);
        sec.set("fuel_energy", fuelEnergy);
        sec.set("active_recipe", activeRecipeId);
        sec.set("queue", queue);
        sec.set("active_seconds", activeSecondsRemaining);
        // Pending craft snapshots: persist each as a sub-list of ItemStacks.
        List<List<ItemStack>> snapshots = new ArrayList<>(pendingCraftSnapshots);
        sec.set("pending_snapshots", snapshots);
    }

    @SuppressWarnings("unchecked")
    public static Laboratory load(ConfigurationSection sec) {
        java.util.UUID worldId = java.util.UUID.fromString(sec.getString("world"));
        World world = Bukkit.getWorld(worldId);
        if (world == null) return null;
        Location loc = new Location(world,
                sec.getInt("x"), sec.getInt("y"), sec.getInt("z"));
        Laboratory lab = new Laboratory(loc);
        lab.inputs = new ArrayList<>((List<ItemStack>) sec.getList("inputs", new ArrayList<>()));
        lab.result = sec.getItemStack("result");
        // Backwards-compat: legacy schema used a list of "outputs" + a single
        // "fuel_ticks" counter. Migrate them so existing servers don't lose
        // crafted items or fuel reserves on the first start after upgrade.
        if (lab.result == null) {
            List<ItemStack> legacyOuts = (List<ItemStack>) sec.getList("outputs", null);
            if (legacyOuts != null) {
                for (ItemStack stack : legacyOuts) {
                    if (stack != null && stack.getType() != org.bukkit.Material.AIR) {
                        lab.result = stack;
                        break;
                    }
                }
            }
        }
        lab.fuel = sec.getItemStack("fuel");
        if (sec.contains("fuel_energy")) {
            lab.fuelEnergy = sec.getInt("fuel_energy", 0);
        } else {
            // Legacy "fuel_ticks" — 200 ticks ~= 1 craft unit baseline.
            int ticks = sec.getInt("fuel_ticks", 0);
            lab.fuelEnergy = ticks > 0 ? Math.max(1, ticks / 200) : 0;
        }
        lab.activeRecipeId = sec.getString("active_recipe");
        lab.queue = sec.getInt("queue", 0);
        lab.activeSecondsRemaining = sec.getInt("active_seconds", 0);
        List<?> rawSnapshots = sec.getList("pending_snapshots", new ArrayList<>());
        for (Object o : rawSnapshots) {
            if (o instanceof List<?> rawList) {
                List<ItemStack> snapshot = new ArrayList<>();
                for (Object e : rawList) {
                    if (e instanceof ItemStack item) snapshot.add(item);
                }
                if (!snapshot.isEmpty()) lab.pendingCraftSnapshots.add(snapshot);
            }
        }
        return lab;
    }
}
