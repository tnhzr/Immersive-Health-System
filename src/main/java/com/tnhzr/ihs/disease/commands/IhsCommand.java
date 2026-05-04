package com.tnhzr.ihs.disease.commands;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.disease.Disease;
import com.tnhzr.ihs.disease.PlayerDiseaseState;
import com.tnhzr.ihs.disease.Stage;
import com.tnhzr.ihs.disease.gui.PlayerHealthMenu;
import com.tnhzr.ihs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IhsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("check", "inject", "heal", "modify", "menu", "give",
                    "tremor", "help", "reload");

    private final ImmersiveHealthSystem plugin;

    public IhsCommand(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (!sender.hasPermission("ihs.admin") && !sub.equals("menu") && !sub.equals("help")) {
            plugin.locale().send(sender, "common.no_permission");
            return true;
        }
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "reload" -> {
                plugin.configs().loadAll();
                plugin.locale().load();
                if (plugin.symptoms() != null) plugin.symptoms().load();
                if (plugin.diseases() != null) plugin.diseases().reload();
                if (plugin.medicines() != null) plugin.medicines().reload();
                if (plugin.laboratories() != null) {
                    plugin.laboratories().recipes().load();
                    plugin.laboratories().onConfigReloaded();
                }
                plugin.locale().send(sender, "common.reload_done");
            }
            case "menu" -> {
                if (!(sender instanceof Player p)) {
                    plugin.locale().send(sender, "common.only_players");
                    return true;
                }
                if (!sender.hasPermission("ihs.menu")) {
                    plugin.locale().send(sender, "common.no_permission");
                    return true;
                }
                new PlayerHealthMenu(plugin).open(p, 0);
            }
            case "check" -> handleCheck(sender, args);
            case "inject" -> handleInject(sender, args);
            case "heal" -> handleHeal(sender, args);
            case "modify" -> handleModify(sender, args);
            case "give" -> handleGive(sender, args);
            case "tremor" -> handleTremor(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleGive(CommandSender s, String[] args) {
        if (!s.hasPermission("ihs.give")) {
            plugin.locale().send(s, "common.no_permission");
            return;
        }
        if (args.length < 3) { sendHelp(s); return; }
        OfflinePlayer target = resolve(args[1]);
        if (target == null || !target.isOnline()) {
            plugin.locale().send(s, "common.player_not_found");
            return;
        }
        Player online = target.getPlayer();
        String medicineId = args[2];
        com.tnhzr.ihs.medicine.Medicine medicine =
                plugin.medicines() == null ? null : plugin.medicines().medicine(medicineId);
        if (medicine == null) {
            plugin.locale().send(s, "ihs.medicine_unknown", Map.of("id", medicineId));
            return;
        }
        int count = 1;
        if (args.length >= 4) {
            try { count = Math.max(1, Integer.parseInt(args[3])); }
            catch (NumberFormatException ignored) { /* keep 1 */ }
        }
        org.bukkit.inventory.ItemStack stack = plugin.medicines().factory().create(medicine, count);
        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover =
                online.getInventory().addItem(stack);
        for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
            online.getWorld().dropItemNaturally(online.getLocation(), drop);
        }
        plugin.locale().send(s, "ihs.give_done", Map.of(
                "player", online.getName(),
                "medicine", medicine.name(),
                "count", String.valueOf(count)));
    }

    /**
     * Debug subcommand: force the tremor visual on a player for a fixed
     * number of seconds. Useful for verifying the freezing-overlay
     * effect without having to actually advance a disease scale past
     * the threshold.
     *
     * Usage: {@code /ihs tremor <player> [seconds]}. Default 10 s.
     * {@code /ihs tremor <player> 0} clears any active forced tremor.
     */
    private void handleTremor(CommandSender s, String[] args) {
        if (args.length < 2) {
            Text.send(s, "&cUsage: /ihs tremor <player> [seconds]");
            return;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null || !target.isOnline()) {
            plugin.locale().send(s, "common.player_not_found");
            return;
        }
        int seconds = 10;
        if (args.length >= 3) {
            try { seconds = Math.max(0, Integer.parseInt(args[2])); }
            catch (NumberFormatException ignored) { /* keep default */ }
        }
        Player online = target.getPlayer();
        if (plugin.diseases() == null) {
            Text.send(s, "&cDisease module disabled.");
            return;
        }
        plugin.diseases().forceTremor(online, seconds);
        if (seconds == 0) {
            Text.send(s, "&aCleared forced tremor on &f" + online.getName());
        } else {
            Text.send(s, "&aForced tremor on &f" + online.getName()
                    + "&a for &f" + seconds + "s&a (visual only — no damage).");
        }
    }

    private void sendHelp(CommandSender s) {
        for (String line : plugin.locale().rawList("ihs.help")) Text.send(s, line);
    }

    private void handleCheck(CommandSender s, String[] args) {
        if (args.length < 2) { sendHelp(s); return; }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            plugin.locale().send(s, "common.player_not_found");
            return;
        }
        UUID id = target.getUniqueId();
        PlayerDiseaseState state = plugin.diseases().state(id);
        String mode = args.length >= 3 ? args[2].toLowerCase() : "infection";
        if (mode.equals("scale")) {
            int scale = state.infections().values().stream().mapToInt(Integer::intValue).max().orElse(0);
            plugin.locale().send(s, "ihs.scale_view", Map.of(
                    "player", String.valueOf(target.getName()),
                    "value", String.valueOf(scale)));
            return;
        }
        if (state.infections().isEmpty()) {
            plugin.locale().send(s, "ihs.player_healthy",
                    Map.of("player", String.valueOf(target.getName())));
        } else {
            for (Map.Entry<String, Integer> e : state.infections().entrySet()) {
                Disease d = plugin.diseases().disease(e.getKey());
                String name = d != null ? d.name() : e.getKey();
                Stage st = d != null ? d.stageFor(e.getValue()) : null;
                plugin.locale().send(s, "ihs.player_sick", Map.of(
                        "player", String.valueOf(target.getName()),
                        "infection", name,
                        "stage", st != null ? st.key() : String.valueOf(e.getValue())));
            }
        }
    }

    private void handleInject(CommandSender s, String[] args) {
        if (args.length < 3) { sendHelp(s); return; }
        OfflinePlayer target = resolve(args[1]);
        if (target == null || !target.isOnline()) {
            plugin.locale().send(s, "common.player_not_found");
            return;
        }
        Player online = target.getPlayer();
        String diseaseId = args[2];
        Disease d = plugin.diseases().disease(diseaseId);
        if (d == null) {
            plugin.locale().send(s, "ihs.infection_unknown", Map.of("id", diseaseId));
            return;
        }
        double chance = 100.0;
        if (args.length >= 4) {
            try { chance = Double.parseDouble(args[3]); }
            catch (NumberFormatException ex) { /* ignore */ }
        }
        if (chance >= 100.0 || Math.random() * 100.0 < chance) {
            plugin.diseases().infect(online, d.id(), 1);
            plugin.locale().send(s, "ihs.injected", Map.of(
                    "player", online.getName(),
                    "infection", d.name()));
        } else {
            plugin.locale().send(s, "ihs.injected_failed");
        }
    }

    private void handleHeal(CommandSender s, String[] args) {
        if (args.length < 2) { sendHelp(s); return; }
        OfflinePlayer target = resolve(args[1]);
        if (target == null || !target.isOnline()) {
            plugin.locale().send(s, "common.player_not_found");
            return;
        }
        Player online = target.getPlayer();
        if (args.length >= 3) {
            String diseaseId = args[2];
            if (plugin.diseases().disease(diseaseId) == null) {
                plugin.locale().send(s, "ihs.infection_unknown", Map.of("id", diseaseId));
                return;
            }
            plugin.diseases().heal(online, diseaseId);
            plugin.locale().send(s, "ihs.healed_one", Map.of(
                    "player", online.getName(),
                    "infection", plugin.diseases().disease(diseaseId).name()));
        } else {
            plugin.diseases().heal(online, null);
            plugin.locale().send(s, "ihs.healed_all",
                    Map.of("player", online.getName()));
        }
    }

    private void handleModify(CommandSender s, String[] args) {
        if (args.length < 3) { sendHelp(s); return; }
        OfflinePlayer target = resolve(args[1]);
        if (target == null || !target.isOnline()) {
            plugin.locale().send(s, "common.player_not_found");
            return;
        }
        int value;
        try { value = Integer.parseInt(args[2]); }
        catch (NumberFormatException ex) { sendHelp(s); return; }
        plugin.diseases().modifyScale(target.getPlayer(), value);
        plugin.locale().send(s, "ihs.scale_set", Map.of(
                "player", String.valueOf(target.getName()),
                "value", String.valueOf(value)));
    }

    private OfflinePlayer resolve(String token) {
        try {
            UUID id = UUID.fromString(token);
            return Bukkit.getOfflinePlayer(id);
        } catch (IllegalArgumentException ignored) {}
        Player p = Bukkit.getPlayerExact(token);
        if (p != null) return p;
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(token);
        if (offline != null && offline.hasPlayedBefore()) return offline;
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return SUBCOMMANDS;
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("inject")) {
            return new ArrayList<>(plugin.diseases().diseases().keySet());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("check")) {
            return Arrays.asList("scale", "infection");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("heal")) {
            return new ArrayList<>(plugin.diseases().diseases().keySet());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")
                && plugin.medicines() != null) {
            return new ArrayList<>(plugin.medicines().all().keySet());
        }
        return List.of();
    }
}
