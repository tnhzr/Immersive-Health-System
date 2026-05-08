package com.tnhzr.ihs.api.internal;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.api.ResourcePackService;
import com.tnhzr.ihs.pack.PackInstaller;
import org.bukkit.entity.Player;

import java.nio.file.Path;

final class ResourcePackServiceImpl implements ResourcePackService {

    private final ImmersiveHealthSystem plugin;

    ResourcePackServiceImpl(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public String activeInstaller() {
        PackInstaller active = plugin.packInstallers().active();
        return active == null ? "none" : active.id();
    }

    @Override
    public Path activeInstallerTarget() {
        PackInstaller active = plugin.packInstallers().active();
        return active == null ? null : active.targetDirectory();
    }

    @Override
    public boolean hasPack(Player player) {
        return plugin.resourcePacks().hasPack(player);
    }

    @Override
    public void markLoaded(Player player) {
        plugin.resourcePacks().markLoaded(player);
    }
}
