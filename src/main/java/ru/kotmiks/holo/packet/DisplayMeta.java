package ru.kotmiks.holo.packet;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.joml.Vector3f;
import ru.kotmiks.holo.hologram.HoloLine;
import ru.kotmiks.holo.hologram.Hologram;

import java.util.ArrayList;
import java.util.List;

/**
 * Сборка метаданных TextDisplay.
 * <p>
 * Индексы (Minecraft 1.20.2 – 1.21.x):
 * <pre>
 *  0..7   — базовая Entity
 *  8      — interpolation start delta (int)
 *  9      — transformation interpolation duration (int)
 *  10     — position/rotation interpolation duration (int)
 *  11     — translation (Vector3f)
 *  12     — scale (Vector3f)
 *  13     — left rotation (Quaternionf)
 *  14     — right rotation (Quaternionf)
 *  15     — billboard (byte)
 *  16     — brightness override (int, -1 = нет)
 *  17     — view range (float)
 *  18     — shadow radius (float)
 *  19     — shadow strength (float)
 *  20     — width (float)
 *  21     — height (float)
 *  22     — glow color override (int)
 *  23     — TEXT (Component)
 *  24     — line width (int)
 *  25     — background color (int ARGB)
 *  26     — text opacity (byte)
 *  27     — style flags (byte): 0x01 shadow, 0x02 see-through, 0x04 default bg, 0x08 left, 0x10 right
 * </pre>
 * Если в будущей версии игры индексы сдвинутся — достаточно поправить
 * {@code advanced.metadata-index-shift} в config.yml (или константы ниже).
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

    private DisplayMeta() {
    }

    /** Ленивый холдер сериализаторов: инициализируется только когда ProtocolLib уже загружен. */
    private static final class Serializers {
        static final WrappedDataWatcher.Serializer BYTE = WrappedDataWatcher.Registry.get(Byte.class);
        static final WrappedDataWatcher.Serializer INT = WrappedDataWatcher.Registry.get(Integer.class);
        static final WrappedDataWatcher.Serializer FLOAT = WrappedDataWatcher.Registry.get(Float.class);
        static final WrappedDataWatcher.Serializer VEC3 = WrappedDataWatcher.Registry.get(Vector3f.class);
        static final WrappedDataWatcher.Serializer CHAT = WrappedDataWatcher.Registry.getChatComponentSerializer(false);

        static void touch() {
            // принудительная инициализация класса
        }
    }

    public static void setIndexShift(int newShift) {
        shift = newShift;
    }

    /** Самопроверка: убеждаемся, что все нужные сериализаторы доступны. */
    public static void init() {
        Serializers.touch();
        if (Serializers.BYTE == null || Serializers.INT == null || Serializers.FLOAT == null
                || Serializers.VEC3 == null || Serializers.CHAT == null) {
            throw new IllegalStateException("ProtocolLib не предоставил один из сериализаторов метаданных");
        }
    }

    /** Полный набор метаданных для спавна блока. */
    public static List<WrappedDataValue> full(Hologram h, HoloLine line, String json) {
        List<WrappedDataValue> out = new ArrayList<>(8);
        out.add(new WrappedDataValue(I_SCALE + shift, Serializers.VEC3,
                new Vector3f((float) line.getSx(), (float) line.getSy(), (float) line.getSz())));
        out.add(new WrappedDataValue(I_BILLBOARD + shift, Serializers.BYTE, Byte.valueOf(h.getBillboard().id())));
        if (h.getBrightness() >= 0) {
            int b = Math.min(15, h.getBrightness());
            out.add(new WrappedDataValue(I_BRIGHTNESS + shift, Serializers.INT, Integer.valueOf((b << 4) | (b << 20))));
        }
        float viewRange = (float) Math.max(0.05D, h.getViewDistance() / 64.0D);
        out.add(new WrappedDataValue(I_VIEW_RANGE + shift, Serializers.FLOAT, Float.valueOf(viewRange)));
        out.add(textValue(json));
        out.add(new WrappedDataValue(I_LINE_WIDTH + shift, Serializers.INT, Integer.valueOf(h.getLineWidth())));
        out.add(new WrappedDataValue(I_BACKGROUND + shift, Serializers.INT, Integer.valueOf(h.getBackground())));
        out.add(new WrappedDataValue(I_FLAGS + shift, Serializers.BYTE, Byte.valueOf((byte) flags(h))));
        return out;
    }

    /** Только текст — для обновления плейсхолдеров. */
    public static List<WrappedDataValue> textOnly(String json) {
        List<WrappedDataValue> out = new ArrayList<>(1);
        out.add(textValue(json));
        return out;
    }

    private static WrappedDataValue textValue(String json) {
        return new WrappedDataValue(I_TEXT + shift, Serializers.CHAT, WrappedChatComponent.fromJson(json).getHandle());
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
