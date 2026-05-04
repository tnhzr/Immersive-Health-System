package com.tnhzr.ihs;

import com.tnhzr.ihs.config.ConfigManager;
import com.tnhzr.ihs.disease.DiseaseManager;
import com.tnhzr.ihs.disease.symptom.ResourcePackTracker;
import com.tnhzr.ihs.disease.symptom.SymptomConfig;
import com.tnhzr.ihs.lab.LabManager;
import com.tnhzr.ihs.locale.LocaleManager;
import com.tnhzr.ihs.medicine.MedicineManager;
import com.tnhzr.ihs.module.ModuleManager;
import com.tnhzr.ihs.pack.CraftEnginePackInjector;
import com.tnhzr.ihs.util.AsciiBanner;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ImmersiveHealthSystem extends JavaPlugin {

    private static ImmersiveHealthSystem instance;

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private ModuleManager moduleManager;
    private DiseaseManager diseaseManager;
    private MedicineManager medicineManager;
    private LabManager labManager;
    private SymptomConfig symptomConfig;
    private ResourcePackTracker resourcePackTracker;

    @Override
    public void onEnable() {
        instance = this;
        AsciiBanner.print(getLogger(), getPluginMeta().getVersion());

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        configManager.loadAll();
        this.localeManager = new LocaleManager(this);
        localeManager.load();
        this.symptomConfig = new SymptomConfig(this);
        symptomConfig.load();
        this.resourcePackTracker = new ResourcePackTracker(this);
        getServer().getPluginManager().registerEvents(resourcePackTracker, this);

        // Drop bundled CraftEngine pack into plugins/CraftEngine/<target>/.
        new CraftEnginePackInjector(this, pluginJarFile()).run();

        this.moduleManager = new ModuleManager(this);

        this.diseaseManager = new DiseaseManager(this);
        this.medicineManager = new MedicineManager(this);
        this.labManager = new LabManager(this);

        moduleManager.register("disease", diseaseManager);
        moduleManager.register("medicine", medicineManager);
        moduleManager.register("laboratory", labManager);

        moduleManager.enableConfigured();

        getLogger().info("Immersive Health System enabled.");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        getLogger().info("Immersive Health System disabled.");
    }

    public static ImmersiveHealthSystem getInstance() {
        return instance;
    }

    public ConfigManager configs() { return configManager; }
    public LocaleManager locale() { return localeManager; }
    public ModuleManager modules() { return moduleManager; }
    public DiseaseManager diseases() { return diseaseManager; }
    public MedicineManager medicines() { return medicineManager; }
    public LabManager laboratories() { return labManager; }
    public SymptomConfig symptoms() { return symptomConfig; }
    public ResourcePackTracker resourcePacks() { return resourcePackTracker; }

    public NamespacedKey key(String name) {
        return new NamespacedKey(this, name);
    }

    /** Exposes JavaPlugin#getFile() to the CraftEngine pack injector. */
    public File pluginJarFile() {
        return getFile();
    }
}
