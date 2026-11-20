package ru.kotmiks.holo.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.kotmiks.holo.KotMiksHoloPlugin;
import ru.kotmiks.holo.config.PluginConfig;
import ru.kotmiks.holo.hologram.Billboard;
import ru.kotmiks.holo.hologram.Hologram;
import ru.kotmiks.holo.hologram.HologramManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class HoloCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "kotmiksholo.admin";
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "movehere", "addline", "setline", "removeline",
            "scale", "billboard", "list", "tp", "reload", "help");

    private final KotMiksHoloPlugin plugin;
    private final HologramManager manager;
    private final PluginConfig config;

    public HoloCommand(KotMiksHoloPlugin plugin, HologramManager manager, PluginConfig config) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
    }

    // ================================================================ execute

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "movehere" -> moveHere(sender, args);
            case "addline" -> addLine(sender, args);
            case "setline" -> setLine(sender, args);
            case "removeline" -> removeLine(sender, args);
            case "scale" -> scale(sender, args);
            case "billboard" -> billboard(sender, args);
            case "list" -> list(sender);
            case "tp" -> tp(sender, args);
            case "reload" -> reload(sender);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        for (Component line : config.help()) {
            sender.sendMessage(line);
        }
    }

    private void create(CommandSender sender, String[] a) {
        if (!(sender instanceof Player player)) {
            send(sender, "player-only");
            return;
        }
        if (a.length < 2) {
            usage(sender, "/holo create <id> [текст]");
            return;
        }
        String id = a[1];
        if (!ID_PATTERN.matcher(id).matches()) {
            send(sender, "invalid-id");
            return;
        }
        if (manager.exists(id)) {
            send(sender, "exists", id(id));
            return;
        }
        String text = a.length > 2 ? join(a, 2) : config.defaultText();
        manager.create(id, holoLocation(player), text);
        send(sender, "created", id(id));
    }

    private void delete(CommandSender sender, String[] a) {
        if (a.length < 2) {
            usage(sender, "/holo delete <id>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        manager.delete(h.getId());
        send(sender, "deleted", id(h.getId()));
    }

    private void moveHere(CommandSender sender, String[] a) {
        if (!(sender instanceof Player player)) {
            send(sender, "player-only");
            return;
        }
        if (a.length < 2) {
            usage(sender, "/holo movehere <id>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        manager.moveTo(h, holoLocation(player));
        send(sender, "moved", id(h.getId()));
    }

    private void addLine(CommandSender sender, String[] a) {
        if (a.length < 3) {
            usage(sender, "/holo addline <id> <текст>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        manager.addLine(h, join(a, 2));
        send(sender, "line-added", id(h.getId()), Placeholder.unparsed("count", String.valueOf(h.getLines().size())));
    }

    private void setLine(CommandSender sender, String[] a) {
        if (a.length < 4) {
            usage(sender, "/holo setline <id> <индекс> <текст>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        int index = parseIndex(sender, h, a[2]);
        if (index < 0) {
            return;
        }
        manager.setLine(h, index, join(a, 3));
        send(sender, "line-set", id(h.getId()), Placeholder.unparsed("index", String.valueOf(index + 1)));
    }

    private void removeLine(CommandSender sender, String[] a) {
        if (a.length < 3) {
            usage(sender, "/holo removeline <id> <индекс>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        int index = parseIndex(sender, h, a[2]);
        if (index < 0) {
            return;
        }
        if (h.getLines().size() <= 1) {
            send(sender, "last-line");
            return;
        }
        manager.removeLine(h, index);
        send(sender, "line-removed", id(h.getId()), Placeholder.unparsed("index", String.valueOf(index + 1)));
    }

    private void scale(CommandSender sender, String[] a) {
        if (a.length < 4) {
            usage(sender, "/holo scale <id> <индекс|all> <число>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        double value;
        try {
            value = Double.parseDouble(a[3].replace(',', '.'));
        } catch (NumberFormatException e) {
            send(sender, "bad-number");
            return;
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0 || value > 64.0) {
            send(sender, "bad-number");
            return;
        }
        int index;
        String target;
        if (a[2].equalsIgnoreCase("all")) {
            index = -1;
            target = "all";
        } else {
            index = parseIndex(sender, h, a[2]);
            if (index < 0) {
                return;
            }
            target = "#" + (index + 1);
        }
        manager.setScale(h, index, value);
        send(sender, "scale-set", id(h.getId()),
                Placeholder.unparsed("target", target),
                Placeholder.unparsed("value", String.valueOf(value)));
    }

    private void billboard(CommandSender sender, String[] a) {
        if (a.length < 3) {
            usage(sender, "/holo billboard <id> <CENTER|VERTICAL|HORIZONTAL|FIXED>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        Billboard mode = Billboard.parse(a[2]);
        if (mode == null) {
            send(sender, "billboard-invalid");
            return;
        }
        manager.setBillboard(h, mode);
        send(sender, "billboard-set", id(h.getId()), Placeholder.unparsed("mode", mode.name()));
    }

    private void list(CommandSender sender) {
        Collection<Hologram> all = manager.all();
        if (all.isEmpty()) {
            send(sender, "list-empty");
            return;
        }
        send(sender, "list-header", Placeholder.unparsed("count", String.valueOf(all.size())));
        for (Hologram h : all) {
            String coords = String.format(Locale.US, " %s %.1f %.1f %.1f • строк: %d%s",
                    h.getWorldName(), h.getX(), h.getY(), h.getZ(), h.getLines().size(),
                    h.isEnabled() ? "" : " (выкл)");
            Component entry = Component.text(" • ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(h.getId(), h.isEnabled() ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY)
                            .clickEvent(ClickEvent.runCommand("/holo tp " + h.getId()))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Нажмите, чтобы телепортироваться", NamedTextColor.GRAY))))
                    .append(Component.text(coords, NamedTextColor.GRAY));
            sender.sendMessage(entry);
        }
    }

    private void tp(CommandSender sender, String[] a) {
        if (!(sender instanceof Player player)) {
            send(sender, "player-only");
            return;
        }
        if (a.length < 2) {
            usage(sender, "/holo tp <id>");
            return;
        }
        Hologram h = need(sender, a[1]);
        if (h == null) {
            return;
        }
        World world = Bukkit.getWorld(h.getWorldName());
        if (world == null) {
            send(sender, "world-missing", Placeholder.unparsed("world", h.getWorldName()));
            return;
        }
        Location target = new Location(world, h.getX(), h.getY(), h.getZ(), player.getLocation().getYaw(),
                player.getLocation().getPitch());
        player.teleportAsync(target);
        send(sender, "teleported", id(h.getId()));
    }

    private void reload(CommandSender sender) {
        plugin.reloadAll();
        send(sender, "reloaded", Placeholder.unparsed("count", String.valueOf(manager.count())));
    }

    // ================================================================ helpers

    /** Позиция новой голограммы: на уровне глаз игрока; для FIXED-режима голограмма повёрнута лицом к игроку. */
    private Location holoLocation(Player player) {
        Location loc = player.getEyeLocation();
        float yaw = loc.getYaw() + 180.0f;
        while (yaw > 180.0f) {
            yaw -= 360.0f;
        }
        loc.setYaw(yaw);
        loc.setPitch(0.0f);
        return loc;
    }

    private Hologram need(CommandSender sender, String id) {
        Hologram h = manager.get(id);
        if (h == null) {
            send(sender, "not-found", id(id));
        }
        return h;
    }

    /** @return индекс с 0 или -1 (ошибка уже отправлена) */
    private int parseIndex(CommandSender sender, Hologram h, String raw) {
        int max = h.getLines().size();
        try {
            int n = Integer.parseInt(raw);
            if (n >= 1 && n <= max) {
                return n - 1;
            }
        } catch (NumberFormatException ignored) {
            // fallthrough
        }
        send(sender, "bad-index", Placeholder.unparsed("max", String.valueOf(max)));
        return -1;
    }

    private static TagResolver id(String id) {
        return Placeholder.unparsed("id", id);
    }

    private void usage(CommandSender sender, String usage) {
        send(sender, "usage", Placeholder.unparsed("usage", usage));
    }

    private void send(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(config.prefixed(key, resolvers));
    }

    private static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ================================================================ tab complete

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] a) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (a.length == 1) {
            return filter(SUBCOMMANDS, a[0]);
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        boolean needsId = sub.equals("delete") || sub.equals("movehere") || sub.equals("addline")
                || sub.equals("setline") || sub.equals("removeline") || sub.equals("scale")
                || sub.equals("billboard") || sub.equals("tp");

        if (a.length == 2 && needsId) {
            List<String> ids = new ArrayList<>();
            for (Hologram h : manager.all()) {
                ids.add(h.getId());
            }
            return filter(ids, a[1]);
        }
        if (a.length == 3) {
            Hologram h = manager.get(a[1]);
            if (h == null) {
                return List.of();
            }
            if (sub.equals("setline") || sub.equals("removeline")) {
                return filter(indexes(h, false), a[2]);
            }
            if (sub.equals("scale")) {
                return filter(indexes(h, true), a[2]);
            }
            if (sub.equals("billboard")) {
                List<String> modes = new ArrayList<>();
                for (Billboard b : Billboard.values()) {
                    modes.add(b.name());
                }
                return filter(modes, a[2]);
            }
        }
        if (a.length == 4 && sub.equals("scale")) {
            return filter(List.of("0.5", "1.0", "1.3", "1.6", "2.0", "3.0"), a[3]);
        }
        return List.of();
    }

    private static List<String> indexes(Hologram h, boolean withAll) {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= h.getLines().size(); i++) {
            out.add(String.valueOf(i));
        }
        if (withAll) {
            out.add("all");
        }
        return out;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
