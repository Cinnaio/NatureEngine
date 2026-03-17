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

    /**
     * 插件内部 randomTickSpeed（默认 3，与原版 gamerule 默认一致）。
     */
    public int getRandomTickSpeed() {
        return Math.max(0, config.getInt("growth.random-tick-speed", 3));
    }
}

