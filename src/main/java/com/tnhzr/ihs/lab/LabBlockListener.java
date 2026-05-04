package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class LabBlockListener implements Listener {

    private final ImmersiveHealthSystem plugin;
    private final LabManager manager;

    public LabBlockListener(ImmersiveHealthSystem plugin, LabManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Forces the given block to {@code note_block[note=0,instrument=harp,
     * powered=false]} — the exact state our resourcepack's
     * {@code assets/minecraft/blockstates/note_block.json} maps to
     * {@code ihs:block/laboratory}. Re-sends the block to nearby
     * clients so the new state is applied even if the underlying type
     * was already a note_block.
     */
    static void applyLabBlockState(Block block) {
        block.setType(Material.NOTE_BLOCK, false);
        BlockData bd = block.getBlockData();
        if (bd instanceof NoteBlock nb) {
            nb.setInstrument(Instrument.PIANO); // "harp" in MC = PIANO in Bukkit's enum
            nb.setNote(new Note(0));
            nb.setPowered(false);
            block.setBlockData(nb, false);
        }
        for (Player nearby : block.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(block.getLocation()) < 96 * 96) {
                nearby.sendBlockChange(block.getLocation(), block.getBlockData());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        if (!manager.isLabItem(inHand)) return;
        Block placed = e.getBlockPlaced();
        manager.createAt(placed.getLocation());
        applyLabBlockState(placed);
        placed.getWorld().playSound(placed.getLocation(),
                Sound.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1.0F, 0.9F);
        plugin.getLogger().fine(() -> "[lab] place at " + placed.getLocation()
                + " by " + e.getPlayer().getName());
    }

    /**
     * Highest-priority + {@code ignoreCancelled=false} so we still run
     * if a protection / anti-grief plugin (or CraftEngine's own block
     * listener) cancelled the event before we reach it. We re-uncancel
     * inside the handler when we're sure this is OUR block, so the
     * vanilla break still goes through and the user actually sees the
     * lab disappear.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        Laboratory lab = manager.at(loc);
        if (lab == null) return;
        // We own this block — force-uncancel so the break completes even
        // if another plugin tried to block it (CE's own note-block hook
        // is the most common culprit on servers running CE).
        if (e.isCancelled()) e.setCancelled(false);
        e.setDropItems(false);
        // Manually play a metal break sound so the audio cue is the
        // same regardless of what underlying block material the lab
        // happens to be sitting on (vanilla note-block plays a wood
        // sound, which would be confusing).
        loc.getWorld().playSound(loc, Sound.BLOCK_METAL_BREAK,
                SoundCategory.BLOCKS, 1.0F, 0.95F);
        // Drop the lab item itself plus everything stored inside.
        loc.getWorld().dropItemNaturally(loc, manager.createLabItem());
        for (ItemStack item : lab.inputs()) {
            if (item != null) loc.getWorld().dropItemNaturally(loc, item);
        }
        if (lab.result() != null) loc.getWorld().dropItemNaturally(loc, lab.result());
        if (lab.fuel() != null) loc.getWorld().dropItemNaturally(loc, lab.fuel());
        for (var snapshot : lab.pendingCraftSnapshots()) {
            for (ItemStack item : snapshot) {
                if (item != null) loc.getWorld().dropItemNaturally(loc, item);
            }
        }
        manager.remove(lab);
        plugin.getLogger().fine(() -> "[lab] break at " + loc + " by "
                + e.getPlayer().getName());
    }

    /**
     * Right-click handler. Runs at HIGHEST so we beat CraftEngine's own
     * note-block right-click hook (which would otherwise pitch the
     * block up by one). We don't ignore cancelled events because we
     * still want to open the GUI even if a protection plugin
     * pre-cancelled (we own this block).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        Laboratory lab = manager.at(e.getClickedBlock().getLocation());
        if (lab == null) return;
        Player p = e.getPlayer();
        if (p.isSneaking() && e.getItem() != null) return;
        // Always cancel — we own this block, suppress the vanilla
        // note-block pitch-up + any placement of held item against it.
        e.setCancelled(true);
        LabGui gui = manager.openGuis().computeIfAbsent(lab.location(),
                k -> new LabGui(plugin, manager, lab));
        gui.open(p);
    }
}
