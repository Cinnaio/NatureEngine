package com.github.cinnaio.natureEngine.core.agriculture.growth;

import com.github.cinnaio.natureEngine.core.agriculture.crop.CropData;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import org.bukkit.Location;

/**
 * 单次生长计算所需的上下文。
 */
public final class GrowthContext {

    private final Location location;
    private final CropData cropData;
    private final int currentStage;
    private final SeasonType seasonType;
    private final double seasonProgress;
    private final WeatherType weatherType;
    private final EnvironmentContext environmentContext;

    public GrowthContext(
            Location location,
            CropData cropData,
            int currentStage,
            SeasonType seasonType,
            double seasonProgress,
            WeatherType weatherType,
            EnvironmentContext environmentContext
    ) {
        this.location = location;
        this.cropData = cropData;
        this.currentStage = currentStage;
        this.seasonType = seasonType;
        this.seasonProgress = seasonProgress;
        this.weatherType = weatherType;
        this.environmentContext = environmentContext;
    }

    public Location getLocation() {
        return location;
    }

    public CropData getCropData() {
        return cropData;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public SeasonType getSeasonType() {
        return seasonType;
    }

    public double getSeasonProgress() {
        return seasonProgress;
    }

    public WeatherType getWeatherType() {
        return weatherType;
    }

    public EnvironmentContext getEnvironmentContext() {
        return environmentContext;
    }
}

