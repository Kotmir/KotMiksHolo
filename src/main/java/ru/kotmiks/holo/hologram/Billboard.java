package ru.kotmiks.holo.hologram;

import java.util.Locale;

public enum Billboard {
    FIXED(0),
    VERTICAL(1),
    HORIZONTAL(2),
    CENTER(3);

    private final byte id;

    Billboard(int id) {
        this.id = (byte) id;
    }

    public byte id() {
        return id;
    }

    /** @return режим или null, если строка не распознана */
    public static Billboard parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Billboard parseOr(String s, Billboard def) {
        Billboard b = parse(s);
        return b == null ? def : b;
    }
}
