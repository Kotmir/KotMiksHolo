package ru.kotmiks.holo.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Расширение PlaceholderAPI: {@code %kotmiksholo_wg_players_<region_id>%}.
 * Класс зависит от PlaceholderAPI, поэтому обращаться к нему нужно только внутри try/catch
 * и только если PlaceholderAPI установлен.
 */
public final class KotMiksExpansion extends PlaceholderExpansion {

    private static KotMiksExpansion instance;

    private final JavaPlugin plugin;

    private KotMiksExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return true, если расширение зарегистрировано */
    public static synchronized boolean registerFor(JavaPlugin plugin) {
        unregisterIfAny();
        instance = new KotMiksExpansion(plugin);
        return instance.register();
    }

    public static synchronized void unregisterIfAny() {
        if (instance != null) {
            try {
                instance.unregister();
            } catch (Throwable ignored) {
                // PlaceholderAPI уже мог быть выключен
            }
            instance = null;
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        return "kotmiksholo";
    }

    @Override
    public @NotNull String getAuthor() {
        return "KotMiks";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("wg_players_")) {
            String region = params.substring("wg_players_".length());
            return String.valueOf(WorldGuardHook.countPlayersInRegion(null, region));
        }
        return null;
    }
}
