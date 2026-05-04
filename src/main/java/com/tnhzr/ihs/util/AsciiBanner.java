package com.tnhzr.ihs.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startup banner — an ASCII rendering of the IHS plugin icon next to
 * the plugin's name and author. Drawn once on plugin enable so the
 * server console reflects the plugin's identity.
 *
 * <p>The banner is printed via the plugin's own {@link Logger}, which
 * preserves the {@code [ImmersiveHealthSystem]} prefix on every line.
 * No emoji or non-ASCII characters are used so the banner stays
 * readable in {@code latin1}, {@code cp866} and other terminal
 * locales.</p>
 */
public final class AsciiBanner {

    private AsciiBanner() {}

    /**
     * Lines that draw the icon glyph. Generated from the official
     * {@code ihs_icon.png} (downsampled to 36 columns then trimmed to
     * the black outline). Characters carry shading information:
     * <ul>
     *   <li>{@code #} — black outline of the icon block.</li>
     *   <li>{@code @} — bright/lit green face.</li>
     *   <li>{@code %} — dark/shadow green face.</li>
     *   <li>{@code O} — white "eye" highlight.</li>
     * </ul>
     */
    private static final String[] ICON = {
            "   ################      ",
            " ####@@%@%##%@%@@####    ",
            "##@% OO@@@@@@@@OO@%@##   ",
            "##@% OOOO %% OOOO %@##   ",
            "##@@@%%OO %% OO%%%@@##   ",
            "####@@@@%%%%@%%%@@####   ",
            "#####%%@@@%%@@@%%#####   ",
            " ######%%@@@@%%######    ",
            " #########@@#########    ",
            "   ################      ",
            "      ##########         ",
            "        ######           "
    };

    /** Lines drawn to the right of the icon. */
    private static final String[] TEXT = {
            "",
            "",
            "  IMMERSIVE  HEALTH  SYSTEM",
            "  ---------------------------------",
            "  Disease  |  Medicine  |  Laboratory",
            "",
            "  Version  : {version}",
            "  Author   : Tannhauser (tnhzr)",
            "",
            "",
            "",
            ""
    };

    /** Print the banner using the plugin's logger. */
    public static void print(Logger logger, String version) {
        if (logger == null) return;
        int rows = Math.max(ICON.length, TEXT.length);
        for (int i = 0; i < rows; i++) {
            String left  = i < ICON.length ? ICON[i] : "                         ";
            String right = i < TEXT.length ? TEXT[i] : "";
            String line = (left + right).replace("{version}",
                    version == null ? "?" : version);
            logger.log(Level.INFO, line);
        }
    }
}
