package com.tnhzr.ihs.pack;

import com.tnhzr.ihs.ImmersiveHealthSystem;

import java.io.File;
import java.nio.file.Path;

/**
 * Installs the IHS pack into Oraxen's external pack directory.
 *
 * <p>Oraxen scans {@code plugins/Oraxen/pack/external_packs/} and merges
 * every entry into the assembled client pack. Reload Oraxen or restart
 * the server after install.</p>
 */
public final class OraxenInstaller extends PackInstaller {

    public OraxenInstaller(ImmersiveHealthSystem plugin, File pluginJar) {
        super(plugin, pluginJar);
    }

    @Override public String id() { return "oraxen"; }

    @Override public boolean isAvailable() { return hostPluginLoaded("Oraxen"); }

    @Override public Path targetDirectory() {
        String folder = plugin.configs().main().getString(
                "resourcepack.oraxen.folder", "ihs");
        return pluginsDir().resolve("Oraxen")
                .resolve("pack").resolve("external_packs").resolve(folder);
    }

    @Override public String describe() { return "Oraxen pack injection"; }
}
