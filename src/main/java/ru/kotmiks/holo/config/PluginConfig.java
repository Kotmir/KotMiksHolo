package ru.kotmiks.holo.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kotmiks.holo.hologram.Alignment;
import ru.kotmiks.holo.hologram.Billboard;
import ru.kotmiks.holo.hologram.HologramDefaults;
import ru.kotmiks.holo.packet.DisplayMeta;
import ru.kotmiks.holo.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/** Загрузка config.yml: настройки, значения по умолчанию и сообщения. */
public final class PluginConfig {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    private int visibilityInterval = 5;
    private boolean placeholderApi = true;
    private String defaultText = "<white>Новая голограмма";
    private HologramDefaults defaults;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();

        visibilityInterval = Math.max(1, cfg.getInt("settings.visibility-check-interval", 5));
        placeholderApi = cfg.getBoolean("settings.placeholderapi", true);
        defaultText = cfg.getString("settings.default-text", "<white>Новая голограмма");
        DisplayMeta.setIndexShift(cfg.getInt("advanced.metadata-index-shift", 0));

        int brightness = cfg.getInt("defaults.brightness", -1);
        if (brightness < -1 || brightness > 15) {
            brightness = -1;
        }
        defaults = new HologramDefaults(
                Math.max(1.0, cfg.getDouble("defaults.view-distance", 48.0)),
                Math.max(0, cfg.getInt("defaults.update-interval", 20)),
                Billboard.parseOr(cfg.getString("defaults.billboard"), Billboard.CENTER),
                cfg.getBoolean("defaults.shadow", true),
                cfg.getBoolean("defaults.see-through", false),
                ColorUtil.parseArgb(cfg.get("defaults.background"), 0x00000000),
                Alignment.parseOr(cfg.getString("defaults.alignment"), Alignment.CENTER),
                Math.max(1, cfg.getInt("defaults.line-width", 200)),
                brightness
        );
    }

    public int visibilityInterval() {
        return visibilityInterval;
    }

    public boolean placeholderApiEnabled() {
        return placeholderApi;
    }

    public String defaultText() {
        return defaultText;
    }

    public HologramDefaults defaults() {
        return defaults;
    }

    /** Сообщение с префиксом. Плейсхолдеры — через Placeholder.unparsed("name", value) → тег &lt;name&gt;. */
    public Component prefixed(String key, TagResolver... resolvers) {
        String prefix = cfg.getString("messages.prefix", "");
        String raw = cfg.getString("messages." + key, "<red>Missing message: " + key);
        return MM.deserialize(prefix + raw, resolvers);
    }

    public List<Component> help() {
        List<Component> out = new ArrayList<>();
        for (String line : cfg.getStringList("messages.help")) {
            out.add(MM.deserialize(line));
        }
        return out;
    }
}
