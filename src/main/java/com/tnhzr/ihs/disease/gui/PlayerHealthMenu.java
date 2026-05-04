package com.tnhzr.ihs.disease.gui;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.disease.Disease;
import com.tnhzr.ihs.disease.PlayerDiseaseState;
import com.tnhzr.ihs.disease.Stage;
import com.tnhzr.ihs.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PlayerHealthMenu implements Listener {

    private static final int PAGE_SIZE = 45;

    private final ImmersiveHealthSystem plugin;
    private Inventory inventory;
    private int page;
    private List<Player> players;

    public PlayerHealthMenu(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, int page) {
        this.page = Math.max(0, page);
        this.players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int size = plugin.configs().main().getInt("menu.size", 54);
        if (size % 9 != 0 || size < 18 || size > 54) size = 54;

        this.inventory = Bukkit.createInventory(null, size, plugin.locale().component("menu.title"));

        renderPage();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inventory);
    }

    private void renderPage() {
        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, players.size());
        for (int i = start; i < end; i++) {
            Player p = players.get(i);
            inventory.setItem(i - start, createHead(p));
        }

        if (page > 0) {
            inventory.setItem(inventory.getSize() - 9,
                    navItem(Material.ARROW, plugin.locale().component("menu.prev_page")));
        }
        if (end < players.size()) {
            inventory.setItem(inventory.getSize() - 1,
                    navItem(Material.ARROW, plugin.locale().component("menu.next_page")));
        }
        inventory.setItem(inventory.getSize() - 5, navItem(Material.PAPER,
                plugin.locale().component("menu.page_indicator",
                        Map.of("page", String.valueOf(page + 1)))));
    }

    private ItemStack createHead(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Text.component("&f" + target.getName()));

        PlayerDiseaseState st = plugin.diseases().state(target.getUniqueId());
        List<Component> lore = new ArrayList<>();
        int max = st.infections().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        lore.add(plugin.locale().component("menu.scale_line",
                Map.of("value", String.valueOf(max))));
        if (st.infections().isEmpty()) {
            lore.add(plugin.locale().component("menu.healthy_line"));
        } else {
            for (Map.Entry<String, Integer> e : st.infections().entrySet()) {
                Disease d = plugin.diseases().disease(e.getKey());
                String name = d != null ? d.name() : e.getKey();
                Stage s = d != null ? d.stageFor(e.getValue()) : null;
                lore.add(plugin.locale().component("menu.infection_line", Map.of(
                        "infection", name,
                        "stage", s != null ? s.key() : String.valueOf(e.getValue()))));
            }
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navItem(Material mat, Component name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        e.setCancelled(true);

        if (e.getRawSlot() == inventory.getSize() - 9 && page > 0) {
            page--;
            renderPage();
        } else if (e.getRawSlot() == inventory.getSize() - 1 && (page + 1) * PAGE_SIZE < players.size()) {
            page++;
            renderPage();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }
}
