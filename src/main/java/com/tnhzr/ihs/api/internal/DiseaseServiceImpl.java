package com.tnhzr.ihs.api.internal;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.api.DiseaseService;
import com.tnhzr.ihs.disease.Disease;
import com.tnhzr.ihs.disease.DiseaseManager;
import com.tnhzr.ihs.disease.PlayerDiseaseState;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class DiseaseServiceImpl implements DiseaseService {

    private final ImmersiveHealthSystem plugin;

    DiseaseServiceImpl(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    private DiseaseManager dm() { return plugin.diseases(); }

    @Override
    public boolean infect(Player player, String diseaseId, double chance) {
        if (player == null || diseaseId == null) return false;
        if (chance < 1.0 && Math.random() >= chance) return false;
        Disease disease = dm().disease(diseaseId);
        if (disease == null) return false;
        PlayerDiseaseState st = dm().state(player.getUniqueId());
        if (st.scale(diseaseId) > 0) return false;
        dm().infect(player, diseaseId, 1);
        return true;
    }

    @Override
    public void heal(Player player, String diseaseId, int amount) {
        if (player == null || diseaseId == null) return;
        PlayerDiseaseState st = dm().state(player.getUniqueId());
        int next = Math.max(0, st.scale(diseaseId) - Math.max(0, amount));
        st.setScale(diseaseId, next);
    }

    @Override
    public void healAll(Player player) {
        if (player == null) return;
        dm().heal(player, null);
    }

    @Override
    public int scale(Player player, String diseaseId) {
        if (player == null || diseaseId == null) return 0;
        return dm().state(player.getUniqueId()).scale(diseaseId);
    }

    @Override
    public void setScale(Player player, String diseaseId, int value) {
        if (player == null || diseaseId == null) return;
        int clamped = Math.max(0, Math.min(100, value));
        dm().state(player.getUniqueId()).setScale(diseaseId, clamped);
    }

    @Override
    public Set<String> activeInfections(Player player) {
        if (player == null) return Set.of();
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, Integer> e :
                dm().state(player.getUniqueId()).infections().entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) out.add(e.getKey());
        }
        return out;
    }

    @Override
    public Map<String, Integer> infectionMap(Player player) {
        if (player == null) return Map.of();
        return new HashMap<>(dm().state(player.getUniqueId()).infections());
    }

    @Override
    public Set<String> knownDiseases() {
        return new HashSet<>(dm().diseases().keySet());
    }
}
