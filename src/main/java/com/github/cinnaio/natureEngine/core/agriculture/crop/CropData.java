package com.github.cinnaio.natureEngine.core.agriculture.crop;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;

import java.util.EnumSet;
import java.util.Set;

/**
 * 描述一种作物或植物的基础属性。
 * 作为 CropType 的默认实现，代表原版作物定义。
 */
public final class CropData implements CropType {

    private final String id;
    private final int stages;
    private final long baseTicksPerStage;
    private final double optimalTemperature;
    private final double temperatureTolerance;
    private final double optimalHumidity;
    private final double humidityTolerance;
    private final int minLight;
    private final Set<SeasonType> preferredSeasons;
    private final boolean enabled;

    public CropData(
            String id,
            int stages,
            long baseTicksPerStage,
            double optimalTemperature,
            double temperatureTolerance,
            double optimalHumidity,
            double humidityTolerance,
            int minLight,
            Set<SeasonType> preferredSeasons,
            boolean enabled
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
        this.enabled = enabled;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getStages() {
        return stages;
    }

    @Override
    public long getBaseTicksPerStage() {
        return baseTicksPerStage;
    }

    @Override
    public double getOptimalTemperature() {
        return optimalTemperature;
    }

    @Override
    public double getTemperatureTolerance() {
        return temperatureTolerance;
    }

    @Override
    public double getOptimalHumidity() {
        return optimalHumidity;
    }

    @Override
    public double getHumidityTolerance() {
        return humidityTolerance;
    }

    @Override
    public int getMinLight() {
        return minLight;
    }

    @Override
    public Set<SeasonType> getPreferredSeasons() {
        return preferredSeasons;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
