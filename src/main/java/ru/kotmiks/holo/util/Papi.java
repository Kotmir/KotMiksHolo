package ru.kotmiks.holo.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Мягкая интеграция с PlaceholderAPI через рефлексию — плагин собирается и работает без PAPI.
 */
public final class Papi {

    private static Method setPlaceholders;

    private Papi() {
    }

    public static void init(boolean enabledInConfig) {
        setPlaceholders = null;
        if (!enabledInConfig || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            setPlaceholders = api.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
        } catch (Throwable t) {
            setPlaceholders = null;
        }
    }

    public static boolean isAvailable() {
        return setPlaceholders != null;
    }

    public static String apply(Player player, String text) {
        if (setPlaceholders == null || text.indexOf('%') < 0) {
            return text;
        }
        try {
            Object result = setPlaceholders.invoke(null, player, text);
            return result == null ? text : (String) result;
        } catch (Throwable t) {
            return text;
        }
    }
}
