package com.github.cinnaio.natureEngine.core.agriculture.crop;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;

import java.util.EnumSet;
import java.util.Set;

/**
 * 描述一种作物或植物的基础属性。
 */
public final class CropData {

    private final String id;
    private final int stages;
    private final long baseTicksPerStage;
    private final double optimalTemperature;
    private final double temperatureTolerance;
    private final double optimalHumidity;
    private final double humidityTolerance;
    private final int minLight;
    private final Set<SeasonType> preferredSeasons;

    public CropData(
            String id,
            int stages,
            long baseTicksPerStage,
            double optimalTemperature,
            double temperatureTolerance,
            double optimalHumidity,
            double humidityTolerance,
            int minLight,
            Set<SeasonType> preferredSeasons
    ) {
        this.id = id;
        this.stages = stages;
        this.baseTicksPerStage = baseTicksPerStage;
        this.optimalTemperature = optimalTemperature;
        this.temperatureTolerance = temperatureTolerance;
        this.optimalHumidity = optimalHumidity;
        this.humidityTolerance = humidityTolerance;
        this.minLight = minLight;
        this.preferredSeasons = EnumSet.copyOf(preferredSeasons);
    }

    public String getId() {
        return id;
    }

    public int getStages() {
        return stages;
    }

    public long getBaseTicksPerStage() {
        return baseTicksPerStage;
    }

    public double getOptimalTemperature() {
        return optimalTemperature;
    }

    public double getTemperatureTolerance() {
        return temperatureTolerance;
    }

    public double getOptimalHumidity() {
        return optimalHumidity;
    }

    public double getHumidityTolerance() {
        return humidityTolerance;
    }

    public int getMinLight() {
        return minLight;
    }

    public Set<SeasonType> getPreferredSeasons() {
        return preferredSeasons;
    }
}

