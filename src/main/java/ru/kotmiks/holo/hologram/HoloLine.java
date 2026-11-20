package ru.kotmiks.holo.hologram;

import ru.kotmiks.holo.util.Papi;
import ru.kotmiks.holo.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Один блок голограммы = один TextDisplay. Внутри блока может быть несколько строк текста.
 */
public final class HoloLine {

    /** Примерная высота одной строки текста при scale = 1 (в блоках). */
    public static final double LINE_HEIGHT = 0.25;
    public static final double DEFAULT_SPACING = 0.05;

    private List<String> text = new ArrayList<>();
    private String joined = "";
    private double sx = 1.0;
    private double sy = 1.0;
    private double sz = 1.0;
    private double spacing = DEFAULT_SPACING;
    private double offsetY = 0.0;

    /** Кэш JSON для строк без плейсхолдеров (одинаков для всех игроков). */
    private String cachedJson;

    public HoloLine() {
        setTextLines(List.of(""));
    }

    public static HoloLine of(String raw) {
        HoloLine line = new HoloLine();
        line.setText(raw);
        return line;
    }

    public void setText(String raw) {
        setTextLines(TextUtil.splitLines(raw));
    }

    public void setTextLines(List<String> lines) {
        this.text = new ArrayList<>(lines);
        if (this.text.isEmpty()) {
            this.text.add("");
        }
        this.joined = String.join("\n", this.text);
        this.cachedJson = null;
    }

    public List<String> getText() {
        return text;
    }

    public String joined() {
        return joined;
    }

    public boolean hasPlaceholders() {
        return Papi.isAvailable() && joined.indexOf('%') >= 0;
    }

    public String getCachedJson() {
        return cachedJson;
    }

    public void setCachedJson(String cachedJson) {
        this.cachedJson = cachedJson;
    }

    public void setScale(double uniform) {
        setScale(uniform, uniform, uniform);
    }

    public void setScale(double x, double y, double z) {
        this.sx = x;
        this.sy = y;
        this.sz = z;
    }

    public double getSx() {
        return sx;
    }

    public double getSy() {
        return sy;
    }

    public double getSz() {
        return sz;
    }

    public boolean isUniformScale() {
        return sx == sy && sy == sz;
    }

    public double getSpacing() {
        return spacing;
    }

    public void setSpacing(double spacing) {
        this.spacing = spacing;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    /** Примерная высота блока в мире с учётом масштаба. */
    public double height() {
        return LINE_HEIGHT * sy * text.size();
    }
}
