package ru.kotmiks.holo.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kotmiks.holo.config.HologramStorage;
import ru.kotmiks.holo.config.PluginConfig;
import ru.kotmiks.holo.packet.DisplayMeta;
import ru.kotmiks.holo.packet.HologramPackets;
import ru.kotmiks.holo.util.Papi;
import ru.kotmiks.holo.util.TextUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Управляет всеми голограммами: загрузка/сохранение, видимость, обновление плейсхолдеров,
 * корректное уничтожение пакетных сущностей. Работает только в главном потоке.
 */
public final class HologramManager {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final HologramStorage storage;
    private final HologramPackets packets;

    private final Map<String, Hologram> holograms = new LinkedHashMap<>();
    private final Set<String> failed = new HashSet<>();
    private final Location scratch = new Location(null, 0, 0, 0);

    private long tickCounter;
    private boolean dirty;
    private boolean savePending;

    public HologramManager(JavaPlugin plugin, PluginConfig config, HologramStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.packets = new HologramPackets(plugin.getLogger());
    }

    // ================================================================ доступ

    private static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    public Hologram get(String id) {
        return holograms.get(key(id));
    }

    public boolean exists(String id) {
        return holograms.containsKey(key(id));
    }

    public Collection<Hologram> all() {
        return holograms.values();
    }

    public int count() {
        return holograms.size();
    }

    // ================================================================ загрузка

    public void load() {
        holograms.clear();
        failed.clear();
        for (Hologram h : storage.load(config.defaults()).values()) {
            holograms.put(key(h.getId()), h);
        }
    }

    /** Полная перезагрузка: уничтожить пакетные сущности, перечитать файл, сразу показать заново. */
    public void reload() {
        hideEverything();
        saveNow();
        load();
        refreshAllVisibility();
    }

    public void shutdown() {
        hideEverything();
        saveNow();
    }

    // ================================================================ тик

    public void tick() {
        tickCounter++;
        boolean checkVisibility = tickCounter % config.visibilityInterval() == 0;
        Map<World, List<Player>> playersByWorld = checkVisibility ? new HashMap<>() : null;

        for (Hologram h : holograms.values()) {
            try {
                World world = h.isEnabled() ? Bukkit.getWorld(h.getWorldName()) : null;
                if (world == null) {
                    if (h.hasViewers()) {
                        hideAll(h);
                    }
                    continue;
                }
                if (checkVisibility) {
                    List<Player> players = playersByWorld.computeIfAbsent(world, World::getPlayers);
                    updateVisibility(h, world, players);
                }
                int interval = h.getUpdateInterval();
                if (interval > 0 && h.hasViewers() && tickCounter % interval == 0) {
                    updateText(h);
                }
            } catch (Throwable t) {
                if (failed.add(key(h.getId()))) {
                    plugin.getLogger().warning("Ошибка в голограмме '" + h.getId() + "': " + t);
                    t.printStackTrace();
                }
            }
        }
    }

    private void refreshAllVisibility() {
        for (Hologram h : holograms.values()) {
            refreshVisibility(h);
        }
    }

    private void refreshVisibility(Hologram h) {
        World world = h.isEnabled() ? Bukkit.getWorld(h.getWorldName()) : null;
        if (world == null) {
            return;
        }
        try {
            updateVisibility(h, world, world.getPlayers());
        } catch (Throwable t) {
            plugin.getLogger().warning("Ошибка показа голограммы '" + h.getId() + "': " + t);
        }
    }

    private void updateVisibility(Hologram h, World world, List<Player> players) {
        for (Player p : players) {
            boolean has = h.viewers().containsKey(p.getUniqueId());
            boolean should = canSee(h, p, has);
            if (should && !has) {
                show(h, p);
            } else if (!should && has) {
                hide(h, p);
            }
        }
        if (h.hasViewers()) {
            // зрители, которые вышли / сменили мир (на случай, если событие не пришло)
            for (UUID id : new ArrayList<>(h.viewers().keySet())) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.isOnline()) {
                    h.viewers().remove(id);
                } else if (!p.getWorld().equals(world)) {
                    hide(h, p);
                }
            }
        }
    }

    private boolean canSee(Hologram h, Player p, boolean alreadyViewing) {
        p.getLocation(scratch);
        double dx = scratch.getX() - h.getX();
        double dy = scratch.getY() - h.getY();
        double dz = scratch.getZ() - h.getZ();
        double max = h.getViewDistance() + (alreadyViewing ? 2.0 : 0.0);
        if (dx * dx + dy * dy + dz * dz > max * max) {
            return false;
        }
        // клиент должен реально иметь чанк, иначе сущность исчезнет/не отобразится
        return p.isChunkSent(h.chunkKey());
    }

    private void updateText(Hologram h) {
        List<HoloLine> lines = h.getLines();
        boolean dynamic = false;
        for (HoloLine line : lines) {
            if (line.hasPlaceholders()) {
                dynamic = true;
                break;
            }
        }
        if (!dynamic) {
            return;
        }
        for (Map.Entry<UUID, String[]> entry : new ArrayList<>(h.viewers().entrySet())) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) {
                h.viewers().remove(entry.getKey());
                continue;
            }
            String[] last = entry.getValue();
            for (int i = 0; i < lines.size() && i < last.length; i++) {
                HoloLine line = lines.get(i);
                if (!line.hasPlaceholders()) {
                    continue;
                }
                String json = renderJson(line, p);
                if (!json.equals(last[i])) {
                    last[i] = json;
                    packets.metadata(p, h.entityId(i), DisplayMeta.textOnly(json));
                }
            }
        }
    }

    // ================================================================ показ / скрытие

    private String renderJson(HoloLine line, Player player) {
        if (!line.hasPlaceholders()) {
            String cached = line.getCachedJson();
            if (cached == null) {
                cached = TextUtil.toJson(line.joined());
                line.setCachedJson(cached);
            }
            return cached;
        }
        return TextUtil.toJson(Papi.apply(player, line.joined()));
    }

    private void show(Hologram h, Player p) {
        List<HoloLine> lines = h.getLines();
        int n = lines.size();
        if (n == 0) {
            return;
        }
        h.ensureEntities();
        double[] ys = h.yOffsets();
        String[] sent = new String[n];
        for (int i = 0; i < n; i++) {
            HoloLine line = lines.get(i);
            String json = renderJson(line, p);
            sent[i] = json;
            packets.spawn(p, h.entityId(i), h.entityUuid(i),
                    h.getX(), h.getY() + ys[i], h.getZ(), h.getYaw(), h.getPitch());
            packets.metadata(p, h.entityId(i), DisplayMeta.full(h, line, json));
        }
        h.viewers().put(p.getUniqueId(), sent);
    }

    public void hide(Hologram h, Player p) {
        if (h.viewers().remove(p.getUniqueId()) != null && p.isOnline()) {
            packets.destroy(p, h.entityIds());
        }
    }

    public void hideAll(Hologram h) {
        if (!h.hasViewers()) {
            return;
        }
        for (UUID id : new ArrayList<>(h.viewers().keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                packets.destroy(p, h.entityIds());
            }
        }
        h.viewers().clear();
    }

    /** Убрать у игрока все голограммы (смена мира, респавн). */
    public void hideAllFor(Player p) {
        for (Hologram h : holograms.values()) {
            hide(h, p);
        }
    }

    /** Забыть игрока без отправки пакетов (выход с сервера). */
    public void forget(UUID uuid) {
        for (Hologram h : holograms.values()) {
            h.viewers().remove(uuid);
        }
    }

    private void hideEverything() {
        for (Hologram h : holograms.values()) {
            hideAll(h);
        }
    }

    // ================================================================ чанки

    /** Сервер выгружает чанк — убираем голограммы из него у всех зрителей. */
    public void onChunkUnload(String worldName, int chunkX, int chunkZ) {
        for (Hologram h : holograms.values()) {
            if (h.hasViewers() && h.getWorldName().equals(worldName)
                    && h.chunkX() == chunkX && h.chunkZ() == chunkZ) {
                hideAll(h);
            }
        }
    }

    /** Клиенту игрока отправлена выгрузка чанка — убираем голограммы из него у этого игрока. */
    public void onPlayerChunkUnload(Player p, String worldName, int chunkX, int chunkZ) {
        for (Hologram h : holograms.values()) {
            if (h.getWorldName().equals(worldName) && h.chunkX() == chunkX && h.chunkZ() == chunkZ) {
                hide(h, p);
            }
        }
    }

    // ================================================================ редактирование

    public Hologram create(String id, Location loc, String text) {
        Hologram h = new Hologram(id, config.defaults());
        h.setLocation(loc);
        h.getLines().add(HoloLine.of(text));
        h.rebuildEntities();
        holograms.put(key(id), h);
        markDirty();
        refreshVisibility(h);
        return h;
    }

    public boolean delete(String id) {
        Hologram h = holograms.remove(key(id));
        if (h == null) {
            return false;
        }
        hideAll(h);
        markDirty();
        return true;
    }

    public void moveTo(Hologram h, Location loc) {
        boolean sameWorld = h.getWorldName().equals(loc.getWorld().getName());
        if (!sameWorld) {
            edit(h, () -> h.setLocation(loc));
            return;
        }
        h.setLocation(loc);
        double[] ys = h.yOffsets();
        for (UUID id : new ArrayList<>(h.viewers().keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                h.viewers().remove(id);
                continue;
            }
            boolean ok = true;
            for (int i = 0; i < ys.length && ok; i++) {
                ok = packets.teleport(p, h.entityId(i), h.getX(), h.getY() + ys[i], h.getZ(), h.getYaw(), h.getPitch());
            }
            if (!ok) {
                // формат ENTITY_TELEPORT не поддержан — пересоздаём сущности
                hide(h, p);
                if (canSee(h, p, false)) {
                    show(h, p);
                }
            }
        }
        markDirty();
    }

    public void addLine(Hologram h, String text) {
        edit(h, () -> h.getLines().add(HoloLine.of(text)));
    }

    public void setLine(Hologram h, int index, String text) {
        edit(h, () -> h.getLines().get(index).setText(text));
    }

    public void removeLine(Hologram h, int index) {
        edit(h, () -> h.getLines().remove(index));
    }

    /** @param index индекс строки (с 0) или -1 для всех строк */
    public void setScale(Hologram h, int index, double scale) {
        edit(h, () -> {
            if (index < 0) {
                for (HoloLine line : h.getLines()) {
                    line.setScale(scale);
                }
            } else {
                h.getLines().get(index).setScale(scale);
            }
        });
    }

    public void setBillboard(Hologram h, Billboard billboard) {
        edit(h, () -> h.setBillboard(billboard));
    }

    /**
     * Безопасное изменение: сначала уничтожаем сущности у всех зрителей (по старым id),
     * применяем изменение, выдаём новые id и показываем снова тем, кто всё ещё должен видеть.
     */
    private void edit(Hologram h, Runnable mutation) {
        List<UUID> before = new ArrayList<>(h.viewers().keySet());
        hideAll(h);
        mutation.run();
        h.rebuildEntities();
        World world = h.isEnabled() ? Bukkit.getWorld(h.getWorldName()) : null;
        if (world != null) {
            for (UUID id : before) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline() && p.getWorld().equals(world) && canSee(h, p, false)) {
                    show(h, p);
                }
            }
        }
        markDirty();
    }

    // ================================================================ сохранение

    private void markDirty() {
        dirty = true;
        if (savePending || !plugin.isEnabled()) {
            return;
        }
        savePending = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            savePending = false;
            saveNow();
        }, 40L);
    }

    public void saveNow() {
        if (!dirty) {
            return;
        }
        dirty = false;
        try {
            storage.write(storage.serialize(holograms.values()));
        } catch (IOException e) {
            dirty = true;
            plugin.getLogger().severe("Не удалось сохранить holograms.yml: " + e.getMessage());
        }
    }
}
