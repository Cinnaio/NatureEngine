package com.github.cinnaio.natureEngine.core.agriculture.season;

/**
 * 描述单个季节的配置参数。
 */
public final class SeasonSettings {

    private final long lengthInDays;
    private final double baseTemperature;
    private final double baseHumidity;
    private final double growthMultiplier;
    private final double yieldMultiplier;
    private final boolean easyToWither;

    public SeasonSettings(
            long lengthInDays,
            double baseTemperature,
            double baseHumidity,
            double growthMultiplier,
            double yieldMultiplier,
            boolean easyToWither
    ) {
        this.lengthInDays = lengthInDays;
        this.baseTemperature = baseTemperature;
        this.baseHumidity = baseHumidity;
        this.growthMultiplier = growthMultiplier;
        this.yieldMultiplier = yieldMultiplier;
        this.easyToWither = easyToWither;
    }

    public long getLengthInDays() {
        return lengthInDays;
    }

    public double getBaseTemperature() {
        return baseTemperature;
    }

    public double getBaseHumidity() {
        return baseHumidity;
    }

    public double getGrowthMultiplier() {
        return growthMultiplier;
    }

    public double getYieldMultiplier() {
        return yieldMultiplier;
    }

    public boolean isEasyToWither() {
        return easyToWither;
    }
}

