package com.github.cinnaio.natureEngine.core.agriculture.weather;

/**
 * 描述某种天气对环境与作物的影响。
 */
public final class WeatherProfile {

    private final double temperatureDelta;
    private final double humidityDelta;
    private final double soilMoistureDelta;
    private final double growthMultiplier;

    public WeatherProfile(
            double temperatureDelta,
            double humidityDelta,
            double soilMoistureDelta,
            double growthMultiplier
    ) {
        this.temperatureDelta = temperatureDelta;
        this.humidityDelta = humidityDelta;
        this.soilMoistureDelta = soilMoistureDelta;
        this.growthMultiplier = growthMultiplier;
    }

    public double getTemperatureDelta() {
        return temperatureDelta;
    }

    public double getHumidityDelta() {
        return humidityDelta;
    }

    public double getSoilMoistureDelta() {
        return soilMoistureDelta;
    }

    public double getGrowthMultiplier() {
        return growthMultiplier;
    }
}

