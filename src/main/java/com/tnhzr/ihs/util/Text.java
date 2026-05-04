package com.tnhzr.ihs.util;

import com.tnhzr.ihs.ImmersiveHealthSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {}

    public static Component component(String legacy) {
        if (legacy == null) return Component.empty();
        return LEGACY.deserialize(legacy);
    }

    public static List<Component> components(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines == null) return out;
        for (String line : lines) out.add(component(line));
        return out;
    }

    public static String fillPlaceholders(String input, Map<String, String> values) {
        if (input == null) return "";
        String s = input;
        for (Map.Entry<String, String> e : values.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }

    public static void send(CommandSender to, String legacy) {
        if (legacy == null || legacy.isEmpty()) return;
        String prefix = ImmersiveHealthSystem.getInstance().locale().raw("prefix");
        to.sendMessage(component(prefix + legacy));
    }

    public static void sendRaw(CommandSender to, String legacy) {
        if (legacy == null || legacy.isEmpty()) return;
        to.sendMessage(component(legacy));
    }

    /** Fetch a localized message body (no prefix) with placeholders applied. */
    public static String message(String key, Map<String, String> placeholders) {
        return ImmersiveHealthSystem.getInstance().locale().raw(key, placeholders);
    }
}
