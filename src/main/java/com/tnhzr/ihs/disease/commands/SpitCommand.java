package com.tnhzr.ihs.disease.commands;

import com.tnhzr.ihs.disease.DiseaseManager;
import com.tnhzr.ihs.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class SpitCommand implements CommandExecutor {

    private final DiseaseManager manager;

    public SpitCommand(DiseaseManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            manager.plugin().locale().send(sender, "common.only_players");
            return true;
        }
        if (!manager.plugin().configs().main().getBoolean("disease.spit.enabled", true)) {
            return true;
        }
        if (!p.hasPermission("ihs.spit")) {
            manager.plugin().locale().send(p, "common.no_permission");
            return true;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = manager.plugin().configs().main()
                .getLong("disease.spit.cooldown_seconds", 4) * 1000L;
        Long last = manager.spitCooldown().get(p.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            long left = (cooldownMs - (now - last)) / 1000L;
            manager.plugin().locale().send(p, "spit.cooldown",
                    Map.of("seconds", String.valueOf(left + 1)));
            return true;
        }
        manager.spitCooldown().put(p.getUniqueId(), now);
        manager.transmissionEvents().manualSpit(p);
        return true;
    }
}
