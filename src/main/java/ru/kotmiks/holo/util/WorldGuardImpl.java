package ru.kotmiks.holo.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Реализация, напрямую использующая классы WorldGuard.
 * Загружается ТОЛЬКО из {@link WorldGuardHook} внутри try/catch, поэтому при отсутствии
 * WorldGuard на сервере NoClassDefFoundError не возникает.
 */
final class WorldGuardImpl {

    /** Проверка, что API реально работает. */
    void probe() {
        container();
    }

    private RegionContainer container() {
        return WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    int count(World world, String regionId) {
        RegionContainer container = container();
        if (world != null) {
            return countInWorld(container, world, regionId);
        }
        int total = 0;
        for (World w : Bukkit.getWorlds()) {
            total += countInWorld(container, w, regionId);
        }
        return total;
    }

    private int countInWorld(RegionContainer container, World world, String regionId) {
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return 0;
        }
        ProtectedRegion region = manager.getRegion(regionId);
        if (region == null) {
            // имя региона сравниваем без учёта регистра
            for (ProtectedRegion r : manager.getRegions().values()) {
                if (r.getId().equalsIgnoreCase(regionId)) {
                    region = r;
                    break;
                }
            }
        }
        if (region == null) {
            return 0;
        }
        boolean global = region.getType() == RegionType.GLOBAL;
        int count = 0;
        for (Player p : world.getPlayers()) {
            if (global) {
                count++;
                continue;
            }
            Location l = p.getLocation();
            if (region.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                count++;
            }
        }
        return count;
    }
}
