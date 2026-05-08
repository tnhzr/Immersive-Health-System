package com.tnhzr.ihs.pack;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Strategy interface for pushing the bundled IHS resource pack into a
 * host plugin's pack folder (or to a manual staging area).
 *
 * <p>Concrete implementations live next to this class — see
 * {@link CraftEngineInstaller}, {@link ItemsAdderInstaller},
 * {@link NexoInstaller}, {@link OraxenInstaller},
 * {@link ManualInstaller}.</p>
 *
 * <p>Selection happens in {@link PackInstallerRegistry} based on the
 * {@code resourcepack.installer} config key. {@code "auto"} picks the
 * first installer whose target plugin is loaded.</p>
 */
public abstract class PackInstaller {

    /** All bundled resourcepack content lives under this jar prefix. */
    protected static final String JAR_PREFIX = "pack/resourcepack/";

    protected final ImmersiveHealthSystem plugin;
    protected final File pluginJar;

    protected PackInstaller(ImmersiveHealthSystem plugin, File pluginJar) {
        this.plugin = plugin;
        this.pluginJar = pluginJar;
    }

    /** Internal id used in {@code config.yml -> resourcepack.installer}. */
    public abstract String id();

    /**
     * @return {@code true} if the host plugin (if any) is loaded and the
     *         installer can perform a meaningful injection in this
     *         environment. {@link ManualInstaller} always returns true.
     */
    public abstract boolean isAvailable();

    /** @return absolute path where the pack should be unpacked. */
    public abstract Path targetDirectory();

    /** Friendly description for log lines / debug output. */
    public abstract String describe();

    /**
     * Performs the unpack. Default implementation walks the bundled
     * jar, copying every entry under {@link #JAR_PREFIX} into
     * {@link #targetDirectory()}.
     *
     * @param force if true, overwrites every existing file; otherwise
     *              only missing files are written so server-side edits
     *              survive restarts.
     */
    public void install(boolean force) {
        Path target = targetDirectory();
        int written = 0;
        int skipped = 0;
        try {
            Files.createDirectories(target);
            try (JarFile jar = new JarFile(pluginJar)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(JAR_PREFIX)) continue;
                    if (entry.isDirectory()) continue;
                    String rel = name.substring(JAR_PREFIX.length());
                    if (rel.isEmpty() || rel.contains("..")) continue;
                    Path dst = target.resolve(rel).normalize();
                    if (!dst.startsWith(target)) continue;
                    Path parent = dst.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    if (!force && Files.exists(dst)) { skipped++; continue; }
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
                        written++;
                    }
                }
            }
            plugin.getLogger().info(describe() + ": " + written
                    + " written, " + skipped + " skipped (target: "
                    + target + ").");
        } catch (IOException ex) {
            plugin.getLogger().warning(describe() + " failed: " + ex.getMessage());
        }
    }

    /* ---------- helpers ---------- */

    protected final Path pluginsDir() {
        return plugin.getDataFolder().toPath().getParent();
    }

    protected final boolean hostPluginLoaded(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }
}
