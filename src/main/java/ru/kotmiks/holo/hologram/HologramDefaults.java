package ru.kotmiks.holo.hologram;

/** Значения по умолчанию для голограмм (секция defaults в config.yml). */
public record HologramDefaults(
        double viewDistance,
        int updateInterval,
        Billboard billboard,
        boolean shadow,
        boolean seeThrough,
        int background,
        Alignment alignment,
        int lineWidth,
        int brightness
) {
}
