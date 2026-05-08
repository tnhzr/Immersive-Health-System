package com.tnhzr.ihs.pack;

import com.tnhzr.ihs.ImmersiveHealthSystem;

import java.io.File;
import java.nio.file.Path;

/**
 * Installs the IHS pack into {@code plugins/CraftEngine/<target>/}.
 * Target folder is configurable via
 * {@code resourcepack.craftengine.target_folder}
 * (default: {@code resources/immersive_health}).
 */
public final class CraftEngineInstaller extends PackInstaller {

    public CraftEngineInstaller(ImmersiveHealthSystem plugin, File pluginJar) {
        super(plugin, pluginJar);
    }

    @Override public String id() { return "craftengine"; }

    @Override public boolean isAvailable() { return hostPluginLoaded("CraftEngine"); }

    @Override public Path targetDirectory() {
        String folder = plugin.configs().main().getString(
                "resourcepack.craftengine.target_folder",
                plugin.configs().main().getString(
                        "craft_engine.target_folder",
                        "resources/immersive_health"));
        return pluginsDir().resolve("CraftEngine").resolve(folder);
    }

    @Override public String describe() { return "CraftEngine pack injection"; }
}
