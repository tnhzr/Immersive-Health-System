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
 * Auto-extracts the bundled CraftEngine pack into
 * {@code plugins/CraftEngine/<target_folder>/} on enable so the
 * laboratory texture, custom block model and symptom sounds resolve
 * out of the box.
 *
 * <p>Behaviour is controlled by {@code config.yml -> craft_engine}:
 * <ul>
 *   <li>{@code inject_pack} — master switch (default: {@code true}).</li>
 *   <li>{@code target_folder} — destination relative to
 *       {@code plugins/CraftEngine/} (default: {@code resources/immersive_health}).</li>
 *   <li>{@code force_overwrite} — when {@code true}, every file is
 *       rewritten on every server start; when {@code false}, only
 *       missing files are written so server-side edits survive
 *       restarts.</li>
 * </ul>
 *
 * <p>If CraftEngine is not installed the injector silently no-ops.</p>
 */
public final class CraftEnginePackInjector {

    private static final String JAR_PREFIX = "pack/";

    private final ImmersiveHealthSystem plugin;
    private final File pluginJar;

    public CraftEnginePackInjector(ImmersiveHealthSystem plugin, File pluginJar) {
        this.plugin = plugin;
        this.pluginJar = pluginJar;
    }

    public void run() {
        if (!plugin.configs().main().getBoolean("craft_engine.inject_pack", true)) return;
        if (Bukkit.getPluginManager().getPlugin("CraftEngine") == null) {
            plugin.getLogger().info("CraftEngine not detected — pack injector skipped.");
            return;
        }
        Path craftEngineDir = plugin.getDataFolder().toPath()
                .getParent().resolve("CraftEngine");
        String targetFolder = plugin.configs().main()
                .getString("craft_engine.target_folder", "resources/immersive_health");
        boolean force = plugin.configs().main()
                .getBoolean("craft_engine.force_overwrite", false);
        Path target = craftEngineDir.resolve(targetFolder);

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
                    Path dstParent = dst.getParent();
                    if (dstParent != null) Files.createDirectories(dstParent);
                    if (!force && Files.exists(dst)) { skipped++; continue; }
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
                        written++;
                    }
                }
            }
            plugin.getLogger().info("CraftEngine pack injection: "
                    + written + " written, " + skipped + " skipped (target: "
                    + target + ").");
        } catch (IOException ex) {
            plugin.getLogger().warning("CraftEngine pack injection failed: " + ex.getMessage());
        }
    }
}
