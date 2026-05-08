package com.tnhzr.ihs.pack;

import com.tnhzr.ihs.ImmersiveHealthSystem;

import java.io.File;
import java.nio.file.Path;

/**
 * Installs the IHS pack into Nexo's external pack directory.
 *
 * <p>Nexo merges every directory under
 * {@code plugins/Nexo/pack/external_packs/} into the final client pack.
 * After install the admin should reload Nexo or restart the server.</p>
 */
public final class NexoInstaller extends PackInstaller {

    public NexoInstaller(ImmersiveHealthSystem plugin, File pluginJar) {
        super(plugin, pluginJar);
    }

    @Override public String id() { return "nexo"; }

    @Override public boolean isAvailable() { return hostPluginLoaded("Nexo"); }

    @Override public Path targetDirectory() {
        String folder = plugin.configs().main().getString(
                "resourcepack.nexo.folder", "ihs");
        return pluginsDir().resolve("Nexo")
                .resolve("pack").resolve("external_packs").resolve(folder);
    }

    @Override public String describe() { return "Nexo pack injection"; }
}
