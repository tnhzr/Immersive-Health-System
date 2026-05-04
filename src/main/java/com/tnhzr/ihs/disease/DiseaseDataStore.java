package com.tnhzr.ihs.disease;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class DiseaseDataStore {

    private final ImmersiveHealthSystem plugin;
    private final File folder;

    public DiseaseDataStore(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();
    }

    public PlayerDiseaseState load(UUID uuid) {
        PlayerDiseaseState state = new PlayerDiseaseState(uuid);
        File file = new File(folder, uuid + ".yml");
        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            state.load(cfg);
        }
        return state;
    }

    public void save(PlayerDiseaseState state) {
        File file = new File(folder, state.uuid() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        state.save(cfg);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player disease data: " + e.getMessage());
        }
    }
}
