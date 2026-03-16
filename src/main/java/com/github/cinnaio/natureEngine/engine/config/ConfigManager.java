package com.github.cinnaio.natureEngine.engine.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 负责加载与保存主配置文件，后续可扩展为分模块配置。
 */
public final class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private SeasonConfigView seasonConfigView;
    private GrowthConfigView growthConfigView;
    private DebugConfigView debugConfigView;
    private WeatherConfigView weatherConfigView;
    private VisualConfigView visualConfigView;
    private ConfigLoader loader;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        this.loader = new ConfigLoader(plugin);
        FileConfiguration growthCfg = loader.loadOrSaveDefault("growth.yml");
        FileConfiguration debugCfg = loader.loadOrSaveDefault("config.yml");
        FileConfiguration weatherCfg = loader.loadOrSaveDefault("weather.yml");
        FileConfiguration visualCfg = loader.loadOrSaveDefault("visual.yml");

        this.seasonConfigView = new SeasonConfigView(loader.loadOrSaveDefault("seasons.yml"));
        this.growthConfigView = new GrowthConfigView(growthCfg);
        this.debugConfigView = new DebugConfigView(debugCfg);
        this.weatherConfigView = new WeatherConfigView(weatherCfg);
        this.visualConfigView = new VisualConfigView(visualCfg);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public SeasonConfigView getSeasonConfig() {
        return seasonConfigView;
    }

    public GrowthConfigView getGrowthConfig() {
        return growthConfigView;
    }

    public DebugConfigView getDebugConfig() {
        return debugConfigView;
    }

    public WeatherConfigView getWeatherConfig() {
        return weatherConfigView;
    }

    public VisualConfigView getVisualConfig() {
        return visualConfigView;
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void saveIfNeeded() {
        // 当前主配置只读，如有运行时修改可在此控制保存策略
    }
}

