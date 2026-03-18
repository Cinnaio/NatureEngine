package com.github.cinnaio.natureEngine.core.agriculture.growth;

import com.github.cinnaio.natureEngine.core.environment.EnvironmentType;

/**
 * 生长调试信息：包含各因子及最终总值，便于定位为什么不长/会枯萎。
 */
public final class GrowthDebugInfo {

    private final double temperatureFactor;
    private final double humidityFactor;
    private final double lightFactor;
    private final double seasonFactor;
    private final double weatherFactor;
    private final double totalBeforeEnv;
    private final double total;
    private final double badEnvPenalty;
    private final double stabilityFactor;
    private final double penaltyMitigation;
    private final double envAdvanceBoost;
    private final EnvironmentType environmentType;
    private final GrowthResult result;

    public GrowthDebugInfo(
            double temperatureFactor,
            double humidityFactor,
            double lightFactor,
            double seasonFactor,
            double weatherFactor,
            double totalBeforeEnv,
            double total,
            double badEnvPenalty,
            double stabilityFactor,
            double penaltyMitigation,
            double envAdvanceBoost,
            EnvironmentType environmentType,
            GrowthResult result
    ) {
        this.temperatureFactor = temperatureFactor;
        this.humidityFactor = humidityFactor;
        this.lightFactor = lightFactor;
        this.seasonFactor = seasonFactor;
        this.weatherFactor = weatherFactor;
        this.totalBeforeEnv = totalBeforeEnv;
        this.total = total;
        this.badEnvPenalty = badEnvPenalty;
        this.stabilityFactor = stabilityFactor;
        this.penaltyMitigation = penaltyMitigation;
        this.envAdvanceBoost = envAdvanceBoost;
        this.environmentType = environmentType;
        this.result = result;
    }

    public double getTemperatureFactor() {
        return temperatureFactor;
    }

    public double getHumidityFactor() {
        return humidityFactor;
    }

    public double getLightFactor() {
        return lightFactor;
    }

    public double getSeasonFactor() {
        return seasonFactor;
    }

    public double getWeatherFactor() {
        return weatherFactor;
    }

    public double getTotalBeforeEnv() {
        return totalBeforeEnv;
    }

    public double getTotal() {
        return total;
    }

    public double getBadEnvPenalty() {
        return badEnvPenalty;
    }

    public double getStabilityFactor() {
        return stabilityFactor;
    }

    public double getPenaltyMitigation() {
        return penaltyMitigation;
    }

    public double getEnvAdvanceBoost() {
        return envAdvanceBoost;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public GrowthResult getResult() {
        return result;
    }
}

