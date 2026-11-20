package ru.kotmiks.holo.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import ru.kotmiks.holo.hologram.HologramManager;

public final class PlayerListener implements Listener {

    private final HologramManager manager;

    public PlayerListener(HologramManager manager) {
        this.manager = manager;
    }

    /** Игрок вышел — просто забываем его (пакеты слать некуда). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.forget(event.getPlayer().getUniqueId());
    }

    /** Смена мира — клиент и так сбросил сущности, но чистим состояние и шлём destroy на всякий случай. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        manager.hideAllFor(event.getPlayer());
    }

    /** Респавн — клиент может пересоздать мир, поэтому голограммы показываем заново. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        manager.hideAllFor(event.getPlayer());
    }
}
