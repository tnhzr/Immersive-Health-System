package com.tnhzr.ihs.pack;

import com.tnhzr.ihs.ImmersiveHealthSystem;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves and runs the configured {@link PackInstaller} on plugin
 * enable. Selection is driven by {@code config.yml -> resourcepack.installer}:
 *
 * <ul>
 *   <li>{@code auto} — pick the first installer whose host plugin is
 *       loaded (CraftEngine ➜ ItemsAdder ➜ Nexo ➜ Oraxen). Falls back
 *       to {@code manual} if none are present.</li>
 *   <li>{@code craftengine}, {@code itemsadder}, {@code nexo},
 *       {@code oraxen}, {@code manual} — explicit selection.</li>
 *   <li>{@code none} or {@code disabled} — skip injection entirely.</li>
 * </ul>
 *
 * <p>Backwards compatibility: if {@code resourcepack.installer} is
 * absent, the legacy {@code craft_engine.inject_pack} switch is honoured
 * (treated as {@code installer: craftengine}).</p>
 */
public final class PackInstallerRegistry {

    private final ImmersiveHealthSystem plugin;
    private final Map<String, PackInstaller> installers = new LinkedHashMap<>();
    private PackInstaller active;

    public PackInstallerRegistry(ImmersiveHealthSystem plugin, File pluginJar) {
        this.plugin = plugin;
        register(new CraftEngineInstaller(plugin, pluginJar));
        register(new ItemsAdderInstaller(plugin, pluginJar));
        register(new NexoInstaller(plugin, pluginJar));
        register(new OraxenInstaller(plugin, pluginJar));
        register(new ManualInstaller(plugin, pluginJar));
    }

    private void register(PackInstaller installer) {
        installers.put(installer.id(), installer);
    }

    public PackInstaller active() { return active; }

    public Map<String, PackInstaller> all() { return installers; }

    /** Resolves config, picks the installer, runs it. */
    public void run() {
        String mode = plugin.configs().main()
                .getString("resourcepack.installer", "auto").toLowerCase();
        // Legacy support: craft_engine.inject_pack=false -> disable.
        if (!plugin.configs().main().getBoolean("craft_engine.inject_pack", true)
                && !plugin.configs().main().contains("resourcepack.installer")) {
            mode = "none";
        }
        if (mode.equals("none") || mode.equals("disabled") || mode.equals("off")) {
            plugin.getLogger().info("Resourcepack injection disabled by config.");
            this.active = null;
            return;
        }

        PackInstaller chosen;
        if (mode.equals("auto")) {
            chosen = pickAuto();
        } else {
            chosen = installers.get(mode);
            if (chosen == null) {
                plugin.getLogger().warning("Unknown resourcepack.installer '"
                        + mode + "', falling back to auto.");
                chosen = pickAuto();
            } else if (!chosen.isAvailable() && !mode.equals("manual")) {
                plugin.getLogger().warning("Installer '" + mode
                        + "' is configured but its host plugin isn't loaded. "
                        + "Falling back to manual.");
                chosen = installers.get("manual");
            }
        }
        this.active = chosen;
        if (chosen == null) return;
        boolean force = plugin.configs().main()
                .getBoolean("resourcepack.force_overwrite",
                        plugin.configs().main()
                                .getBoolean("craft_engine.force_overwrite", false));
        chosen.install(force);
    }

    private PackInstaller pickAuto() {
        // CraftEngine first to preserve existing behaviour, then the
        // other big custom-content plugins, falling back to manual.
        for (String id : new String[]{"craftengine", "itemsadder", "nexo", "oraxen"}) {
            PackInstaller candidate = installers.get(id);
            if (candidate != null && candidate.isAvailable()) {
                return candidate;
            }
        }
        return installers.get("manual");
    }
}
