package ru.kotmiks.holo.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Встроенные плейсхолдеры:
 * <ul>
 *   <li>{@code {wg_players:<region>}} — игроков в регионе WorldGuard (по всем мирам)</li>
 *   <li>{@code {wg_players:<world>:<region>}} — игроков в регионе конкретного мира</li>
 *   <li>{@code {svc_players}} — онлайн-игроков с модом Simple Voice Chat
 *       (через PlaceholderAPI: {@code %vcplaceholder_installed%} == "true")</li>
 * </ul>
 */
public final class BuiltinPlaceholders {

    private static final Pattern WG_PLAYERS = Pattern.compile(
            "\\{wg_players:([^:{}]+)(?::([^:{}]+))?\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVC_PLAYERS = Pattern.compile("\\{svc_players\\}", Pattern.CASE_INSENSITIVE);

    private static final String SVC_PAPI = "%vcplaceholder_installed%";
    private static final long SVC_TTL_MS = 1000L;

    private static long svcTime;
    private static int svcValue;

    private BuiltinPlaceholders() {
    }

    public static boolean containsAny(String text) {
        if (text == null || text.indexOf('{') < 0) {
            return false;
        }
        return WG_PLAYERS.matcher(text).find() || SVC_PLAYERS.matcher(text).find();
    }

    public static String apply(String text) {
        if (text == null || text.indexOf('{') < 0) {
            return text;
        }
        text = applyWg(text);
        text = applySvc(text);
        return text;
    }

    private static String applyWg(String text) {
        Matcher m = WG_PLAYERS.matcher(text);
        if (!m.find()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        do {
            String first = m.group(1);
            String second = m.group(2);
            int count;
            if (second == null) {
                count = WorldGuardHook.countPlayersInRegion(null, first);
            } else {
                World world = Bukkit.getWorld(first);
                count = world == null ? 0 : WorldGuardHook.countPlayersInRegion(world, second);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(count)));
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private static String applySvc(String text) {
        Matcher m = SVC_PLAYERS.matcher(text);
        if (!m.find()) {
            return text;
        }
        // считаем один раз на все вхождения в строке
        return m.replaceAll(Matcher.quoteReplacement(String.valueOf(countSvcPlayers())));
    }

    /**
     * Игроки с Simple Voice Chat. Без PlaceholderAPI — 0.
     * Результат кэшируется на 1 секунду (обход всех игроков + PAPI не должен идти на каждый пакет),
     * вне главного потока отдаётся последнее закэшированное значение.
     */
    public static synchronized int countSvcPlayers() {
        if (!Papi.isAvailable() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return 0;
        }
        long now = System.currentTimeMillis();
        if (now - svcTime < SVC_TTL_MS || !Bukkit.isPrimaryThread()) {
            return svcValue;
        }
        int count = 0;
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if ("true".equalsIgnoreCase(Papi.apply(p, SVC_PAPI).trim())) {
                    count++;
                }
            }
        } catch (Throwable t) {
            count = 0;
        }
        svcValue = count;
        svcTime = now;
        return count;
    }

    public static synchronized void clearCache() {
        svcTime = 0L;
        svcValue = 0;
    }
}
