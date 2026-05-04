package com.tnhzr.ihs.medicine;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.disease.DiseaseLoader;
import com.tnhzr.ihs.disease.PlayerDiseaseState;
import com.tnhzr.ihs.util.Text;
import com.tnhzr.ihs.util.TimeParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public final class MedicineConsumeListener implements Listener {

    private final ImmersiveHealthSystem plugin;
    private final MedicineManager manager;

    public MedicineConsumeListener(ImmersiveHealthSystem plugin, MedicineManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        String id = manager.factory().medicineIdOf(event.getItem());
        if (id == null) return;
        Medicine m = manager.medicine(id);
        if (m == null) return;

        // Cancel the vanilla consume so golden-apple absorption/regen never fire.
        event.setCancelled(true);
        // Decrement the live inventory stack (event.getItem() is a clone).
        org.bukkit.inventory.EquipmentSlot hand = event.getHand();
        org.bukkit.inventory.PlayerInventory inv = p.getInventory();
        org.bukkit.inventory.ItemStack live = (hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND)
                ? inv.getItemInOffHand()
                : inv.getItemInMainHand();
        if (live == null || manager.factory().medicineIdOf(live) == null) {
            // Fall back to main-hand if event slot doesn't actually hold the medicine.
            live = inv.getItemInMainHand();
        }
        if (live != null && manager.factory().medicineIdOf(live) != null) {
            if (live.getAmount() <= 1) {
                if (hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND) inv.setItemInOffHand(null);
                else inv.setItemInMainHand(null);
            } else {
                live.setAmount(live.getAmount() - 1);
            }
        }

        PlayerDiseaseState state = plugin.diseases().state(p.getUniqueId());
        state.rolloverDayIfNeeded(p.getWorld().getFullTime() / 24000L);

        int dailyCount = state.medicineUsage().getOrDefault(m.id(), 0);
        if (dailyCount >= m.dailyLimit()) {
            // Toxicity: poison, no healing.
            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1, true, true, true));
            plugin.locale().send(p, "medicine.overdose");
            return;
        }
        state.medicineUsage().put(m.id(), dailyCount + 1);

        switch (m.type()) {
            case CURE -> applyCure(p, m, state);
            case EFFECT_CLEAR -> applyEffectClear(p, m);
            case BUFF -> applyBuff(p, m);
        }

        plugin.locale().send(p, "medicine.used", Map.of("medicine", m.name()));
    }

    private void applyCure(Player p, Medicine m, PlayerDiseaseState state) {
        boolean panaceaAll = m.curesInfections().contains("ALL");
        if (panaceaAll) {
            plugin.diseases().heal(p, null);
            return;
        }
        for (String diseaseId : m.curesInfections()) {
            if (!state.hasInfection(diseaseId)) continue;
            int prevScale = state.scale(diseaseId);
            int newScale = prevScale - m.healPoints();
            // Suppress today's growth on this disease.
            state.growthSuppressed().put(diseaseId, true);
            if (newScale <= 0) {
                state.setScale(diseaseId, 0);
            } else {
                state.setScale(diseaseId, newScale);
            }
            int delta = prevScale - state.scale(diseaseId);
            plugin.diseases().recordReliefProgress(p, diseaseId, delta);
        }
    }

    private void applyEffectClear(Player p, Medicine m) {
        for (String fx : m.clearsPotionEffects()) {
            PotionEffectType t = DiseaseLoader.matchEffect(fx);
            if (t != null) p.removePotionEffect(t);
        }
    }

    private void applyBuff(Player p, Medicine m) {
        for (String entry : m.applyPotionEffects()) {
            String[] parts = entry.split(":");
            if (parts.length < 1) continue;
            PotionEffectType t = DiseaseLoader.matchEffect(parts[0]);
            if (t == null) continue;
            int amp = parts.length > 1 ? safeInt(parts[1], 0) : 0;
            int seconds = parts.length > 2 ? (int) (TimeParser.toSeconds(parts[2] + "s")) : 60;
            // Treat the third number as plain seconds (no suffix), per the spec.
            if (parts.length > 2) {
                try { seconds = Integer.parseInt(parts[2]); }
                catch (NumberFormatException ignored) {}
            }
            p.addPotionEffect(new PotionEffect(t, seconds * 20, amp, true, true, true));
        }
    }

    private static int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
