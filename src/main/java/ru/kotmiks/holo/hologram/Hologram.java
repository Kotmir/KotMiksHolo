package ru.kotmiks.holo.hologram;

import org.bukkit.Chunk;
import org.bukkit.Location;
import ru.kotmiks.holo.util.EntityIds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Данные голограммы + её рантайм-состояние (id пакетных сущностей и список зрителей).
 * Все обращения происходят из основного потока сервера.
 */
public final class Hologram {

    private final String id;

    private boolean enabled = true;
    private String worldName = "world";
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    private double viewDistance;
    private int updateInterval;
    private Billboard billboard;
    private boolean shadow;
    private boolean seeThrough;
    private int background;
    private Alignment alignment;
    private int lineWidth;
    private int brightness;

    private final List<HoloLine> lines = new ArrayList<>();

    // ---- runtime ----
    private int[] entityIds = new int[0];
    private UUID[] entityUuids = new UUID[0];
    /** Зрители: UUID игрока -> последний отправленный JSON текста каждой строки. */
    private final Map<UUID, String[]> viewers = new HashMap<>();

    public Hologram(String id, HologramDefaults d) {
        this.id = id;
        this.viewDistance = d.viewDistance();
        this.updateInterval = d.updateInterval();
        this.billboard = d.billboard();
        this.shadow = d.shadow();
        this.seeThrough = d.seeThrough();
        this.background = d.background();
        this.alignment = d.alignment();
        this.lineWidth = d.lineWidth();
        this.brightness = d.brightness();
    }

    // ---------------- runtime ----------------

    public void rebuildEntities() {
        int n = lines.size();
        entityIds = new int[n];
        entityUuids = new UUID[n];
        for (int i = 0; i < n; i++) {
            entityIds[i] = EntityIds.next();
            entityUuids[i] = UUID.randomUUID();
        }
    }

    public void ensureEntities() {
        if (entityIds.length != lines.size()) {
            rebuildEntities();
        }
    }

    public int[] entityIds() {
        return entityIds;
    }

    public int entityId(int index) {
        return entityIds[index];
    }

    public UUID entityUuid(int index) {
        return entityUuids[index];
    }

    public Map<UUID, String[]> viewers() {
        return viewers;
    }

    public boolean hasViewers() {
        return !viewers.isEmpty();
    }

    /**
     * Смещения по Y (относительно базовой точки) для каждого блока.
     * Базовая точка — низ самого нижнего блока; блоки растут вверх.
     * spacing блока i = зазор между блоком i и блоком i+1 (следующим вниз).
     */
    public double[] yOffsets() {
        int n = lines.size();
        double[] ys = new double[n];
        double cursor = 0.0;
        for (int i = n - 1; i >= 0; i--) {
            HoloLine line = lines.get(i);
            ys[i] = cursor + line.getOffsetY();
            if (i > 0) {
                cursor += line.height() + lines.get(i - 1).getSpacing();
            }
        }
        return ys;
    }

    public int chunkX() {
        return ((int) Math.floor(x)) >> 4;
    }

    public int chunkZ() {
        return ((int) Math.floor(z)) >> 4;
    }

    public long chunkKey() {
        return Chunk.getChunkKey(chunkX(), chunkZ());
    }

    public void setLocation(Location loc) {
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    // ---------------- getters / setters ----------------

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public double getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(double viewDistance) {
        this.viewDistance = viewDistance;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public Billboard getBillboard() {
        return billboard;
    }

    public void setBillboard(Billboard billboard) {
        this.billboard = billboard;
    }

    public boolean isShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    public boolean isSeeThrough() {
        return seeThrough;
    }

    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
    }

    public int getBackground() {
        return background;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public List<HoloLine> getLines() {
        return lines;
    }
}
