package com.github.cinnaio.natureEngine.engine.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.File;

/**
 * 负责加载与保存主配置文件，后续可扩展为分模块配置。
 */
public final class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private SeasonConfigView seasonConfigView;
    private GrowthConfigView growthConfigView;
    private DebugConfigView debugConfigView;
    private TimeConfigView timeConfigView;
    private WeatherConfigView weatherConfigView;
    private VisualConfigView visualConfigView;
    private CropConfigView cropConfigView;
    private BiomeTitleConfigView biomeTitleConfigView;
    private EnvironmentConfigView environmentConfigView;
    private ConfigLoader loader;

    // 模块配置文件（保持同一个对象引用，便于运行时 reload 后所有 ConfigView 自动读到新值）
    private FileConfiguration seasonsCfg;
    private FileConfiguration growthCfg;
    private FileConfiguration debugCfg;
    private FileConfiguration weatherCfg;
    private FileConfiguration visualCfg;
    private FileConfiguration cropsCfg;
    private FileConfiguration biomeTitlesCfg;
    private FileConfiguration environmentCfg;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        this.loader = new ConfigLoader(plugin);
        this.growthCfg = loader.loadOrSaveDefault("growth.yml");
        this.debugCfg = loader.loadOrSaveDefault("config.yml");
        this.weatherCfg = loader.loadOrSaveDefault("weather.yml");
        this.visualCfg = loader.loadOrSaveDefault("visual.yml");
        this.seasonsCfg = loader.loadOrSaveDefault("seasons.yml");
        this.cropsCfg = loader.loadOrSaveDefault("crops.yml");
        this.biomeTitlesCfg = loader.loadOrSaveDefault("biome-titles.yml");
        this.environmentCfg = loader.loadOrSaveDefault("environment.yml");
        this.seasonConfigView = new SeasonConfigView(seasonsCfg);
        this.growthConfigView = new GrowthConfigView(growthCfg);
        this.debugConfigView = new DebugConfigView(debugCfg);
        this.timeConfigView = new TimeConfigView(debugCfg);
        this.weatherConfigView = new WeatherConfigView(weatherCfg);
        this.visualConfigView = new VisualConfigView(visualCfg);
        this.cropConfigView = new CropConfigView(cropsCfg);
        this.biomeTitleConfigView = new BiomeTitleConfigView(biomeTitlesCfg);
        this.environmentConfigView = new EnvironmentConfigView(environmentCfg);
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

    public TimeConfigView getTimeConfig() {
        return timeConfigView;
    }

    public WeatherConfigView getWeatherConfig() {
        return weatherConfigView;
    }

    public VisualConfigView getVisualConfig() {
        return visualConfigView;
    }

    public CropConfigView getCropConfig() {
        return cropConfigView;
    }

    public BiomeTitleConfigView getBiomeTitleConfig() {
        return biomeTitleConfigView;
    }

    public EnvironmentConfigView getEnvironmentConfig() {
        return environmentConfigView;
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * 按模块重载 YAML 文件（不会重建 ConfigView 对象，避免引用失效）。
     * 支持：seasons/growth/debug/weather/visual/crops/biome/all
     */
    public void reloadModule(String module) {
        if (module == null) return;
        String m = module.trim().toLowerCase();
        switch (m) {
            case "all" -> {
                reloadYaml(seasonsCfg, "seasons.yml");
                reloadYaml(growthCfg, "growth.yml");
                reloadYaml(debugCfg, "config.yml");
                reloadYaml(weatherCfg, "weather.yml");
                reloadYaml(visualCfg, "visual.yml");
                reloadYaml(cropsCfg, "crops.yml");
                reloadYaml(biomeTitlesCfg, "biome-titles.yml");
                reloadYaml(environmentCfg, "environment.yml");
                if (cropConfigView != null) {
                    cropConfigView.reload();
                }
                if (biomeTitleConfigView != null) {
                    biomeTitleConfigView.reload();
                }
                // environment 视图无需拥有 reload 方法，直接读最新 FileConfiguration 即可
                // 主配置（config.yml 之外的 plugin.yml 主配置）也一并刷新
                reload();
            }
            case "season", "seasons" -> reloadYaml(seasonsCfg, "seasons.yml");
            case "growth" -> reloadYaml(growthCfg, "growth.yml");
            case "debug", "config" -> reloadYaml(debugCfg, "config.yml");
            case "weather" -> reloadYaml(weatherCfg, "weather.yml");
            case "visual" -> reloadYaml(visualCfg, "visual.yml");
            case "crop", "crops" -> {
                reloadYaml(cropsCfg, "crops.yml");
                if (cropConfigView != null) {
                    cropConfigView.reload();
                }
            }
            case "biome", "biomes" -> {
                reloadYaml(biomeTitlesCfg, "biome-titles.yml");
                if (biomeTitleConfigView != null) {
                    biomeTitleConfigView.reload();
                }
            }
            case "environment", "env" -> reloadYaml(environmentCfg, "environment.yml");
            default -> {
                // ignore unknown
            }
        }
    }

    private void reloadYaml(FileConfiguration cfg, String fileName) {
        if (cfg == null) return;
        if (!(cfg instanceof YamlConfiguration yml)) return;
        try {
            File target = new File(plugin.getDataFolder(), fileName);
            yml.load(target);
        } catch (Exception e) {
            plugin.getLogger().warning("[NatureEngine] reload failed: " + fileName + " -> " + e.getMessage());
        }
    }

    public void saveIfNeeded() {
        // 当前主配置只读，如有运行时修改可在此控制保存策略
    }

    /**
     * 运行时修改并保存 growth.yml 的插件 randomTickSpeed。
     */
    public boolean setPluginRandomTickSpeed(int value) {
        if (growthCfg == null) return false;
        int v = Math.max(0, value);
        growthCfg.set("growth.random-tick-speed", v);
        if (growthCfg instanceof YamlConfiguration yml) {
            try {
                File target = new File(plugin.getDataFolder(), "growth.yml");
                yml.save(target);
                return true;
            } catch (IOException e) {
                plugin.getLogger().warning("[NatureEngine] save growth.yml failed: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
}

