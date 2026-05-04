package com.tnhzr.ihs.disease.commands;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import com.tnhzr.ihs.disease.Disease;
import com.tnhzr.ihs.disease.PlayerDiseaseState;
import com.tnhzr.ihs.disease.TransmissionSettings;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin-only debug command for the cough/sneeze/vomit transmission
 * events. The plugin registers one executor and routes the actual event
 * via the command label, so {@code /cough}, {@code /sneeze} and
 * {@code /vomit} all share the same code path.
 *
 * <p>Permission: {@code ihs.debug.symptom}. The sender may be a player
 * (uses themselves as source) or the console (must specify a target).</p>
 */
public final class SymptomCommand implements CommandExecutor, TabCompleter {

    private final ImmersiveHealthSystem plugin;

    public SymptomCommand(ImmersiveHealthSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("ihs.debug.symptom")) {
            plugin.locale().send(sender, "common.no_permission");
            return true;
        }
        String event = command.getName().toLowerCase();
        if (!event.equals("cough") && !event.equals("sneeze") && !event.equals("vomit")) {
            return true;
        }
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                plugin.locale().send(sender, "common.player_not_found");
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            plugin.locale().send(sender, "common.only_players");
            return true;
        }
        Disease disease = pickDisease(target);
        if (disease == null) {
            // Fire a "blank" event so the cosmetic part still plays.
            disease = new Disease("debug", "Debug",
                    Disease.Type.LOCAL, "general", 0.0, Disease.TREMOR_DEFAULT,
                    java.util.Map.of(event, new TransmissionSettings(0.0, 0.0, 0.0)),
                    java.util.List.of());
        }
        plugin.diseases().transmissionEvents().fire(target, event, disease);
        plugin.locale().send(sender, "ihs.debug.symptom_fired", java.util.Map.of(
                "event", event,
                "player", target.getName()));
        return true;
    }

    private Disease pickDisease(Player target) {
        PlayerDiseaseState state = plugin.diseases().state(target.getUniqueId());
        if (state.infections().isEmpty()) return null;
        return plugin.diseases().disease(state.infections().keySet().iterator().next());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("ihs.debug.symptom")) return List.of();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        return List.of();
    }
}
