package com.tnhzr.ihs.util;

public final class TimeParser {

    private TimeParser() {}

    /**
     * Parses values like "10s", "5m", "2h", "1d" into ticks.
     * A bare number is treated as ticks.
     */
    public static long toTicks(String input) {
        if (input == null || input.isEmpty()) return 0L;
        String s = input.trim().toLowerCase();
        char last = s.charAt(s.length() - 1);
        long mult;
        String num;
        switch (last) {
            case 't' -> { mult = 1L; num = s.substring(0, s.length() - 1); }
            case 's' -> { mult = 20L; num = s.substring(0, s.length() - 1); }
            case 'm' -> { mult = 20L * 60L; num = s.substring(0, s.length() - 1); }
            case 'h' -> { mult = 20L * 60L * 60L; num = s.substring(0, s.length() - 1); }
            case 'd' -> { mult = 20L * 60L * 60L * 24L; num = s.substring(0, s.length() - 1); }
            default  -> { mult = 1L; num = s; }
        }
        try { return (long) (Double.parseDouble(num) * mult); }
        catch (NumberFormatException ex) { return 0L; }
    }

    public static long toSeconds(String input) {
        return toTicks(input) / 20L;
    }
}
