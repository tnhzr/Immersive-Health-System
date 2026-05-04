package com.tnhzr.ihs.config;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class ConfigManager {

    private final ImmersiveHealthSystem plugin;

    private FileConfiguration infections;
    private FileConfiguration medicines;
    private FileConfiguration labRecipes;

    public ConfigManager(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.reloadConfig();
        migrateLegacyValues();
        migrateLabRecipesFile();
        migrateLocaleFiles();
        this.infections = loadResource("infections.yml");
        this.medicines  = loadResource("medicines.yml");
        this.labRecipes = loadResource("lab_recipes.yml");
    }

    /**
     * One-shot migration for users upgrading from older config layouts.
     * Currently handles:
     *   <li>{@code laboratory.block_material: CRAFTING_TABLE} \u2192 {@code BLAST_FURNACE}
     *       (old default rendered the lab as a wooden bench)</li>
     *   <li>Adds {@code laboratory.fuels.max_energy} default when absent
     *       (so existing servers pick up the configurable cap)</li>
     */
    private void migrateLegacyValues() {
        FileConfiguration cfg = plugin.getConfig();
        boolean dirty = false;
        String blockMaterial = cfg.getString("laboratory.block_material", "");
        if ("CRAFTING_TABLE".equalsIgnoreCase(blockMaterial)) {
            cfg.set("laboratory.block_material", "BLAST_FURNACE");
            plugin.getLogger().info(
                    "Migrated laboratory.block_material CRAFTING_TABLE \u2192 BLAST_FURNACE.");
            dirty = true;
        }
        if (!cfg.isSet("laboratory.fuels.max_energy")) {
            cfg.set("laboratory.fuels.max_energy", 256);
            plugin.getLogger().info(
                    "Added missing laboratory.fuels.max_energy = 256 default.");
            dirty = true;
        }
        if (!cfg.isSet("laboratory.item_model")) {
            cfg.set("laboratory.item_model", "ihs:block/laboratory");
            dirty = true;
        }
        // Force-migrate the laboratory GUI slot map. Existing installs may
        // have a stale fuel_slot value from earlier layouts; pinning
        // every slot to the canonical blueprint guarantees the rendered
        // GUI matches the chest layout the user expects.
        // Blueprint reference (6×9 double chest, 0-indexed):
        //   row 2 col 5 → slot 23  (progress arrow)
        //   row 2 col 7 → slot 25  (result)
        //   row 3 col 7 → slot 34  (confirm)
        //   row 4 col 5 → slot 41  (fuel — one slot vertically below progress)
        //   row 5 col 0 → slot 45  (recipe book)
        // Input slots stay [10,11,12,19,20,21,28,29,30] (3×3 grid top-left).
        java.util.Map<String, Object> guiSlots = new java.util.LinkedHashMap<>();
        guiSlots.put("progress_slot", 23);
        guiSlots.put("result_slot", 25);
        guiSlots.put("confirm_slot", 34);
        guiSlots.put("fuel_slot", 41);
        guiSlots.put("recipe_book_slot", 45);
        for (var entry : guiSlots.entrySet()) {
            String path = "laboratory.gui." + entry.getKey();
            int expected = (Integer) entry.getValue();
            int current = cfg.getInt(path, expected);
            if (current != expected) {
                plugin.getLogger().info("Migrated " + path + ": " + current
                        + " \u2192 " + expected);
                cfg.set(path, expected);
                dirty = true;
            }
        }
        if (dirty) plugin.saveConfig();
    }

    /**
     * Rewrites legacy ingredient identifiers in the user's on-disk
     * {@code lab_recipes.yml} so existing installs pick up the
     * canonical material names without forcing a manual edit.
     *
     * <ul>
     *   <li>{@code vanilla:resin_brick} \u2192 {@code vanilla:resin_clump}
     *       (fixes the "smoljanoy kirpich" name appearing in the recipe
     *       book; the user's spec always referenced the resin clump)</li>
     *   <li>{@code vanilla:eyeblossom} \u2192 {@code vanilla:open_eyeblossom}
     *       (the bare {@code eyeblossom} material does not exist in
     *       1.21.4+; sleeping pills should use the OPEN variant per the
     *       user's spec)</li>
     * </ul>
     */
    private void migrateLabRecipesFile() {
        File file = new File(plugin.getDataFolder(), "lab_recipes.yml");
        if (!file.exists()) return;
        try {
            String contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            // Use a regex with negative lookbehind so we don't double-replace
            // already-migrated identifiers (e.g. "vanilla:closed_eyeblossom"
            // or "vanilla:open_eyeblossom" stay intact).
            String migrated = contents
                    .replace("vanilla:resin_brick", "vanilla:resin_clump")
                    .replaceAll("(?<![a-zA-Z_])vanilla:eyeblossom",
                            "vanilla:open_eyeblossom");
            if (!migrated.equals(contents)) {
                Files.writeString(file.toPath(), migrated, StandardCharsets.UTF_8);
                plugin.getLogger().info(
                        "Migrated legacy ingredient identifiers in lab_recipes.yml.");
            }
        } catch (IOException ex) {
            plugin.getLogger().warning(
                    "Failed to migrate lab_recipes.yml: " + ex.getMessage());
        }
    }

    /**
     * Refreshes recently-added keys in the user's locale bundles so
     * existing installs pick up the standardised prefix and the
     * energy-cap message without erasing any of their custom
     * translations.
     */
    private void migrateLocaleFiles() {
        for (String lang : List.of("ru", "en")) {
            String resource = "lang/messages_" + lang + ".yml";
            File file = new File(plugin.getDataFolder(), resource);
            if (!file.exists()) continue;
            FileConfiguration on = YamlConfiguration.loadConfiguration(file);
            FileConfiguration bundled = bundled(resource);
            if (bundled == null) continue;
            boolean dirty = false;
            // Always synchronise the prefix — earlier alphas shipped no
            // prefix at all and some pre-release tests left a literal
            // "[IHS] " prefix in user bundles.
            String currentPrefix = on.getString("prefix", "");
            String newPrefix = bundled.getString("prefix", "&aIHS &8\u00bb &r");
            if (!newPrefix.equals(currentPrefix)) {
                on.set("prefix", newPrefix);
                dirty = true;
            }
            // Add any new keys that did not exist in older bundles.
            for (String key : List.of(
                    "lab.energy_full",
                    "lab.fuel_credited")) {
                if (!on.isSet(key) && bundled.isSet(key)) {
                    on.set(key, bundled.getString(key));
                    dirty = true;
                }
            }
            if (dirty) {
                try {
                    on.save(file);
                    plugin.getLogger().info(
                            "Synchronised prefix/energy-cap keys in " + resource + ".");
                } catch (IOException ex) {
                    plugin.getLogger().warning("Failed to migrate "
                            + resource + ": " + ex.getMessage());
                }
            }
        }
    }

    private FileConfiguration bundled(String resource) {
        InputStream in = plugin.getResource(resource);
        if (in == null) return null;
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private FileConfiguration loadResource(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        InputStream defaults = plugin.getResource(name);
        if (defaults != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
        return cfg;
    }

    public void save(String name, FileConfiguration cfg) {
        try {
            cfg.save(new File(plugin.getDataFolder(), name));
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save " + name + ": " + ex.getMessage());
        }
    }

    public FileConfiguration main() { return plugin.getConfig(); }
    public FileConfiguration infections() { return infections; }
    public FileConfiguration medicines() { return medicines; }
    public FileConfiguration labRecipes() { return labRecipes; }

    /** Convenience accessor for the laboratory section. */
    public ConfigurationSection laboratory() {
        return main().getConfigurationSection("laboratory");
    }
}
