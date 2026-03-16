package com.github.cinnaio.natureEngine.api;

import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import org.bukkit.World;

public final class WeatherAPI {

    private static final ServiceLocator SERVICES = ServiceLocator.getInstance();

    private WeatherAPI() {
    }

    public static WeatherType getCurrentWeather(World world) {
        return SERVICES.get(WeatherManager.class).getCurrentWeather(world);
    }

    public static void setWeather(World world, WeatherType type, int durationTicks) {
        SERVICES.get(WeatherManager.class).setWeather(world, type, durationTicks);
    }
}

