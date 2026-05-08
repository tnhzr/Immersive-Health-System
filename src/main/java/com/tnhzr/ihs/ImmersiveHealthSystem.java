package com.tnhzr.ihs;

import com.tnhzr.ihs.config.ConfigManager;
import com.tnhzr.ihs.disease.DiseaseManager;
import com.tnhzr.ihs.disease.symptom.ResourcePackTracker;
import com.tnhzr.ihs.disease.symptom.SymptomConfig;
import com.tnhzr.ihs.lab.LabManager;
import com.tnhzr.ihs.locale.LocaleManager;
import com.tnhzr.ihs.medicine.MedicineManager;
import com.tnhzr.ihs.module.ModuleManager;
import com.tnhzr.ihs.api.IHSApi;
import com.tnhzr.ihs.api.internal.IHSApiImpl;
import com.tnhzr.ihs.pack.PackInstallerRegistry;
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
    private PackInstallerRegistry packInstallers;
    private IHSApi api;

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

        // Resolve the configured pack installer (CraftEngine /
        // ItemsAdder / Nexo / Oraxen / Manual) and unpack the bundled
        // resourcepack into its target folder.
        this.packInstallers = new PackInstallerRegistry(this, pluginJarFile());
        this.packInstallers.run();

        this.moduleManager = new ModuleManager(this);

        this.diseaseManager = new DiseaseManager(this);
        this.medicineManager = new MedicineManager(this);
        this.labManager = new LabManager(this);

        moduleManager.register("disease", diseaseManager);
        moduleManager.register("medicine", medicineManager);
        moduleManager.register("laboratory", labManager);

        moduleManager.enableConfigured();

        // Public API service registration. Forks / third-party plugins
        // get a stable entry point via Bukkit's ServicesManager.
        this.api = new IHSApiImpl(this);
        getServer().getServicesManager().register(
                IHSApi.class, api, this,
                org.bukkit.plugin.ServicePriority.Normal);

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
    public PackInstallerRegistry packInstallers() { return packInstallers; }
    public IHSApi api() { return api; }

    public NamespacedKey key(String name) {
        return new NamespacedKey(this, name);
    }

    /** Exposes JavaPlugin#getFile() to the resourcepack installer. */
    public File pluginJarFile() {
        return getFile();
    }
}
