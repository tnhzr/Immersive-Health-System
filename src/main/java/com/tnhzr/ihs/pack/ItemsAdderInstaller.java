package com.tnhzr.ihs.pack;

import com.tnhzr.ihs.ImmersiveHealthSystem;

import java.io.File;
import java.nio.file.Path;

/**
 * Installs the IHS pack into ItemsAdder's contents directory.
 *
 * <p>ItemsAdder picks up custom packs from
 * {@code plugins/ItemsAdder/contents/<namespace>/resourcepack/}.
 * After install the server admin should run {@code /iazip} or
 * {@code /iareload} to rebuild the bundled client pack.</p>
 */
public final class ItemsAdderInstaller extends PackInstaller {

    public ItemsAdderInstaller(ImmersiveHealthSystem plugin, File pluginJar) {
        super(plugin, pluginJar);
    }

    @Override public String id() { return "itemsadder"; }

    @Override public boolean isAvailable() { return hostPluginLoaded("ItemsAdder"); }

    @Override public Path targetDirectory() {
        String namespace = plugin.configs().main().getString(
                "resourcepack.itemsadder.namespace", "ihs");
        return pluginsDir().resolve("ItemsAdder")
                .resolve("contents").resolve(namespace).resolve("resourcepack");
    }

    @Override public String describe() { return "ItemsAdder pack injection"; }
}
