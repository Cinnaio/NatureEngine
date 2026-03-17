package com.github.cinnaio.natureEngine.bootstrap;

import com.github.cinnaio.natureEngine.command.NeRootCommand;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropManager;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropRegistry;
import com.github.cinnaio.natureEngine.core.agriculture.crop.listener.VanillaCropListener;
import com.github.cinnaio.natureEngine.core.agriculture.crop.vanilla.VanillaCropController;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthCalculator;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonNotifier;
import com.github.cinnaio.natureEngine.core.agriculture.season.visual.PacketSeasonVisualizer;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherController;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentManager;
import com.github.cinnaio.natureEngine.engine.config.ConfigManager;
import com.github.cinnaio.natureEngine.engine.scheduler.GlobalScheduler;
import com.github.cinnaio.natureEngine.engine.text.I18n;
import com.github.cinnaio.natureEngine.integration.craftengine.CraftEngineHook;
import com.github.cinnaio.natureEngine.integration.craftengine.CraftEngineCropController;
import com.github.cinnaio.natureEngine.integration.customnameplates.CustomNameplatesHook;
import com.github.cinnaio.natureEngine.integration.placeholderapi.NatureEngineExpansion;
import com.github.cinnaio.natureEngine.integration.protocollib.ProtocolLibHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.ConsoleCommandSender;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * 负责插件生命周期内各核心模块的启动与关闭。
 */
public final class LifecycleManager {

    private final JavaPlugin plugin;
    private final ServiceLocator serviceLocator;

    private ConfigManager configManager;
    private GlobalScheduler globalScheduler;
    private SeasonManager seasonManager;
    private WeatherManager weatherManager;
    private EnvironmentManager environmentManager;
    private CropManager cropManager;
    private VanillaCropController vanillaCropController;
    private CraftEngineHook craftEngineHook;
    private CraftEngineCropController craftEngineCropController;
    private CustomNameplatesHook customNameplatesHook;
    private PacketSeasonVisualizer packetSeasonVisualizer;
    private ProtocolLibHook protocolLibHook;
    private NatureEngineExpansion placeholderExpansion;

    public LifecycleManager(JavaPlugin plugin, ServiceLocator serviceLocator) {
        this.plugin = plugin;
        this.serviceLocator = serviceLocator;
    }

    public void onEnable() {
        // 初始化配置
        this.configManager = new ConfigManager(plugin);
        configManager.load();
        serviceLocator.register(ConfigManager.class, configManager);

        // i18n
        I18n i18n = new I18n(plugin);
        i18n.load();
        serviceLocator.register(I18n.class, i18n);

        // 初始化全局调度器（后续季节/天气/生长模拟都基于此）
        this.globalScheduler = new GlobalScheduler(plugin);
        globalScheduler.init();
        serviceLocator.register(GlobalScheduler.class, globalScheduler);

        // 核心农业环境相关服务
        this.seasonManager = new SeasonManager(configManager.getSeasonConfig());
        serviceLocator.register(SeasonNotifier.class, new SeasonNotifier(seasonManager, configManager.getSeasonConfig(), i18n));
        // 强制依赖 ProtocolLib（发包视觉层）
        this.protocolLibHook = new ProtocolLibHook(plugin);
        WeatherController weatherController = new WeatherController(configManager.getWeatherConfig());
        this.weatherManager = new WeatherManager(plugin, globalScheduler, seasonManager, configManager.getWeatherConfig(), weatherController);
        this.weatherManager.start();

        this.environmentManager = new EnvironmentManager(seasonManager, configManager.getSeasonConfig(), weatherManager, configManager.getWeatherConfig());
        CropRegistry cropRegistry = new CropRegistry(configManager.getCropConfig());
        GrowthCalculator growthCalculator = new GrowthCalculator(configManager.getGrowthConfig(), configManager.getWeatherConfig());
        this.cropManager = new CropManager(cropRegistry, growthCalculator);
        this.craftEngineHook = new CraftEngineHook(plugin);
        this.customNameplatesHook = new CustomNameplatesHook(plugin);
        this.packetSeasonVisualizer = new PacketSeasonVisualizer(plugin, seasonManager, configManager.getVisualConfig(), i18n);

        serviceLocator.register(SeasonManager.class, seasonManager);
        serviceLocator.register(WeatherManager.class, weatherManager);
        serviceLocator.register(EnvironmentManager.class, environmentManager);
        serviceLocator.register(CropManager.class, cropManager);
        serviceLocator.register(CraftEngineHook.class, craftEngineHook);
        serviceLocator.register(CustomNameplatesHook.class, customNameplatesHook);
        serviceLocator.register(PacketSeasonVisualizer.class, packetSeasonVisualizer);
        serviceLocator.register(ProtocolLibHook.class, protocolLibHook);

        // biome tint（仿 AdvancedSeasons）：读取 plugins/NatureEngine/biomeConfiguration/*.yml 并拦截注册表同步包改 effects 颜色

        // 注册原版作物监听
        Bukkit.getPluginManager().registerEvents(new VanillaCropListener(), plugin);
        vanillaCropController = new VanillaCropController(
                plugin,
                configManager,
                globalScheduler,
                seasonManager,
                weatherManager,
                environmentManager,
                cropManager
        );
        vanillaCropController.start();
        serviceLocator.register(VanillaCropController.class, vanillaCropController);

        // CraftEngine 植物接管（软依赖）
        if (craftEngineHook.isPresent()) {
            craftEngineCropController = new CraftEngineCropController(
                    plugin,
                    configManager,
                    globalScheduler,
                    seasonManager,
                    weatherManager,
                    environmentManager,
                    cropManager
            );
            craftEngineCropController.start();
            serviceLocator.register(com.github.cinnaio.natureEngine.integration.craftengine.CraftEngineTrackService.class, craftEngineCropController);
        }

        // 注册 /ne 命令（包含 ne debug）
        registerCommands();

        // PlaceholderAPI 软依赖：存在则注册占位符扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new NatureEngineExpansion(plugin);
            placeholderExpansion.register();
        }

        // 刷新队列处理：每 tick 执行，每玩家每 tick 处理 chunks-per-tick-per-player 个
        globalScheduler.runTaskTimer(packetSeasonVisualizer::tick, 20L, 1L);
        // 自动跟随：按间隔检测玩家移动并入队刷新，使“走到哪视觉跟到哪”
        long followInterval = configManager.getVisualConfig().getAutoFollowIntervalTicks();
        globalScheduler.runTaskTimer(packetSeasonVisualizer::tickAutoFollow, followInterval, followInterval);

        logStartupSummary(i18n);
    }

    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (globalScheduler != null) {
            globalScheduler.shutdown();
        }
        if (configManager != null) {
            configManager.saveIfNeeded();
        }
        serviceLocator.clear();
    }

    private void registerCommands() {
        try {
            Object server = Bukkit.getServer();
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(server);

            NeRootCommand neCommand = new NeRootCommand();
            commandMap.register("natureengine", neCommand);
        } catch (Exception e) {
            plugin.getLogger().warning("无法注册 /ne 命令: " + e.getMessage());
        }
    }

    private void logStartupSummary(I18n i18n) {
        String ver = plugin.getDescription().getVersion();
        String serverName = Bukkit.getName();
        String bukkitVer = Bukkit.getBukkitVersion();
        String mcVer = Bukkit.getMinecraftVersion();
        boolean folia = isFoliaLike();

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        int rts = configManager != null ? configManager.getGrowthConfig().getRandomTickSpeed() : 3;
        boolean cropsEnabled = configManager != null && configManager.getCropConfig() != null && configManager.getCropConfig().isGlobalEnabled();

        // 直接发到控制台（无 [INFO]/[NatureEngine] 前缀），更像 TrChat 的风格
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(""));
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(
                "&7Loading &bNatureEngine &fv" + ver + " &8(&7MC: &f" + mcVer + "&8)"
        ));
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(""));
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(
                "&8&7Server &8: &f" + serverName + " &8(&7" + bukkitVer + "&8)"
        ));
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(
                "&8&7FoliaLike &8: &f" + folia
        ));
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(
                "&8&7Config &8: &frts=" + rts + " &8  &7crops.enabled&8=&f" + cropsEnabled
        ));
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse("&8&7Hooks"));

        logHook(console, "ProtocolLib", Optional.ofNullable(protocolLibHook).map(h -> h.getProtocolLib()).orElse(null));
        logHook(console, "CraftEngine", Bukkit.getPluginManager().getPlugin("CraftEngine"));
        logHook(console, "PlaceholderAPI", Bukkit.getPluginManager().getPlugin("PlaceholderAPI"));
        logHook(console, "CustomNameplates", Bukkit.getPluginManager().getPlugin("CustomNameplates"));

        boolean papiExpansion = placeholderExpansion != null;
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(
                "&8&7PAPI Expansion &8: " + (papiExpansion ? "&aregistered" : "&cnot registered")
        ));
    }

    private void logHook(ConsoleCommandSender console, String name, Plugin p) {
        if (p == null) {
            console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(
                    "&8  &7" + padRight(name, 14) + " &8: &cnot installed"
            ));
            return;
        }
        String v = (p.getDescription() != null) ? p.getDescription().getVersion() : "?";
        String color = p.isEnabled() ? "&a" : "&c";
        String status = "&fv" + v + " &8(enabled=" + color + p.isEnabled() + "&8)";
        console.sendMessage(com.github.cinnaio.natureEngine.engine.text.Text.parse(
                "&8  &7" + padRight(name, 14) + " &8: " + status
        ));
    }

    private static String padRight(String s, int len) {
        if (s == null) s = "";
        if (s.length() >= len) return s;
        return s + " ".repeat(len - s.length());
    }

    private static boolean isFoliaLike() {
        try {
            // Paper/Folia API: exists on Folia and most folia-like forks
            Bukkit.getRegionScheduler();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

