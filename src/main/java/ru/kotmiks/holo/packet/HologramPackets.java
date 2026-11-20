package ru.kotmiks.holo.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Отправка пакетов SPAWN_ENTITY / ENTITY_METADATA / ENTITY_TELEPORT / ENTITY_DESTROY через ProtocolLib.
 */
public final class HologramPackets {

    private final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    private final Logger logger;
    private boolean teleportWarned;

    public HologramPackets(Logger logger) {
        this.logger = logger;
    }

    public void spawn(Player player, int entityId, UUID uuid, double x, double y, double z, float yaw, float pitch) {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        packet.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
        packet.getDoubles().write(0, x);
        packet.getDoubles().write(1, y);
        packet.getDoubles().write(2, z);
        packet.getBytes().write(0, angle(pitch));
        packet.getBytes().write(1, angle(yaw));
        send(player, packet);
    }

    public void metadata(Player player, int entityId, List<WrappedDataValue> values) {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entityId);
        packet.getDataValueCollectionModifier().write(0, values);
        send(player, packet);
    }

    /**
     * Телепорт пакетной сущности.
     *
     * @return false, если формат пакета в этой версии не поддержан — вызывающий код должен
     * тогда пересоздать сущность (destroy + spawn).
     */
    public boolean teleport(Player player, int entityId, double x, double y, double z, float yaw, float pitch) {
        try {
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            packet.getIntegers().write(0, entityId);
            packet.getDoubles().write(0, x);
            packet.getDoubles().write(1, y);
            packet.getDoubles().write(2, z);
            packet.getBytes().write(0, angle(yaw));
            packet.getBytes().write(1, angle(pitch));
            packet.getBooleans().write(0, false);
            manager.sendServerPacket(player, packet);
            return true;
        } catch (Exception ex) {
            if (!teleportWarned) {
                teleportWarned = true;
                logger.info("ENTITY_TELEPORT в этой версии имеет другой формат, использую пересоздание сущностей.");
            }
            return false;
        }
    }

    public void destroy(Player player, int[] entityIds) {
        if (entityIds.length == 0) {
            return;
        }
        List<Integer> ids = new ArrayList<>(entityIds.length);
        for (int id : entityIds) {
            ids.add(id);
        }
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().write(0, ids);
        send(player, packet);
    }

    private void send(Player player, PacketContainer packet) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            manager.sendServerPacket(player, packet);
        } catch (Exception ex) {
            logger.warning("Не удалось отправить пакет " + packet.getType() + " игроку "
                    + player.getName() + ": " + ex.getMessage());
        }
    }

    private static byte angle(float degrees) {
        return (byte) Math.floor(degrees * 256.0f / 360.0f);
    }
}
