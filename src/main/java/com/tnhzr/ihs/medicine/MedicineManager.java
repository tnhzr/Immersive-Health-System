package com.tnhzr.ihs.medicine;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MedicineManager implements Module {

    private final ImmersiveHealthSystem plugin;
    private final Map<String, Medicine> medicines = new LinkedHashMap<>();
    private final MedicineItemFactory factory;

    public MedicineManager(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
        // Factory is read-only and side-effect free; initialise eagerly so
        // other modules (e.g. laboratory) can use it even when this module
        // is disabled in config.
        this.factory = new MedicineItemFactory(plugin);
    }

    @Override public String id() { return "medicine"; }

    @Override
    public void enable() {
        load();
        Bukkit.getPluginManager().registerEvents(new MedicineConsumeListener(plugin, this), plugin);
        Bukkit.getPluginManager().registerEvents(new MilkNerfListener(plugin), plugin);
    }

    @Override
    public void disable() {
        // Listeners are unregistered automatically when the plugin disables.
    }

    /** Re-parses the medicine catalog from the latest YAML on disk. */
    public void reload() {
        load();
    }

    private void load() {
        medicines.clear();
        ConfigurationSection root = plugin.configs().medicines().getConfigurationSection("medicines");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                medicines.put(id, parse(id, s));
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to parse medicine '" + id + "': " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + medicines.size() + " medicines.");
    }

    private Medicine parse(String id, ConfigurationSection s) {
        Material mat = Material.matchMaterial(s.getString("material", "GOLDEN_APPLE"));
        if (mat == null) mat = Material.GOLDEN_APPLE;
        int cmd = s.getInt("custom_model_data", 0);
        // Vanilla 1.21.4+ item-model component. Default convention is
        // "ihs:item/<id>" so each medicine that has a matching texture
        // bundled in the resourcepack picks it up automatically without
        // any per-medicine config.
        String itemModel = s.getString("item_model", "ihs:item/" + id);
        String name = s.getString("name", id);

        ConfigurationSection data = s.getConfigurationSection("medicine_data");
        Medicine.Type type = Medicine.Type.CURE;
        if (data != null) {
            String t = data.getString("type", "cure").toLowerCase();
            type = switch (t) {
                case "effect_clear" -> Medicine.Type.EFFECT_CLEAR;
                case "buff" -> Medicine.Type.BUFF;
                default -> Medicine.Type.CURE;
            };
        }

        return new Medicine(
                id, mat, cmd, itemModel, name, s.getStringList("lore"),
                type,
                data != null ? data.getStringList("cures_infections") : null,
                data != null ? data.getInt("heal_points", 0) : 0,
                data != null ? data.getInt("daily_limit", 1) : 1,
                data != null ? data.getStringList("clears_potion_effects") : null,
                data != null ? data.getStringList("apply_potion_effects") : null
        );
    }

    public Medicine medicine(String id) { return medicines.get(id); }
    public Map<String, Medicine> medicines() { return medicines; }
    /** Convenience alias of {@link #medicines()}. */
    public Map<String, Medicine> all() { return medicines; }
    public MedicineItemFactory factory() { return factory; }
}
