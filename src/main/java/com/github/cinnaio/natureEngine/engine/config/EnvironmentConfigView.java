package com.github.cinnaio.natureEngine.engine.config;

import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * environment.yml 的简单视图。
 */
public final class EnvironmentConfigView {

    private final FileConfiguration config;

    public EnvironmentConfigView(FileConfiguration config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.getBoolean("environment.enabled", true);
    }

    public double getOutdoorThreshold() {
        return Math.max(0.0, Math.min(1.0, config.getDouble("environment.indoor.outdoor-threshold", 0.6)));
    }

    public boolean isNearWaterEnabled() {
        return config.getBoolean("environment.near-water.enabled", false);
    }

    /**
     * 按天气类型获取对室外得分的修正系数。
     * 若配置缺失则返回一套合理默认值。
     */
    public double getOutdoorWeatherFactor(WeatherType type) {
        if (type == null) return 1.0;
        String path = "environment.indoor.weather-factor." + type.name().toLowerCase();
        double def;
        switch (type) {
            case SUNNY -> def = 1.0;
            case CLOUDY -> def = 0.95;
            case RAIN -> def = 0.9;
            case SNOW -> def = 0.85;
            case STORM -> def = 0.7;
            default -> def = 1.0;
        }
        double v = config.getDouble(path, def);
        return Math.max(0.0, Math.min(2.0, v));
    }

    // 温室相关配置

    public boolean isGreenhouseEnabled() {
        return config.getBoolean("environment.greenhouse.enabled", true);
    }

    public int getGreenhouseRadius() {
        return Math.max(1, config.getInt("environment.greenhouse.radius", 4));
    }

    public int getGreenhouseMaxRoofHeight() {
        return Math.max(1, config.getInt("environment.greenhouse.max-roof-height", 6));
    }

    public double getGreenhouseThreshold() {
        return Math.max(0.0, Math.min(1.0, config.getDouble("environment.greenhouse.threshold", 0.6)));
    }

    public double getGreenhouseGamma() {
        double g = config.getDouble("environment.greenhouse.gamma", 1.0);
        if (g <= 0.0) return 1.0;
        return g;
    }
}

