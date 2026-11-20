package ru.kotmiks.holo.listener;

import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import ru.kotmiks.holo.hologram.HologramManager;

public final class ChunkListener implements Listener {

    private final HologramManager manager;

    public ChunkListener(HologramManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        manager.onChunkUnload(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
        manager.onPlayerChunkUnload(event.getPlayer(), event.getChunk().getWorld().getName(),
                event.getChunk().getX(), event.getChunk().getZ());
    }
}
