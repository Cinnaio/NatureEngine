package com.github.cinnaio.natureEngine.engine.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 生长算法通用配置视图。
 */
public final class GrowthConfigView {

    private final FileConfiguration config;

    public GrowthConfigView(FileConfiguration config) {
        this.config = config;
    }

    public double getAdvanceThreshold() {
        return config.getDouble("growth.advance-threshold", 0.8);
    }

    public double getWitherThreshold() {
        return config.getDouble("growth.wither-threshold", 0.1);
    }
}

