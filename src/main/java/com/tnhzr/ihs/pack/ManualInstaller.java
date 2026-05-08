package com.tnhzr.ihs.pack;

import com.tnhzr.ihs.ImmersiveHealthSystem;

import java.io.File;
import java.nio.file.Path;

/**
 * Always-available fallback installer. Unpacks the bundled pack into
 * {@code plugins/ImmersiveHealthSystem/resourcepack/} so admins who
 * don't run any of the supported pack-host plugins can still pick up
 * the assets manually (zip + serve).
 */
public final class ManualInstaller extends PackInstaller {

    public ManualInstaller(ImmersiveHealthSystem plugin, File pluginJar) {
        super(plugin, pluginJar);
    }

    @Override public String id() { return "manual"; }

    @Override public boolean isAvailable() { return true; }

    @Override public Path targetDirectory() {
        return plugin.getDataFolder().toPath().resolve("resourcepack");
    }

    @Override public String describe() { return "Manual resourcepack staging"; }
}
