package ru.kotmiks.holo.packet;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.joml.Vector3f;
import ru.kotmiks.holo.hologram.HoloLine;
import ru.kotmiks.holo.hologram.Hologram;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сборка метаданных TextDisplay.
 * <p>
 * Индексы (Minecraft 1.20.2 – 1.21.x):
 * <pre>
 *  11 translation (Vector3f)   12 scale (Vector3f)   13/14 rotations
 *  15 billboard (byte)         16 brightness (int, -1 = нет)
 *  17 view range (float)       23 TEXT (Component)   24 line width (int)
 *  25 background (int ARGB)    26 text opacity       27 flags (byte): 0x01 shadow, 0x02 see-through, 0x08 left, 0x10 right
 * </pre>
 * Сериализаторы находятся лениво и по отдельности: если какой-то не найден, соответствующее поле
 * просто не отправляется (с предупреждением в лог), а плагин продолжает работать.
 */
public final class DisplayMeta {

    private static final int I_SCALE = 12;
    private static final int I_BILLBOARD = 15;
    private static final int I_BRIGHTNESS = 16;
    private static final int I_VIEW_RANGE = 17;
    private static final int I_TEXT = 23;
    private static final int I_LINE_WIDTH = 24;
    private static final int I_BACKGROUND = 25;
    private static final int I_FLAGS = 27;

    private static final int FLAG_SHADOW = 0x01;
    private static final int FLAG_SEE_THROUGH = 0x02;

    private static volatile int shift = 0;

    // Нет static-инициализаторов, которые могут уронить класс: всё заполняется в resolve().
    private static WrappedDataWatcher.Serializer byteSer;
    private static WrappedDataWatcher.Serializer intSer;
    private static WrappedDataWatcher.Serializer floatSer;
    private static WrappedDataWatcher.Serializer vec3Ser;
    private static WrappedDataWatcher.Serializer chatSer;
    private static boolean resolved;

    private DisplayMeta() {
    }

    public static void setIndexShift(int newShift) {
        shift = newShift;
    }

    /**
     * Находит все сериализаторы. Никогда не бросает исключений.
     *
     * @return true, если найден сериализатор текста (без него голограмма бессмысленна)
     */
    public static synchronized boolean init(Logger log) {
        resolved = false;
        resolve(log);
        return chatSer != null;
    }

    private static void resolve(Logger log) {
        if (resolved) {
            return;
        }
        resolved = true;

        byteSer = find(log, "Byte", () -> WrappedDataWatcher.Registry.get(Byte.class), null);
        intSer = find(log, "Integer", () -> WrappedDataWatcher.Registry.get(Integer.class), null);
        floatSer = find(log, "Float", () -> WrappedDataWatcher.Registry.get(Float.class), null);
        vec3Ser = find(log, "Vector3f", () -> WrappedDataWatcher.Registry.get(Vector3f.class), "VECTOR3");
        chatSer = find(log, "Component (текст)", () -> WrappedDataWatcher.Registry.getChatComponentSerializer(false), "COMPONENT");

        log.info("Сериализаторы метаданных: byte=" + ok(byteSer) + ", int=" + ok(intSer) + ", float=" + ok(floatSer)
                + ", vector3f=" + ok(vec3Ser) + ", text=" + ok(chatSer));
    }

    private static String ok(Object o) {
        return o != null ? "OK" : "НЕТ";
    }

    @FunctionalInterface
    private interface Lookup {
        WrappedDataWatcher.Serializer get() throws Throwable;
    }

    /** Основной путь через Registry, затем (если задано) запасной — через EntityDataSerializers.<nmsField>. */
    private static WrappedDataWatcher.Serializer find(Logger log, String name, Lookup primary, String nmsField) {
        try {
            WrappedDataWatcher.Serializer s = primary.get();
            if (s != null) {
                return s;
            }
            log.warning("ProtocolLib вернул null для сериализатора " + name);
        } catch (Throwable t) {
            log.log(Level.WARNING, "ProtocolLib не смог выдать сериализатор " + name + ", пробую запасной путь", t);
        }
        if (nmsField == null) {
            return null;
        }
        try {
            WrappedDataWatcher.Serializer s = viaNms(nmsField);
            if (s != null) {
                log.info("Сериализатор " + name + " получен через EntityDataSerializers." + nmsField);
                return s;
            }
        } catch (Throwable t) {
            log.log(Level.WARNING, "Запасной путь для сериализатора " + name + " (EntityDataSerializers."
                    + nmsField + ") тоже не сработал", t);
        }
        return null;
    }

    private static WrappedDataWatcher.Serializer viaNms(String field) throws Throwable {
        Class<?> holder;
        try {
            holder = Class.forName("net.minecraft.network.syncher.EntityDataSerializers");
        } catch (Throwable first) {
            holder = MinecraftReflection.getMinecraftClass("network.syncher.EntityDataSerializers");
        }
        Object handle = holder.getField(field).get(null);
        Method fromHandle = WrappedDataWatcher.Registry.class.getMethod("fromHandle", Object.class);
        return (WrappedDataWatcher.Serializer) fromHandle.invoke(null, handle);
    }

    /** Полный набор метаданных для спавна блока. Поля без найденного сериализатора пропускаются. */
    public static synchronized List<WrappedDataValue> full(Hologram h, HoloLine line, String json) {
        List<WrappedDataValue> out = new ArrayList<>(8);
        if (vec3Ser != null) {
            out.add(new WrappedDataValue(I_SCALE + shift, vec3Ser,
                    new Vector3f((float) line.getSx(), (float) line.getSy(), (float) line.getSz())));
        }
        add(out, I_BILLBOARD, byteSer, Byte.valueOf(h.getBillboard().id()));
        if (h.getBrightness() >= 0) {
            int b = Math.min(15, h.getBrightness());
            add(out, I_BRIGHTNESS, intSer, Integer.valueOf((b << 4) | (b << 20)));
        }
        float viewRange = (float) Math.max(0.05D, h.getViewDistance() / 64.0D);
        add(out, I_VIEW_RANGE, floatSer, Float.valueOf(viewRange));
        addText(out, json);
        add(out, I_LINE_WIDTH, intSer, Integer.valueOf(h.getLineWidth()));
        add(out, I_BACKGROUND, intSer, Integer.valueOf(h.getBackground()));
        add(out, I_FLAGS, byteSer, Byte.valueOf((byte) flags(h)));
        return out;
    }

    /** Только текст — для обновления плейсхолдеров. */
    public static synchronized List<WrappedDataValue> textOnly(String json) {
        List<WrappedDataValue> out = new ArrayList<>(1);
        addText(out, json);
        return out;
    }

    private static void add(List<WrappedDataValue> out, int index, WrappedDataWatcher.Serializer ser, Object value) {
        if (ser != null) {
            out.add(new WrappedDataValue(index + shift, ser, value));
        }
    }

    private static void addText(List<WrappedDataValue> out, String json) {
        if (chatSer != null) {
            out.add(new WrappedDataValue(I_TEXT + shift, chatSer, WrappedChatComponent.fromJson(json).getHandle()));
        }
    }

    private static int flags(Hologram h) {
        int f = h.getAlignment().flagBits();
        if (h.isShadow()) {
            f |= FLAG_SHADOW;
        }
        if (h.isSeeThrough()) {
            f |= FLAG_SEE_THROUGH;
        }
        return f;
    }
}
