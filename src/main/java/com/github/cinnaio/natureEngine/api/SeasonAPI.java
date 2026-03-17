package com.github.cinnaio.natureEngine.api;

import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.World;

public final class SeasonAPI {

    private static final ServiceLocator SERVICES = ServiceLocator.getInstance();

    private SeasonAPI() {
    }

    public static SeasonType getCurrentSeason(World world) {
        return SERVICES.get(SeasonManager.class).getCurrentSeason(world);
    }

    public static double getSeasonProgress(World world) {
        return SERVICES.get(SeasonManager.class).getSeasonProgress(world);
    }

    public static void setSeasonOverride(World world, SeasonType type) {
        SERVICES.get(SeasonManager.class).setSeasonOverride(world, type);
    }

    public static void clearSeasonOverride(World world) {
        SERVICES.get(SeasonManager.class).clearSeasonOverride(world);
    }

    public static boolean hasOverride(World world) {
        return SERVICES.get(SeasonManager.class).hasOverride(world);
    }

    public static long getCurrentSeasonLengthDays(World world) {
        return SERVICES.get(SeasonManager.class).getCurrentSeasonLengthDays(world);
    }

    public static SeasonType getNextSeasonType(World world) {
        return SERVICES.get(SeasonManager.class).getNextSeasonType(world);
    }

    public static long getDaysUntilNextSeason(World world) {
        return SERVICES.get(SeasonManager.class).getDaysUntilNextSeason(world);
    }
}

