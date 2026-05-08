package com.tnhzr.ihs.api;

/**
 * Public, stable entry point for the Immersive Health System plugin.
 *
 * <h2>Acquiring the API</h2>
 *
 * <pre>{@code
 * RegisteredServiceProvider<IHSApi> rsp =
 *     Bukkit.getServicesManager().getRegistration(IHSApi.class);
 * if (rsp != null) {
 *     IHSApi ihs = rsp.getProvider();
 *     ihs.diseases().infect(player, "tuberculosis", 1.0);
 * }
 * }</pre>
 *
 * <h2>Stability</h2>
 *
 * <p>Everything reachable from this interface (and its sub-interfaces in
 * the {@code com.tnhzr.ihs.api} package) follows semver:</p>
 * <ul>
 *   <li><b>Patch / minor</b> bumps may add new methods but never break
 *       existing signatures.</li>
 *   <li><b>Major</b> bumps may break compatibility — these are documented
 *       in the corresponding GitHub release notes.</li>
 * </ul>
 *
 * <p>Anything in {@code com.tnhzr.ihs.api.internal.*} is implementation
 * detail and may change without notice — do not import it from a
 * downstream plugin.</p>
 */
public interface IHSApi {

    /** Plugin version, mirrors {@code plugin.yml}. */
    String version();

    /** Disease scale, infections, transmissions. */
    DiseaseService diseases();

    /** Medicine catalogue and item factory. */
    MedicineService medicines();

    /** Laboratory placement, recipes and synthesis. */
    LaboratoryService laboratories();

    /** Active resourcepack installer (CraftEngine / ItemsAdder / ...). */
    ResourcePackService resourcePack();

    /**
     * Common namespace used for all {@code NamespacedKey}s the plugin
     * stores in PDC. Forks may override this in their config.
     */
    String namespace();
}
