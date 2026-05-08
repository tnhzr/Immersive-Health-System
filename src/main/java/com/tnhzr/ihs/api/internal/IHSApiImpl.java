package com.tnhzr.ihs.api.internal;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.api.DiseaseService;
import com.tnhzr.ihs.api.IHSApi;
import com.tnhzr.ihs.api.LaboratoryService;
import com.tnhzr.ihs.api.MedicineService;
import com.tnhzr.ihs.api.ResourcePackService;

/**
 * Default {@link IHSApi} implementation. Forks may swap this with their
 * own subclass before {@code onEnable} finishes, but they should keep
 * the public interface contracts intact.
 */
public final class IHSApiImpl implements IHSApi {

    private final ImmersiveHealthSystem plugin;
    private final DiseaseService diseases;
    private final MedicineService medicines;
    private final LaboratoryService laboratories;
    private final ResourcePackService resourcePack;

    public IHSApiImpl(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
        this.diseases = new DiseaseServiceImpl(plugin);
        this.medicines = new MedicineServiceImpl(plugin);
        this.laboratories = new LaboratoryServiceImpl(plugin);
        this.resourcePack = new ResourcePackServiceImpl(plugin);
    }

    @Override public String version() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override public DiseaseService diseases() { return diseases; }
    @Override public MedicineService medicines() { return medicines; }
    @Override public LaboratoryService laboratories() { return laboratories; }
    @Override public ResourcePackService resourcePack() { return resourcePack; }

    @Override public String namespace() {
        return plugin.getName().toLowerCase().replace(' ', '_');
    }
}
