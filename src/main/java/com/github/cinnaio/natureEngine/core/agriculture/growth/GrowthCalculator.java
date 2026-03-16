package com.github.cinnaio.natureEngine.core.agriculture.growth;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.engine.config.GrowthConfigView;
import com.github.cinnaio.natureEngine.engine.config.WeatherConfigView;

/**
 * 将季节、天气与环境因素综合为一次生长决策。
 */
public final class GrowthCalculator {

    private final GrowthConfigView configView;
    private final WeatherConfigView weatherConfigView;

    public GrowthCalculator(GrowthConfigView configView, WeatherConfigView weatherConfigView) {
        this.configView = configView;
        this.weatherConfigView = weatherConfigView;
    }

    public GrowthResult calculate(GrowthContext context) {
        EnvironmentContext env = context.getEnvironmentContext();

        double temperatureFactor = computeTemperatureFactor(
                env.getTemperature(),
                context.getCropData().getOptimalTemperature(),
                context.getCropData().getTemperatureTolerance()
        );
        double humidityFactor = computeHumidityFactor(
                env.getHumidity(),
                context.getCropData().getOptimalHumidity(),
                context.getCropData().getHumidityTolerance()
        );
        double lightFactor = env.getLightLevel() >= context.getCropData().getMinLight() ? 1.0 : 0.3;
        double seasonFactor = computeSeasonFactor(
                context.getSeasonType(),
                context.getCropData().getPreferredSeasons()
        );
        double weatherFactor = computeWeatherFactor(context.getWeatherType());

        double total = temperatureFactor * humidityFactor * lightFactor * seasonFactor * weatherFactor;

        if (total <= configView.getWitherThreshold()) {
            return GrowthResult.wither();
        }
        if (total >= configView.getAdvanceThreshold()) {
            return GrowthResult.advanceOneStage(total);
        }
        return GrowthResult.noChange();
    }

    private double computeTemperatureFactor(double actual, double optimal, double tolerance) {
        double diff = Math.abs(actual - optimal);
        if (diff >= tolerance) {
            return 0.0;
        }
        return 1.0 - (diff / tolerance);
    }

    private double computeHumidityFactor(double actual, double optimal, double tolerance) {
        double diff = Math.abs(actual - optimal);
        if (diff >= tolerance) {
            return 0.2;
        }
        return 1.0 - (diff / tolerance) * 0.5;
    }

    private double computeSeasonFactor(SeasonType seasonType, java.util.Set<SeasonType> preferred) {
        return preferred.contains(seasonType) ? 1.2 : 0.8;
    }

    private double computeWeatherFactor(WeatherType weatherType) {
        if (weatherConfigView != null) {
            return weatherConfigView.getProfile(weatherType).getGrowthMultiplier();
        }
        switch (weatherType) {
            case RAIN:
                return 1.1;
            case STORM:
                return 0.9;
            case SNOW:
                return 0.6;
            case CLOUDY:
                return 0.95;
            case SUNNY:
            default:
                return 1.0;
        }
    }
}

