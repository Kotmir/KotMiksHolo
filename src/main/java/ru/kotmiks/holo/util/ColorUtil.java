package ru.kotmiks.holo.util;

public final class ColorUtil {

    private ColorUtil() {
    }

    /**
     * Разбирает ARGB: число, "#AARRGGBB", "#RRGGBB" (альфа = FF), "0x...", либо "0".
     */
    public static int parseArgb(Object value, int def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return def;
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        } else if (s.matches("-?\\d+")) {
            try {
                return (int) Long.parseLong(s);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        try {
            if (s.length() == 6) {
                return 0xFF000000 | Integer.parseInt(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException ignored) {
            // fallthrough
        }
        return def;
    }

    public static String toHex(int argb) {
        return String.format("#%08X", argb);
    }
}
