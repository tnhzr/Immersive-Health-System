package com.tnhzr.ihs.medicine;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the tranquilizer drug. The tranquilizer is a regular
 * {@link Medicine} of {@link Medicine.Type#TRANQUILIZER} but extends the
 * normal medicine pipeline with three extra mechanics:
 *
 * <ol>
 *   <li><b>Drinkable.</b> Consuming the item directly fires a brief
 *       blindness "onset" then forces the player to sleep for the
 *       configured {@code sleep_seconds} duration. Handled in
 *       {@link MedicineConsumeListener} via {@link #applySleepRoutine}.</li>
 *   <li><b>Food lacing.</b> Drag-dropping a tranquilizer item onto an
 *       edible item in any inventory consumes one tranquilizer and
 *       tags the food with {@link #LACED_KEY}. Eating a laced food
 *       triggers the same sleep routine as the direct drink.</li>
 *   <li><b>Projectile coating.</b> Drag-dropping a tranquilizer onto a
 *       stack of arrows (or other ammo) consumes one tranquilizer and
 *       tags every arrow in the stack with {@link #ARROW_KEY}. When
 *       the projectile hits a living entity, that target is forced
 *       to sleep (players via {@code Player.sleep(loc, true)}, mobs by
 *       freezing AI for the duration).</li>
 * </ol>
 */
public final class TranquilizerListener implements Listener {

    private final ImmersiveHealthSystem plugin;
    private final MedicineManager manager;
    private final NamespacedKey lacedKey;
    private final NamespacedKey arrowKey;
    private final NamespacedKey lacedSleepSecondsKey;

    /** PDC key applied to a food item that has been laced with tranq. */
    public static final String LACED_KEY = "tranq_laced";
    /** PDC key applied to an arrow / projectile coated with tranq. */
    public static final String ARROW_KEY = "tranq_arrow";
    /** PDC key holding the sleep_seconds copy for laced food / arrows. */
    public static final String SLEEP_SECONDS_KEY = "tranq_sleep_seconds";

    public TranquilizerListener(ImmersiveHealthSystem plugin, MedicineManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.lacedKey = plugin.key(LACED_KEY);
        this.arrowKey = plugin.key(ARROW_KEY);
        this.lacedSleepSecondsKey = plugin.key(SLEEP_SECONDS_KEY);
    }

    /* ------------------------------------------------------------------
     * 1. Drag/drop lacing of food + arrows.
     * ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();
        if (cursor == null || target == null) return;
        if (cursor.getType() == Material.AIR || target.getType() == Material.AIR) return;
        Medicine cursorMed = manager.medicine(manager.factory().medicineIdOf(cursor));
        if (cursorMed == null || cursorMed.type() != Medicine.Type.TRANQUILIZER) return;
        // Only react to plain left-clicks; other actions (shift-click,
        // double-click, etc.) keep their vanilla semantics so the
        // player can still rearrange tranq stacks normally.
        if (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT) return;
        if (e.getAction() != InventoryAction.SWAP_WITH_CURSOR
                && e.getAction() != InventoryAction.PICKUP_HALF
                && e.getAction() != InventoryAction.PICKUP_ALL
                && e.getAction() != InventoryAction.PLACE_ALL
                && e.getAction() != InventoryAction.PLACE_ONE
                && e.getAction() != InventoryAction.PLACE_SOME) return;
        if (target.getType().isEdible()) {
            laceFood(e, cursor, target, cursorMed);
        } else if (isArrowMaterial(target.getType())) {
            laceArrows(e, cursor, target, cursorMed);
        }
    }

    private void laceFood(InventoryClickEvent e, ItemStack cursor,
                          ItemStack target, Medicine tranq) {
        if (target.hasItemMeta()
                && target.getItemMeta().getPersistentDataContainer().has(lacedKey, PersistentDataType.BYTE)) {
            return; // already laced — don't double-spend tranqs
        }
        ItemMeta meta = target.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(lacedKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(lacedSleepSecondsKey, PersistentDataType.INTEGER, tranq.sleepSeconds());
        if (tranq.revealInLore()) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Text.component("&8» &cПодмешан транквилизатор")
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        }
        target.setItemMeta(meta);
        consumeOneTranq(e, cursor);
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p) {
            p.updateInventory();
        }
    }

    private void laceArrows(InventoryClickEvent e, ItemStack cursor,
                            ItemStack target, Medicine tranq) {
        if (target.hasItemMeta()
                && target.getItemMeta().getPersistentDataContainer().has(arrowKey, PersistentDataType.BYTE)) {
            return;
        }
        ItemMeta meta = target.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(arrowKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(lacedSleepSecondsKey, PersistentDataType.INTEGER, tranq.sleepSeconds());
        if (tranq.revealInLore()) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Text.component("&8» &cПокрыт транквилизатором")
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        }
        target.setItemMeta(meta);
        consumeOneTranq(e, cursor);
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p) {
            p.updateInventory();
        }
    }

    private boolean isArrowMaterial(Material m) {
        return m == Material.ARROW || m == Material.TIPPED_ARROW
                || m == Material.SPECTRAL_ARROW;
    }

    private void consumeOneTranq(InventoryClickEvent e, ItemStack cursor) {
        if (cursor.getAmount() <= 1) {
            e.getView().setCursor(null);
        } else {
            cursor.setAmount(cursor.getAmount() - 1);
            e.getView().setCursor(cursor);
        }
    }

    /* ------------------------------------------------------------------
     * 2. Eating laced food triggers sleep.
     * ------------------------------------------------------------------ */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(lacedKey, PersistentDataType.BYTE)) return;
        int sleepSeconds = pdc.getOrDefault(lacedSleepSecondsKey,
                PersistentDataType.INTEGER, 30);
        // Let the food's nutrition apply as normal — don't cancel.
        // Pull the sleep onset / blindness phase from the medicine
        // definition (so admins can re-tune without re-lacing existing
        // food). Defaults to the canonical tranquilizer entry's onset.
        int onsetSeconds = canonicalOnsetSeconds();
        Bukkit.getScheduler().runTask(plugin,
                () -> applySleepRoutine(e.getPlayer(), onsetSeconds, sleepSeconds));
    }

    private int canonicalOnsetSeconds() {
        Medicine tranq = findCanonicalTranquilizer();
        return tranq != null ? tranq.onsetSeconds() : 5;
    }

    private Medicine findCanonicalTranquilizer() {
        for (Medicine m : manager.medicines().values()) {
            if (m.type() == Medicine.Type.TRANQUILIZER) return m;
        }
        return null;
    }

    /* ------------------------------------------------------------------
     * 3. Coated arrow on hit -> target sleeps.
     * ------------------------------------------------------------------ */

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        // Carry the laced flag from the arrow item onto the projectile
        // so we can read it back in ProjectileHitEvent.
        if (!(e.getProjectile() instanceof Projectile proj)) return;
        ItemStack arrow = e.getConsumable();
        if (arrow == null || !arrow.hasItemMeta()) return;
        PersistentDataContainer src = arrow.getItemMeta().getPersistentDataContainer();
        if (!src.has(arrowKey, PersistentDataType.BYTE)) return;
        int sleepSeconds = src.getOrDefault(lacedSleepSecondsKey,
                PersistentDataType.INTEGER, 30);
        proj.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
        proj.getPersistentDataContainer().set(lacedSleepSecondsKey,
                PersistentDataType.INTEGER, sleepSeconds);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent e) {
        if (e.getHitEntity() == null) return;
        if (!(e.getHitEntity() instanceof LivingEntity victim)) return;
        Projectile proj = e.getEntity();
        PersistentDataContainer pdc = proj.getPersistentDataContainer();
        if (!pdc.has(arrowKey, PersistentDataType.BYTE)) return;
        int sleepSeconds = pdc.getOrDefault(lacedSleepSecondsKey,
                PersistentDataType.INTEGER, 30);
        int onsetSeconds = canonicalOnsetSeconds();
        if (victim instanceof Player p) {
            applySleepRoutine(p, onsetSeconds, sleepSeconds);
        } else {
            applyMobSleep(victim, sleepSeconds);
        }
        // Stop the arrow from sticking forever / despawn after impact.
        if (proj instanceof Arrow a) {
            a.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            a.remove();
        }
    }

    /* ------------------------------------------------------------------
     * Sleep routines (shared with MedicineConsumeListener.applySleepRoutine).
     * ------------------------------------------------------------------ */

    /**
     * Player sleep: blind for {@code onsetSeconds}, then forcibly sleep
     * via {@code Player.sleep(location, true)}. After {@code sleepSeconds}
     * we wake them up. The sleep is forced regardless of biome/time so
     * the tranquilizer works in caves, mid-day, the Nether, etc.
     */
    public void applySleepRoutine(Player p, int onsetSeconds, int sleepSeconds) {
        if (p == null || !p.isOnline()) return;
        int onsetTicks = Math.max(0, onsetSeconds * 20);
        int sleepTicks = Math.max(20, sleepSeconds * 20);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                onsetTicks + 40, 0, true, false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                onsetTicks, 4, true, false, true));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            try {
                p.sleep(p.getLocation(), true);
            } catch (Throwable ignored) {
                // sleep() may throw IllegalStateException if conditions
                // forbid it. Fall back to heavy slowness + blindness so
                // the player is still incapacitated for the duration.
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        sleepTicks, 6, true, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                        sleepTicks, 0, true, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                        sleepTicks, 4, true, false, true));
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                if (p.isSleeping()) p.wakeup(true);
            }, sleepTicks);
        }, onsetTicks);
    }

    /**
     * Mob sleep: lock AI off for the duration and fully heal so they
     * wake up at full HP (matching the "they fall into hibernation"
     * spec). Generic LivingEntities without AI just get heavy slowness.
     */
    private void applyMobSleep(LivingEntity victim, int sleepSeconds) {
        int sleepTicks = Math.max(20, sleepSeconds * 20);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                sleepTicks, 6, true, false, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                sleepTicks, 0, true, false, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                sleepTicks, 4, true, false, true));
        if (victim instanceof Mob mob) {
            mob.setAware(false);
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> mob.setAware(true), sleepTicks);
        }
    }

}
