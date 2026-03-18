package com.github.cinnaio.natureEngine.engine.config;

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
}

