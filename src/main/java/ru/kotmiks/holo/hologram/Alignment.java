package ru.kotmiks.holo.hologram;

import java.util.Locale;

public enum Alignment {
    CENTER(0x00),
    LEFT(0x08),
    RIGHT(0x10);

    private final int flagBits;

    Alignment(int flagBits) {
        this.flagBits = flagBits;
    }

    public int flagBits() {
        return flagBits;
    }

    public static Alignment parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Alignment parseOr(String s, Alignment def) {
        Alignment a = parse(s);
        return a == null ? def : a;
    }
}
