package com.github.cinnaio.natureEngine.core.agriculture.weather;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.engine.config.WeatherConfigView;
import com.github.cinnaio.natureEngine.engine.scheduler.GlobalScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 负责查询与控制世界天气。
 * 目前实现与 Bukkit 世界天气保持同步，后续可加入更复杂的 WeatherController。
 */
public final class WeatherManager {

    private final JavaPlugin plugin;
    private final GlobalScheduler scheduler;
    private final SeasonManager seasonManager;
    private final WeatherConfigView configView;
    private final WeatherController controller;

    public WeatherManager(JavaPlugin plugin,
                          GlobalScheduler scheduler,
                          SeasonManager seasonManager,
                          WeatherConfigView configView,
                          WeatherController controller) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.seasonManager = seasonManager;
        this.configView = configView;
        this.controller = controller;
    }

    public void start() {
        long periodTicks = configView.getTickIntervalSeconds() * 20L;
        scheduler.runTaskTimer(this::tick, periodTicks, periodTicks);
    }

    private void tick() {
        for (World world : Bukkit.getWorlds()) {
            WeatherType next = controller.chooseNextWeather(seasonManager.getCurrentSeason(world));
            int durationTicks = configView.getDurationSeconds(next) * 20;
            setWeather(world, next, durationTicks);
        }
    }

    public WeatherType getCurrentWeather(World world) {
        if (world.hasStorm()) {
            return world.isThundering() ? WeatherType.STORM : WeatherType.RAIN;
        }
        // 简化：雪地生物群系下的降水可在之后通过 Environment 系统进一步细化为 SNOW
        return WeatherType.SUNNY;
    }

    public void setWeather(World world, WeatherType type, int durationTicks) {
        switch (type) {
            case SUNNY:
                world.setStorm(false);
                world.setThundering(false);
                break;
            case RAIN:
                world.setStorm(true);
                world.setThundering(false);
                break;
            case STORM:
                world.setStorm(true);
                world.setThundering(true);
                break;
            case SNOW:
            case CLOUDY:
            default:
                // 先简单映射到 RAIN，后续由 Environment 再区分具体表现
                world.setStorm(true);
                world.setThundering(false);
                break;
        }
        world.setWeatherDuration(durationTicks);
    }
}

