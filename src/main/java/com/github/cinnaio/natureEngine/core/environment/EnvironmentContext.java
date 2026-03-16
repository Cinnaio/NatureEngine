package com.github.cinnaio.natureEngine.core.environment;

/**
 * 聚合某个坐标点的环境信息，供生长计算使用。
 */
public final class EnvironmentContext {

    private final double temperature;
    private final double humidity;
    private final double soilMoisture;
    private final int lightLevel;

    public EnvironmentContext(double temperature, double humidity, double soilMoisture, int lightLevel) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.soilMoisture = soilMoisture;
        this.lightLevel = lightLevel;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getSoilMoisture() {
        return soilMoisture;
    }

    public int getLightLevel() {
        return lightLevel;
    }
}

