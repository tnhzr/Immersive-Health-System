package com.tnhzr.ihs.medicine;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * The vanilla milk bucket clears every potion effect. We let the player still
 * drink the milk (and consume the bucket as normal), but immediately re-apply
 * any active effects so the cure-all behaviour goes away.
 */
public final class MilkNerfListener implements Listener {

    private final ImmersiveHealthSystem plugin;

    public MilkNerfListener(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (!plugin.configs().main().getBoolean("medicine.disable_vanilla_milk_clear", true)) return;
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.MILK_BUCKET) return;

        Player p = e.getPlayer();
        List<PotionEffect> active = new ArrayList<>(p.getActivePotionEffects());
        // Re-apply on the next tick after Bukkit's cure pass.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (PotionEffect fx : active) p.addPotionEffect(fx, true);
        });
    }
}
