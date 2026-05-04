package com.tnhzr.ihs.module;

import com.tnhzr.ihs.ImmersiveHealthSystem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleManager {

    private final ImmersiveHealthSystem plugin;
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<String, Boolean> enabled = new LinkedHashMap<>();

    public ModuleManager(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    public void register(String id, Module module) {
        modules.put(id, module);
    }

    public void enableConfigured() {
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            String id = entry.getKey();
            boolean enable = plugin.getConfig().getBoolean("modules." + id, true);
            if (enable) {
                try {
                    entry.getValue().enable();
                    enabled.put(id, true);
                    plugin.getLogger().info("Module enabled: " + id);
                } catch (Throwable t) {
                    enabled.put(id, false);
                    plugin.getLogger().severe("Failed to enable module " + id + ": " + t.getMessage());
                    t.printStackTrace();
                }
            } else {
                enabled.put(id, false);
                plugin.getLogger().info("Module disabled by config: " + id);
            }
        }
    }

    public boolean isEnabled(String id) {
        return enabled.getOrDefault(id, false);
    }

    public void disableAll() {
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            if (Boolean.TRUE.equals(enabled.get(entry.getKey()))) {
                try {
                    entry.getValue().disable();
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed to disable module " + entry.getKey() + ": " + t.getMessage());
                }
            }
        }
    }
}
