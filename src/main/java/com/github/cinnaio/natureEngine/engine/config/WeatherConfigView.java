package com.github.cinnaio.natureEngine.engine.config;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.season.SolarTerm;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherProfile;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public final class WeatherConfigView {

    private final FileConfiguration config;

    public WeatherConfigView(FileConfiguration config) {
        this.config = config;
    }

    public int getTickIntervalSeconds() {
        return config.getInt("weather.tick-interval-seconds", 90);
    }

    public int getDurationSeconds(WeatherType type) {
        return config.getInt("weather.duration-seconds." + type.name(), 240);
    }

    public Map<WeatherType, Integer> getWeightsForSeason(SeasonType season) {
        ConfigurationSection section = config.getConfigurationSection("weather.season-weights." + season.name().toLowerCase());
        Map<WeatherType, Integer> weights = new EnumMap<>(WeatherType.class);
        for (WeatherType t : WeatherType.values()) {
            int w = section != null ? section.getInt(t.name(), 0) : 0;
            weights.put(t, Math.max(0, w));
        }
        return weights;
    }

    /**
     * 节气权重乘子：用于叠加到季节权重上（默认 1.0）。
     * 读取路径：weather.solar-term-weight-multipliers.<termKey>.<WEATHER>
     */
    public Map<WeatherType, Double> getWeightMultipliersForSolarTerm(SolarTerm term) {
        Map<WeatherType, Double> mult = new EnumMap<>(WeatherType.class);
        if (term == null) {
            for (WeatherType t : WeatherType.values()) mult.put(t, 1.0);
            return mult;
        }
        String base = "weather.solar-term-weight-multipliers." + term.key();
        ConfigurationSection section = config.getConfigurationSection(base);
        for (WeatherType t : WeatherType.values()) {
            double v = section != null ? section.getDouble(t.name(), 1.0) : 1.0;
            if (Double.isNaN(v) || Double.isInfinite(v)) v = 1.0;
            if (v < 0.0) v = 0.0;
            mult.put(t, v);
        }
        return mult;
    }

    public WeatherProfile getProfile(WeatherType type) {
        String path = "weather.profiles." + type.name();
        double temperatureDelta = config.getDouble(path + ".temperature-delta", 0.0);
        double humidityDelta = config.getDouble(path + ".humidity-delta", 0.0);
        double soilDelta = config.getDouble(path + ".soil-moisture-delta", 0.0);
        double growthMultiplier = config.getDouble(path + ".growth-multiplier", 1.0);
        return new WeatherProfile(temperatureDelta, humidityDelta, soilDelta, growthMultiplier);
    }
}

