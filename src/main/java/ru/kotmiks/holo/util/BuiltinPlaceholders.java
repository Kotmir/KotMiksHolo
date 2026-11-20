package ru.kotmiks.holo.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Встроенные плейсхолдеры (работают и без PlaceholderAPI):
 * <ul>
 *   <li>{@code {wg_players:<region>}} — игроков в регионе (по всем мирам)</li>
 *   <li>{@code {wg_players:<world>:<region>}} — игроков в регионе конкретного мира</li>
 * </ul>
 */
public final class BuiltinPlaceholders {

    private static final Pattern WG_PLAYERS = Pattern.compile(
            "\\{wg_players:([^:{}]+)(?::([^:{}]+))?\\}", Pattern.CASE_INSENSITIVE);

    private BuiltinPlaceholders() {
    }

    public static boolean containsAny(String text) {
        return text != null && text.indexOf('{') >= 0 && WG_PLAYERS.matcher(text).find();
    }

    public static String apply(String text) {
        if (text == null || text.indexOf('{') < 0) {
            return text;
        }
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
}
