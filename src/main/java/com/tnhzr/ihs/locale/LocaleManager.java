package com.tnhzr.ihs.locale;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads localized message bundles for the plugin and exposes a small
 * lookup API over them. The active language is taken from
 * {@code config.yml -> language}; if the requested file is missing the
 * manager falls back to the bundled English file.
 *
 * <p>Each bundle is a flat YAML file under {@code lang/messages_<code>.yml}.
 * Keys are dot-separated paths (e.g. {@code lab.gui.craft_button}) and
 * support {@code {placeholder}} substitutions. Legacy {@code &}-colour
 * codes are translated by {@link Text}.</p>
 */
public final class LocaleManager {

    private static final String DEFAULT_LANGUAGE = "ru";
    private static final String FALLBACK_LANGUAGE = "en";

    private final ImmersiveHealthSystem plugin;
    private final Set<String> warnedKeys = new HashSet<>();
    private FileConfiguration active;
    private FileConfiguration fallback;
    private String language;

    public LocaleManager(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    /** (Re)load the bundle indicated by {@code config.yml -> language}. */
    public void load() {
        warnedKeys.clear();

        // Make sure both bundled languages are present in the data folder.
        ensureExtracted("lang/messages_en.yml");
        ensureExtracted("lang/messages_ru.yml");

        this.fallback = readFile("lang/messages_" + FALLBACK_LANGUAGE + ".yml");

        String requested = plugin.getConfig().getString("language", DEFAULT_LANGUAGE);
        if (requested == null || requested.isBlank()) requested = DEFAULT_LANGUAGE;
        this.language = requested.toLowerCase();

        FileConfiguration loaded = readFile("lang/messages_" + language + ".yml");
        if (loaded == null) {
            plugin.getLogger().warning("Locale '" + language
                    + "' not found, falling back to '" + FALLBACK_LANGUAGE + "'.");
            loaded = fallback;
        }
        this.active = loaded;

        plugin.getLogger().info("Loaded locale '" + language + "'.");
    }

    /** Currently active language code (e.g. {@code ru}, {@code en}). */
    public String language() { return language; }

    /** Raw legacy string for the given dotted key, or empty if missing. */
    public String raw(String key) {
        if (active != null) {
            String s = active.getString(key);
            if (s != null) return s;
        }
        if (fallback != null && active != fallback) {
            String s = fallback.getString(key);
            if (s != null) return s;
        }
        warnMissing(key);
        return "";
    }

    /** Same as {@link #raw(String)} with placeholder substitution. */
    public String raw(String key, Map<String, String> placeholders) {
        return Text.fillPlaceholders(raw(key), placeholders);
    }

    /** Translates a raw legacy string into an Adventure {@link Component}. */
    public Component component(String key) {
        return Text.component(raw(key));
    }

    public Component component(String key, Map<String, String> placeholders) {
        return Text.component(raw(key, placeholders));
    }

    /** Multi-line value (returns empty list if the key is missing). */
    public List<String> rawList(String key) {
        if (active != null) {
            List<String> ls = active.getStringList(key);
            if (!ls.isEmpty()) return ls;
        }
        if (fallback != null && active != fallback) {
            List<String> ls = fallback.getStringList(key);
            if (!ls.isEmpty()) return ls;
        }
        warnMissing(key);
        return Collections.emptyList();
    }

    /** Sends a prefixed translated message to the recipient. */
    public void send(CommandSender to, String key) {
        send(to, key, Collections.emptyMap());
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        String body = raw(key, placeholders);
        if (body.isEmpty()) return;
        String prefix = raw("prefix");
        to.sendMessage(Text.component(prefix + body));
    }

    /** Sends a translated message without the global prefix. */
    public void sendRaw(CommandSender to, String key) {
        sendRaw(to, key, Collections.emptyMap());
    }

    public void sendRaw(CommandSender to, String key, Map<String, String> placeholders) {
        String body = raw(key, placeholders);
        if (body.isEmpty()) return;
        to.sendMessage(Text.component(body));
    }

    private void warnMissing(String key) {
        if (warnedKeys.add(key)) {
            plugin.getLogger().warning("Missing locale key '" + key + "' (lang="
                    + language + ").");
        }
    }

    private void ensureExtracted(String resource) {
        File out = new File(plugin.getDataFolder(), resource);
        if (out.exists()) return;
        if (plugin.getResource(resource) == null) return;
        plugin.saveResource(resource, false);
    }

    /**
     * Reads a locale bundle from the data folder, layering the bundled
     * (in-jar) copy of the same file as YAML defaults so that a
     * user-edited file with missing keys still resolves through to the
     * latest plugin defaults. Returns {@code null} only if the file is
     * absent both on disk and in the JAR.
     */
    private FileConfiguration readFile(String relative) {
        File file = new File(plugin.getDataFolder(), relative);
        boolean haveFile = file.exists();
        InputStream defaults = plugin.getResource(relative);
        if (!haveFile && defaults == null) return null;

        FileConfiguration cfg = haveFile
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        if (defaults != null) {
            try (Reader reader = new InputStreamReader(defaults, StandardCharsets.UTF_8)) {
                cfg.setDefaults(YamlConfiguration.loadConfiguration(reader));
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to read bundled defaults for '"
                        + relative + "': " + ex.getMessage());
            }
        }
        return cfg;
    }
}
