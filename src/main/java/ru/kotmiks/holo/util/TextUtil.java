package ru.kotmiks.holo.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор текста голограмм: MiniMessage + legacy (&amp;a, &amp;l, §a) + HEX (&amp;#RRGGBB).
 * <p>
 * Логика:
 * <ul>
 *   <li>Legacy-коды переводятся в теги MiniMessage, затем строка один раз передаётся в MiniMessage.</li>
 * </ul>
 */
public final class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();


    // §x§R§R§G§G§B§B (формат PlaceholderAPI/Bukkit для HEX)
    private static final Pattern SECTION_HEX = Pattern.compile(
            "§[xX]§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])");
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private TextUtil() {
    }

    /** Разбивает сырой текст на строки: поддерживает и настоящий перенос, и литерал "\n". */
    public static List<String> splitLines(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            out.add("");
            return out;
        }
        String normalized = raw.replace("\\n", "\n");
        for (String part : normalized.split("\n", -1)) {
            out.add(part);
        }
        return out;
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        try {
            // Единый путь: legacy-коды (&a, &#RRGGBB, §a) -> теги MiniMessage, затем ОДИН вызов deserialize.
            // Голые #RRGGBB не трогаем — они часть тегов <#RRGGBB> и <gradient:#RRGGBB:#RRGGBB>.
            return MM.deserialize(legacyToMini(input));
        } catch (Exception ex) {
            return Component.text(input);
        }
    }

    public static String toJson(String raw) {
        return GsonComponentSerializer.gson().serialize(parse(raw));
    }

    /** Переводит только явный legacy-синтаксис (&amp;x, &amp;#RRGGBB, §x, §x§r§r...) в теги MiniMessage. */
    static String legacyToMini(String input) {
        if (input.indexOf('&') < 0 && input.indexOf('§') < 0) {
            return input;
        }
        String s = SECTION_HEX.matcher(input).replaceAll("&#$1$2$3$4$5$6");
        s = s.replace('§', '&');
        Matcher hex = HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (hex.find()) {
            hex.appendReplacement(sb, Matcher.quoteReplacement("<#" + hex.group(1) + ">"));
        }
        hex.appendTail(sb);

        Matcher code = CODE.matcher(sb.toString());
        StringBuilder out = new StringBuilder();
        while (code.find()) {
            code.appendReplacement(out, Matcher.quoteReplacement(codeToTag(Character.toLowerCase(code.group(1).charAt(0)))));
        }
        code.appendTail(out);
        return out.toString();
    }

    private static String codeToTag(char c) {
        return switch (c) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }
}
