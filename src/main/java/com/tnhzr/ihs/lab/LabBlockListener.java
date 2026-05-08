package com.tnhzr.ihs.lab;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
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
     * Forces the given block to {@code note_block[note=N,instrument=harp,
     * powered=false]} where N encodes the cardinal facing (0=N, 1=E,
     * 2=S, 3=W). Our resourcepack's {@code assets/minecraft/blockstates/
     * note_block.json} maps each of these note values to the IHS
     * laboratory model rotated by 0/90/180/270°.
     *
     * <p>Re-sends the block to nearby clients so the new state is
     * applied even if the underlying type was already a note_block.</p>
     */
    static void applyLabBlockState(Block block, int facingNote) {
        int safeNote = ((facingNote % 4) + 4) % 4;
        block.setType(Material.NOTE_BLOCK, false);
        BlockData bd = block.getBlockData();
        if (bd instanceof NoteBlock nb) {
            nb.setInstrument(Instrument.PIANO); // "harp" in MC = PIANO in Bukkit's enum
            nb.setNote(new Note(safeNote));
            nb.setPowered(false);
            block.setBlockData(nb, false);
        }
        for (Player nearby : block.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(block.getLocation()) < 96 * 96) {
                nearby.sendBlockChange(block.getLocation(), block.getBlockData());
            }
        }
    }

    /** Backwards-compatible overload: defaults to NORTH (note=0). Used
     *  by the lab-block migration path that has no facing context. */
    static void applyLabBlockState(Block block) {
        applyLabBlockState(block, 0);
    }

    /**
     * Maps a player's yaw to a cardinal facing note (0..3) such that the
     * lab's "front" face points back at the player who placed it.
     * Vanilla yaw runs 0=south, 90=west, 180=north, 270=east; we treat
     * the yaw modulo 360 in 90° quadrants centred on each cardinal.
     */
    static int facingNoteForPlayer(Player p) {
        float yaw = (p.getLocation().getYaw() % 360f + 360f) % 360f;
        // Quadrant centred on cardinal: 0=south=y0..45 & 315..360,
        // 1=west=45..135, 2=north=135..225, 3=east=225..315.
        if (yaw < 45f || yaw >= 315f) return 2;     // facing south -> front north
        if (yaw < 135f) return 3;                   // facing west  -> front east
        if (yaw < 225f) return 0;                   // facing north -> front south(==N variant)
        return 1;                                   // facing east  -> front west
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        if (!manager.isLabItem(inHand)) return;
        Block placed = e.getBlockPlaced();
        manager.createAt(placed.getLocation());
        int note = facingNoteForPlayer(e.getPlayer());
        applyLabBlockState(placed, note);
        manager.sounds().play(placed.getLocation(), "place");
        plugin.getLogger().fine(() -> "[lab] place at " + placed.getLocation()
                + " (note=" + note + ") by " + e.getPlayer().getName());
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
        // Tool-tier gate: laboratory needs at least the configured pickaxe.
        Player breaker = e.getPlayer();
        ItemStack tool = breaker.getInventory().getItemInMainHand();
        if (!manager.canBreakLab(tool)) {
            e.setCancelled(true);
            manager.sounds().play(loc, "denied");
            plugin.locale().send(breaker, "lab.tool_too_weak");
            return;
        }
        // We own this block — force-uncancel so the break completes even
        // if another plugin tried to block it (CE's own note-block hook
        // is the most common culprit on servers running CE).
        if (e.isCancelled()) e.setCancelled(false);
        e.setDropItems(false);
        manager.sounds().play(loc, "break");
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
                + breaker.getName());
    }

    /**
     * Suppresses the vanilla note-block harp note that plays whenever a
     * player left-clicks the block. {@code Block.attack()} (which is
     * what plays the note) is called once per LMB-press from the
     * {@link PlayerInteractEvent} pipeline; subsequent mining-progress
     * ticks fire {@link BlockDamageEvent} independently and don't
     * re-trigger the note. So cancelling LEFT_CLICK_BLOCK at HIGHEST
     * silences the harp without interfering with mining.
     *
     * <p>If the player's tool is too weak for the configured break
     * tier, we additionally play the "denied" feedback sound and let
     * BlockBreakEvent's separate cancel happen as soon as Vanilla
     * tries to complete the break. The user can still tap the block
     * — they just won't break it without a proper pickaxe.</p>
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        Laboratory lab = manager.at(e.getClickedBlock().getLocation());
        if (lab == null) return;
        // Always cancel — this is the only path that calls
        // NoteBlock.attack() and emits the note. Mining progress runs
        // through BlockDamageEvent which fires regardless.
        e.setCancelled(true);
        ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
        if (!manager.canBreakLab(tool)) {
            manager.sounds().play(e.getClickedBlock().getLocation(), "denied");
        }
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
        manager.sounds().play(e.getClickedBlock().getLocation(), "interact");
        LabGui gui = manager.openGuis().computeIfAbsent(lab.location(),
                k -> new LabGui(plugin, manager, lab));
        gui.open(p);
    }
}
