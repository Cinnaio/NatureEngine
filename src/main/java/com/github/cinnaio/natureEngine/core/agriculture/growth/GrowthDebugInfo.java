package com.github.cinnaio.natureEngine.core.agriculture.growth;

/**
 * 生长调试信息：包含各因子及最终总值，便于定位为什么不长/会枯萎。
 */
public final class GrowthDebugInfo {

    private final double temperatureFactor;
    private final double humidityFactor;
    private final double lightFactor;
    private final double seasonFactor;
    private final double weatherFactor;
    private final double total;
    private final GrowthResult result;

    public GrowthDebugInfo(
            double temperatureFactor,
            double humidityFactor,
            double lightFactor,
            double seasonFactor,
            double weatherFactor,
            double total,
            GrowthResult result
    ) {
        this.temperatureFactor = temperatureFactor;
        this.humidityFactor = humidityFactor;
        this.lightFactor = lightFactor;
        this.seasonFactor = seasonFactor;
        this.weatherFactor = weatherFactor;
        this.total = total;
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

    public double getTotal() {
        return total;
    }

    public GrowthResult getResult() {
        return result;
    }
}

