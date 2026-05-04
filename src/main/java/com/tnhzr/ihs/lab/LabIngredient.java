package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.medicine.MedicineItemFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed laboratory ingredient. Examples:
 *   vanilla:honeycomb:1
 *   custom:uranium_shard:1
 *   vanilla:coal|vanilla:charcoal:1   (matches either material)
 */
public final class LabIngredient {

    public enum Source { VANILLA, CUSTOM }

    public static final class Option {
        public final Source source;
        public final String identifier; // material name (vanilla) or medicine id / pdc id (custom)
        public Option(Source source, String identifier) {
            this.source = source;
            this.identifier = identifier;
        }
    }

    private final List<Option> options;
    private final int amount;

    public LabIngredient(List<Option> options, int amount) {
        this.options = options;
        this.amount = amount;
    }

    public List<Option> options() { return options; }
    public int amount() { return amount; }

    public boolean matches(ItemStack item, MedicineItemFactory factory) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getAmount() < amount) return false;

        for (Option opt : options) {
            if (opt.source == Source.VANILLA) {
                Material m = Material.matchMaterial(opt.identifier);
                if (m != null && item.getType() == m && factory.medicineIdOf(item) == null) return true;
            } else {
                String id = factory.medicineIdOf(item);
                if (id != null && id.equalsIgnoreCase(opt.identifier)) return true;
            }
        }
        return false;
    }

    public static LabIngredient parse(String entry) {
        // "<typeA>:<idA>|<typeB>:<idB>:<amount>" — last segment is amount.
        int last = entry.lastIndexOf(':');
        if (last < 0) return null;
        int amount;
        try { amount = Integer.parseInt(entry.substring(last + 1).trim()); }
        catch (NumberFormatException ex) { return null; }
        String head = entry.substring(0, last);

        List<Option> opts = new ArrayList<>();
        for (String alt : head.split("\\|")) {
            String[] parts = alt.split(":", 2);
            if (parts.length != 2) continue;
            Source s = "custom".equalsIgnoreCase(parts[0]) ? Source.CUSTOM : Source.VANILLA;
            opts.add(new Option(s, parts[1].trim()));
        }
        if (opts.isEmpty()) return null;
        return new LabIngredient(opts, amount);
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) sb.append('|');
            Option o = options.get(i);
            sb.append(o.source.name().toLowerCase()).append(':').append(o.identifier);
        }
        sb.append(" x").append(amount);
        return sb.toString();
    }

    /**
     * Best-effort visual representation for the recipe-detail GUI.
     * Returns the first vanilla material from this ingredient's options,
     * or {@code null} if the ingredient is exclusively custom (in which
     * case the caller should fall back to building a medicine itemstack).
     */
    public Material previewVanillaMaterial() {
        for (Option opt : options) {
            if (opt.source == Source.VANILLA) {
                Material m = Material.matchMaterial(opt.identifier);
                if (m != null) return m;
            }
        }
        return null;
    }

    /**
     * Returns the first {@code custom:<id>} option, or {@code null} if
     * this ingredient has no custom options. The caller should look up
     * the medicine by id and craft a preview itemstack via the medicine
     * factory.
     */
    public String previewCustomId() {
        for (Option opt : options) {
            if (opt.source == Source.CUSTOM) return opt.identifier;
        }
        return null;
    }
}
