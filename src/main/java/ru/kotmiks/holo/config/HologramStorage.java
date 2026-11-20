package ru.kotmiks.holo.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.kotmiks.holo.hologram.Alignment;
import ru.kotmiks.holo.hologram.Billboard;
import ru.kotmiks.holo.hologram.HoloLine;
import ru.kotmiks.holo.hologram.Hologram;
import ru.kotmiks.holo.hologram.HologramDefaults;
import ru.kotmiks.holo.util.ColorUtil;
import ru.kotmiks.holo.util.TextUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Чтение и запись holograms.yml. */
public final class HologramStorage {

    private final File file;
    private final Logger log;

    public HologramStorage(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    // ------------------------------------------------------------ load

    public Map<String, Hologram> load(HologramDefaults d) {
        Map<String, Hologram> result = new LinkedHashMap<>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("holograms");
        if (root == null) {
            return result;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            try {
                Hologram h = parse(id, s, d);
                result.put(id.toLowerCase(Locale.ROOT), h);
            } catch (Exception ex) {
                log.warning("Не удалось загрузить голограмму '" + id + "': " + ex.getMessage());
            }
        }
        return result;
    }

    private Hologram parse(String id, ConfigurationSection s, HologramDefaults d) {
        Hologram h = new Hologram(id, d);
        h.setEnabled(s.getBoolean("enabled", true));
        h.setWorldName(s.getString("world", "world"));
        h.setX(s.getDouble("x", 0.0));
        h.setY(s.getDouble("y", 64.0));
        h.setZ(s.getDouble("z", 0.0));
        h.setYaw((float) s.getDouble("yaw", 0.0));
        h.setPitch((float) s.getDouble("pitch", 0.0));

        h.setViewDistance(Math.max(1.0, s.getDouble("view-distance", d.viewDistance())));
        h.setUpdateInterval(Math.max(0, s.getInt("update-interval", d.updateInterval())));
        h.setBillboard(Billboard.parseOr(s.getString("billboard"), d.billboard()));
        h.setShadow(s.getBoolean("shadow", d.shadow()));
        h.setSeeThrough(s.getBoolean("see-through", d.seeThrough()));
        h.setBackground(ColorUtil.parseArgb(s.get("background"), d.background()));
        h.setAlignment(Alignment.parseOr(s.getString("alignment"), d.alignment()));
        h.setLineWidth(Math.max(1, s.getInt("line-width", d.lineWidth())));
        int br = s.getInt("brightness", d.brightness());
        h.setBrightness(br < 0 ? -1 : Math.min(15, br));

        List<?> rawLines = s.getList("lines");
        if (rawLines != null) {
            for (Object o : rawLines) {
                h.getLines().add(parseLine(o));
            }
        }
        if (h.getLines().isEmpty()) {
            h.getLines().add(HoloLine.of(""));
        }
        h.rebuildEntities();
        return h;
    }

    private HoloLine parseLine(Object o) {
        HoloLine line = new HoloLine();
        if (o instanceof Map<?, ?> map) {
            line.setTextLines(readText(map.get("text")));
            Object scale = map.get("scale");
            if (scale != null) {
                readScale(line, scale);
            }
            Object spacing = map.get("spacing");
            if (spacing != null) {
                line.setSpacing(number(spacing, HoloLine.DEFAULT_SPACING));
            }
            Object offset = map.get("offset-y");
            if (offset != null) {
                line.setOffsetY(number(offset, 0.0));
            }
        } else if (o instanceof List<?> list) {
            line.setTextLines(readText(list));
        } else {
            line.setText(String.valueOf(o));
        }
        return line;
    }

    private List<String> readText(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object item : list) {
                out.addAll(TextUtil.splitLines(String.valueOf(item)));
            }
        } else if (o != null) {
            out.addAll(TextUtil.splitLines(String.valueOf(o)));
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }

    private void readScale(HoloLine line, Object o) {
        if (o instanceof List<?> list) {
            if (list.size() >= 3) {
                line.setScale(number(list.get(0), 1.0), number(list.get(1), 1.0), number(list.get(2), 1.0));
            } else if (list.size() == 1) {
                line.setScale(number(list.get(0), 1.0));
            }
        } else {
            line.setScale(number(o, 1.0));
        }
    }

    private double number(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(o).replace(',', '.'));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ------------------------------------------------------------ save

    public String serialize(Collection<Hologram> holograms) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.options().header("KotMiksHolo — голограммы. Формат описан в комментариях исходного файла / README.");
        for (Hologram h : holograms) {
            String p = "holograms." + h.getId() + ".";
            yml.set(p + "enabled", h.isEnabled());
            yml.set(p + "world", h.getWorldName());
            yml.set(p + "x", round(h.getX()));
            yml.set(p + "y", round(h.getY()));
            yml.set(p + "z", round(h.getZ()));
            yml.set(p + "yaw", round(h.getYaw()));
            yml.set(p + "pitch", round(h.getPitch()));
            yml.set(p + "view-distance", h.getViewDistance());
            yml.set(p + "update-interval", h.getUpdateInterval());
            yml.set(p + "billboard", h.getBillboard().name());
            yml.set(p + "shadow", h.isShadow());
            yml.set(p + "see-through", h.isSeeThrough());
            yml.set(p + "background", ColorUtil.toHex(h.getBackground()));
            yml.set(p + "alignment", h.getAlignment().name());
            yml.set(p + "line-width", h.getLineWidth());
            yml.set(p + "brightness", h.getBrightness());

            List<Map<String, Object>> lines = new ArrayList<>();
            for (HoloLine line : h.getLines()) {
                Map<String, Object> m = new LinkedHashMap<>();
                List<String> text = line.getText();
                m.put("text", text.size() == 1 ? text.get(0) : new ArrayList<>(text));
                if (line.isUniformScale()) {
                    m.put("scale", line.getSx());
                } else {
                    List<Double> scale = new ArrayList<>();
                    scale.add(line.getSx());
                    scale.add(line.getSy());
                    scale.add(line.getSz());
                    m.put("scale", scale);
                }
                m.put("spacing", line.getSpacing());
                if (line.getOffsetY() != 0.0) {
                    m.put("offset-y", line.getOffsetY());
                }
                lines.add(m);
            }
            yml.set(p + "lines", lines);
        }
        return yml.saveToString();
    }

    public void write(String data) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
