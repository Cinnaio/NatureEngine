package com.github.cinnaio.natureEngine.core.agriculture.weather;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.engine.config.WeatherConfigView;

import java.util.Map;
import java.util.Random;

public final class WeatherController {

    private final WeatherConfigView config;
    private final Random random;

    public WeatherController(WeatherConfigView config) {
        this.config = config;
        this.random = new Random();
    }

    public WeatherType chooseNextWeather(SeasonType seasonType) {
        Map<WeatherType, Integer> weights = config.getWeightsForSeason(seasonType);
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

