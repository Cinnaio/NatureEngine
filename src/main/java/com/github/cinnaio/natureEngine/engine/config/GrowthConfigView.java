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

    // 环境参与生长（全局默认）

    public boolean isEnvironmentAffectGrowthEnabled() {
        return config.getBoolean("growth.environment.enabled", true);
    }

    public double getEnvironmentMitigationStrength() {
        double v = config.getDouble("growth.environment.mitigation-strength", 0.60);
        return clamp01(v);
    }

    public double getEnvironmentStability(String typeKeyLower) {
        if (typeKeyLower == null || typeKeyLower.isBlank()) return 0.0;
        double v = config.getDouble("growth.environment.stability." + typeKeyLower, 0.0);
        return clamp01(v);
    }

    public double getEnvironmentAdvanceBoost(String typeKeyLower) {
        if (typeKeyLower == null || typeKeyLower.isBlank()) return 1.0;
        double v = config.getDouble("growth.environment.advance-boost." + typeKeyLower, 1.0);
        // 合理范围：0.0~2.0
        return Math.max(0.0, Math.min(2.0, v));
    }

    public double getEnvironmentExposureBoostRange() {
        double v = config.getDouble("growth.environment.exposure-boost-range", 0.05);
        return clamp01(v);
    }

    /**
     * 插件内部 randomTickSpeed（默认 3，与原版 gamerule 默认一致）。
     */
    public int getRandomTickSpeed() {
        return Math.max(0, config.getInt("growth.random-tick-speed", 3));
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}

