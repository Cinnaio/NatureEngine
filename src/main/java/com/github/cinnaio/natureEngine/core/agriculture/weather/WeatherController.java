package com.github.cinnaio.natureEngine.core.agriculture.weather;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.season.SolarTerm;
import com.github.cinnaio.natureEngine.engine.config.WeatherConfigView;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public final class WeatherController {

    private final WeatherConfigView config;
    private final Random random;

    public WeatherController(WeatherConfigView config) {
        this.config = config;
        this.random = new Random();
    }

    public WeatherType chooseNextWeather(SeasonType seasonType, SolarTerm solarTerm) {
        Map<WeatherType, Integer> baseWeights = config.getWeightsForSeason(seasonType);
        Map<WeatherType, Double> multipliers = config.getWeightMultipliersForSolarTerm(solarTerm);

        Map<WeatherType, Integer> weights = new EnumMap<>(WeatherType.class);
        for (WeatherType t : WeatherType.values()) {
            int base = Math.max(0, baseWeights.getOrDefault(t, 0));
            double m = multipliers.getOrDefault(t, 1.0);
            int effective = (int) Math.round(base * Math.max(0.0, m));
            weights.put(t, Math.max(0, effective));
        }
        int total = 0;
        for (int w : weights.values()) {
            total += Math.max(0, w);
        }
        if (total <= 0) {
            return WeatherType.SUNNY;
        }
        int roll = random.nextInt(total);
        int cursor = 0;
        for (WeatherType type : WeatherType.values()) {
            cursor += Math.max(0, weights.getOrDefault(type, 0));
            if (roll < cursor) {
                return type;
            }
        }
        return WeatherType.SUNNY;
    }
}

