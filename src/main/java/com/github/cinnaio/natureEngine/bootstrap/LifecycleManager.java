package com.github.cinnaio.natureEngine.bootstrap;

import com.github.cinnaio.natureEngine.command.NeRootCommand;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropManager;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropRegistry;
import com.github.cinnaio.natureEngine.core.agriculture.crop.listener.VanillaCropListener;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthCalculator;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.visual.PacketSeasonVisualizer;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherController;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentManager;
import com.github.cinnaio.natureEngine.engine.config.ConfigManager;
import com.github.cinnaio.natureEngine.engine.scheduler.GlobalScheduler;
import com.github.cinnaio.natureEngine.integration.craftengine.CraftEngineHook;
import com.github.cinnaio.natureEngine.integration.protocollib.ProtocolLibHook;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

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
    private CraftEngineHook craftEngineHook;
    private PacketSeasonVisualizer packetSeasonVisualizer;
    private ProtocolLibHook protocolLibHook;

    public LifecycleManager(JavaPlugin plugin, ServiceLocator serviceLocator) {
        this.plugin = plugin;
        this.serviceLocator = serviceLocator;
    }

    public void onEnable() {
        // 初始化配置
        this.configManager = new ConfigManager(plugin);
        configManager.load();
        serviceLocator.register(ConfigManager.class, configManager);

        // 初始化全局调度器（后续季节/天气/生长模拟都基于此）
        this.globalScheduler = new GlobalScheduler(plugin);
        globalScheduler.init();
        serviceLocator.register(GlobalScheduler.class, globalScheduler);

        // 核心农业环境相关服务
        this.seasonManager = new SeasonManager(configManager.getSeasonConfig());
        // 强制依赖 ProtocolLib（发包视觉层）
        this.protocolLibHook = new ProtocolLibHook(plugin);
        WeatherController weatherController = new WeatherController(configManager.getWeatherConfig());
        this.weatherManager = new WeatherManager(plugin, globalScheduler, seasonManager, configManager.getWeatherConfig(), weatherController);
        this.weatherManager.start();

        this.environmentManager = new EnvironmentManager(weatherManager, configManager.getWeatherConfig());
        CropRegistry cropRegistry = new CropRegistry();
        GrowthCalculator growthCalculator = new GrowthCalculator(configManager.getGrowthConfig(), configManager.getWeatherConfig());
        this.cropManager = new CropManager(cropRegistry, growthCalculator);
        this.craftEngineHook = new CraftEngineHook(plugin);
        this.packetSeasonVisualizer = new PacketSeasonVisualizer(plugin, seasonManager, configManager.getVisualConfig());

        serviceLocator.register(SeasonManager.class, seasonManager);
        serviceLocator.register(WeatherManager.class, weatherManager);
        serviceLocator.register(EnvironmentManager.class, environmentManager);
        serviceLocator.register(CropManager.class, cropManager);
        serviceLocator.register(CraftEngineHook.class, craftEngineHook);
        serviceLocator.register(PacketSeasonVisualizer.class, packetSeasonVisualizer);
        serviceLocator.register(ProtocolLibHook.class, protocolLibHook);

        // 注册原版作物监听
        Bukkit.getPluginManager().registerEvents(new VanillaCropListener(), plugin);

        // 注册 /ne 命令（包含 ne debug）
        registerCommands();

        // TODO: 在后续阶段补充 SeasonManager、WeatherManager、EnvironmentManager 等核心服务注册

        // 发包视觉刷新 tick（20 chunk/秒：每 tick 约 1 chunk）
        globalScheduler.runTaskTimer(packetSeasonVisualizer::tick, 20L, 1L);
    }

    public void onDisable() {
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
}

