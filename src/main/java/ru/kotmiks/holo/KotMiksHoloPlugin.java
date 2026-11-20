package ru.kotmiks.holo;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.kotmiks.holo.command.HoloCommand;
import ru.kotmiks.holo.config.HologramStorage;
import ru.kotmiks.holo.config.PluginConfig;
import ru.kotmiks.holo.hologram.HologramManager;
import ru.kotmiks.holo.listener.ChunkListener;
import ru.kotmiks.holo.listener.PlayerListener;
import ru.kotmiks.holo.packet.DisplayMeta;
import ru.kotmiks.holo.util.BuiltinPlaceholders;
import ru.kotmiks.holo.util.KotMiksExpansion;
import ru.kotmiks.holo.util.Papi;
import ru.kotmiks.holo.util.WorldGuardHook;

import java.io.File;
import java.util.logging.Level;

public final class KotMiksHoloPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private HologramManager manager;
    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().severe("ProtocolLib не найден или не включён — плагин отключается.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        boolean textOk;
        try {
            textOk = DisplayMeta.init(getLogger());
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Не удалось инициализировать метаданные Display-сущностей", t);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!textOk) {
            getLogger().severe("Сериализатор текста недоступен — голограммы не смогут показывать текст. "
                    + "Причины см. выше (Caused by). Обновите ProtocolLib до последней dev-сборки.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Papi.init(pluginConfig.placeholderApiEnabled());
        getLogger().info(Papi.isAvailable()
                ? "PlaceholderAPI найден — плейсхолдеры включены."
                : "PlaceholderAPI не используется.");

        WorldGuardHook.init(getLogger());
        registerPlaceholderExpansion();

        File holoFile = new File(getDataFolder(), "holograms.yml");
        if (!holoFile.exists()) {
            saveResource("holograms.yml", false);
        }
        HologramStorage storage = new HologramStorage(holoFile, getLogger());
        manager = new HologramManager(this, pluginConfig, storage);
        manager.load();

        PluginCommand command = getCommand("holo");
        if (command != null) {
            HoloCommand executor = new HoloCommand(this, manager, pluginConfig);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(manager), this);
        Bukkit.getPluginManager().registerEvents(new ChunkListener(manager), this);

        tickTask = Bukkit.getScheduler().runTaskTimer(this, manager::tick, 20L, 1L);
        getLogger().info("Загружено голограмм: " + manager.count());
    }

    /** Регистрирует %kotmiksholo_...%, если PlaceholderAPI установлен. Без него ничего не делает. */
    private void registerPlaceholderExpansion() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            if (KotMiksExpansion.registerFor(this)) {
                getLogger().info("Расширение PlaceholderAPI 'kotmiksholo' зарегистрировано "
                        + "(%kotmiksholo_wg_players_<region_id>%).");
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Не удалось зарегистрировать расширение PlaceholderAPI", t);
        }
    }

    @Override
    public void onDisable() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                KotMiksExpansion.unregisterIfAny();
            } catch (Throwable ignored) {
                // PlaceholderAPI недоступен
            }
        }
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (manager != null) {
            manager.shutdown();
        }
    }

    /** /holo reload: перечитать config.yml и holograms.yml, переотправить пакеты. */
    public void reloadAll() {
        pluginConfig.load();
        Papi.init(pluginConfig.placeholderApiEnabled());
        WorldGuardHook.clearCache();
        BuiltinPlaceholders.clearCache();
        manager.reload();
    }
}
