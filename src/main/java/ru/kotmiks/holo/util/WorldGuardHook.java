package ru.kotmiks.holo.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Безопасная интеграция с WorldGuard (классы WorldGuard вынесены в {@link WorldGuardImpl}
 * и грузятся только при наличии плагина).
 * <p>
 * Результат подсчёта кэшируется на 1 секунду: один и тот же регион опрашивается многими
 * голограммами и зрителями, а обход игроков не должен выполняться на каждый пакет.
 */
public final class WorldGuardHook {

    private static final long TTL_MS = 1000L;

    private record Entry(long time, int value) {
    }

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static volatile WorldGuardImpl impl;

    private WorldGuardHook() {
    }

    /** Вызывать из onEnable. Логирует, найден ли WorldGuard. */
    public static void init(Logger log) {
        impl = null;
        CACHE.clear();
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            log.info("WorldGuard не найден — {wg_players:...} будет всегда возвращать 0.");
            return;
        }
        try {
            WorldGuardImpl candidate = new WorldGuardImpl();
            candidate.probe();
            impl = candidate;
            log.info("WorldGuard найден — плейсхолдер {wg_players:<регион>} включён.");
        } catch (Throwable t) {
            impl = null;
            log.log(Level.WARNING, "WorldGuard найден, но его API недоступен — {wg_players:...} вернёт 0", t);
        }
    }

    public static boolean isAvailable() {
        return impl != null;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Количество онлайн-игроков в регионе.
     *
     * @param world    мир; если null — регион ищется во всех загруженных мирах (результаты суммируются)
     * @param regionId имя региона (без учёта регистра)
     * @return число игроков; 0, если WorldGuard нет или регион не найден
     */
    public static int countPlayersInRegion(World world, String regionId) {
        WorldGuardImpl wg = impl;
        if (wg == null || regionId == null || regionId.isEmpty()) {
            return 0;
        }
        String key = (world == null ? "*" : world.getName().toLowerCase(Locale.ROOT))
                + ":" + regionId.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Entry cached = CACHE.get(key);
        if (cached != null && now - cached.time() < TTL_MS) {
            return cached.value();
        }
        if (!Bukkit.isPrimaryThread()) {
            // API Bukkit/WorldGuard трогаем только из главного потока
            return cached == null ? 0 : cached.value();
        }
        int value;
        try {
            value = wg.count(world, regionId);
        } catch (Throwable t) {
            value = 0;
        }
        CACHE.put(key, new Entry(now, value));
        return value;
    }
}
