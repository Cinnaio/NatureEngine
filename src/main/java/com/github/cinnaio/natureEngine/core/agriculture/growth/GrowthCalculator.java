package com.github.cinnaio.natureEngine.core.agriculture.growth;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentType;
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
        return calculateDebug(context).getResult();
    }

    public GrowthDebugInfo calculateDebug(GrowthContext context) {
        EnvironmentContext env = context.getEnvironmentContext();

        double temperatureFactor = computeTemperatureFactor(
                env.getTemperature(),
                context.getCropType().getOptimalTemperature(),
                context.getCropType().getTemperatureTolerance()
        );
        double humidityFactor = computeHumidityFactor(
                env.getHumidity(),
                context.getCropType().getOptimalHumidity(),
                context.getCropType().getHumidityTolerance()
        );
        double lightFactor = env.getLightLevel() >= context.getCropType().getMinLight() ? 1.0 : 0.3;
        double seasonFactor = computeSeasonFactor(
                context.getSeasonType(),
                context.getCropType().getPreferredSeasons()
        );
        double weatherFactor = computeWeatherFactor(context.getWeatherType());

        double totalBeforeEnv = temperatureFactor * humidityFactor * lightFactor * seasonFactor * weatherFactor;

        // 环境参与生长：推进速度微调 + 坏环境惩罚削弱
        double totalAfterEnv = totalBeforeEnv;
        double badEnvPenalty = 0.0;
        double stabilityFactor = 0.0;
        double penaltyMitigation = 1.0;
        double envAdvanceBoost = 1.0;
        EnvironmentType envType = env.getEnvironmentType();

        if (configView != null && configView.isEnvironmentAffectGrowthEnabled()) {
            // 1) badEnvPenalty：温湿因子越低，惩罚越大
            double avgTH = clamp01((temperatureFactor + humidityFactor) / 2.0);
            badEnvPenalty = 1.0 - avgTH;

            // 2) stabilityFactor：可被 crops.yml 覆写，否则走全局默认
            var policy = context.getCropType().getEnvironmentPolicy();
            boolean policyEnabled = policy == null || policy.isEnabled();
            if (policyEnabled && envType != null) {
                String typeKey = envType.name().toLowerCase(java.util.Locale.ROOT);
                Double stOverride = policy != null ? policy.getStabilityOverride(envType) : null;
                stabilityFactor = stOverride != null ? clamp01(stOverride) : configView.getEnvironmentStability(typeKey);

                double mitigationStrength = policy != null && policy.getMitigationStrengthOverride() != null
                        ? clamp01(policy.getMitigationStrengthOverride())
                        : configView.getEnvironmentMitigationStrength();

                penaltyMitigation = 1.0 - badEnvPenalty * stabilityFactor * mitigationStrength;
                penaltyMitigation = Math.max(0.0, Math.min(1.0, penaltyMitigation));
                totalAfterEnv = totalAfterEnv * penaltyMitigation;

                // 3) envAdvanceBoost：基础倍率 + exposureScore 插值
                Double abOverride = policy != null ? policy.getAdvanceBoostOverride(envType) : null;
                double baseBoost = abOverride != null ? Math.max(0.0, Math.min(2.0, abOverride)) : configView.getEnvironmentAdvanceBoost(typeKey);
                double range = policy != null && policy.getExposureBoostRangeOverride() != null
                        ? clamp01(policy.getExposureBoostRangeOverride())
                        : configView.getEnvironmentExposureBoostRange();
                // exposureScore in [0,1] -> extra in [-range, +range]
                double extra = (clamp01(env.getExposureScore()) - 0.5) * 2.0 * range;
                envAdvanceBoost = Math.max(0.0, Math.min(2.0, baseBoost + extra));
                totalAfterEnv = totalAfterEnv * envAdvanceBoost;
            }
        }

        if (totalAfterEnv <= configView.getWitherThreshold()) {
            return new GrowthDebugInfo(
                    temperatureFactor, humidityFactor, lightFactor, seasonFactor, weatherFactor,
                    totalBeforeEnv, totalAfterEnv, badEnvPenalty, stabilityFactor, penaltyMitigation, envAdvanceBoost, envType,
                    GrowthResult.wither()
            );
        }
        if (totalAfterEnv >= configView.getAdvanceThreshold()) {
            return new GrowthDebugInfo(
                    temperatureFactor, humidityFactor, lightFactor, seasonFactor, weatherFactor,
                    totalBeforeEnv, totalAfterEnv, badEnvPenalty, stabilityFactor, penaltyMitigation, envAdvanceBoost, envType,
                    GrowthResult.advanceOneStage(totalAfterEnv)
            );
        }
        return new GrowthDebugInfo(
                temperatureFactor, humidityFactor, lightFactor, seasonFactor, weatherFactor,
                totalBeforeEnv, totalAfterEnv, badEnvPenalty, stabilityFactor, penaltyMitigation, envAdvanceBoost, envType,
                GrowthResult.noChange()
        );
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
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

