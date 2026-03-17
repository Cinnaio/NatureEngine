package com.github.cinnaio.natureEngine.engine.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

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
    private WeatherConfigView weatherConfigView;
    private VisualConfigView visualConfigView;
    private CropConfigView cropConfigView;
    private ConfigLoader loader;

    // 模块配置文件（保持同一个对象引用，便于运行时 reload 后所有 ConfigView 自动读到新值）
    private FileConfiguration seasonsCfg;
    private FileConfiguration growthCfg;
    private FileConfiguration debugCfg;
    private FileConfiguration weatherCfg;
    private FileConfiguration visualCfg;
    private FileConfiguration cropsCfg;

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
        this.seasonConfigView = new SeasonConfigView(seasonsCfg);
        this.growthConfigView = new GrowthConfigView(growthCfg);
        this.debugConfigView = new DebugConfigView(debugCfg);
        this.weatherConfigView = new WeatherConfigView(weatherCfg);
        this.visualConfigView = new VisualConfigView(visualCfg);
        this.cropConfigView = new CropConfigView(cropsCfg);
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

    public CropConfigView getCropConfig() {
        return cropConfigView;
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * 按模块重载 YAML 文件（不会重建 ConfigView 对象，避免引用失效）。
     * 支持：seasons/growth/debug/weather/visual/crops/all
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
                if (cropConfigView != null) {
                    cropConfigView.reload();
                }
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
}

