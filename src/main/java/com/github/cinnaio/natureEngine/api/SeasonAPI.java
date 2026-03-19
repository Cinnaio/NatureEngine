package com.github.cinnaio.natureEngine.api;

import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.SolarTerm;
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

    public static SolarTerm getCurrentSolarTerm(World world) {
        return SERVICES.get(SeasonManager.class).getCurrentSolarTerm(world);
    }

    public static double getSolarTermProgress(World world) {
        return SERVICES.get(SeasonManager.class).getSolarTermProgress(world);
    }

    public static long getDaysUntilNextSolarTerm(World world) {
        return SERVICES.get(SeasonManager.class).getDaysUntilNextSolarTerm(world);
    }

    /** 基于“时间基准世界”的第几天（从 0 开始）。 */
    public static long getWorldDay(World world) {
        return SERVICES.get(SeasonManager.class).getWorldDay(world);
    }

    /** 一年总天数（四季 length-days 之和）。 */
    public static long getYearLengthDays() {
        return SERVICES.get(SeasonManager.class).getYearLengthDays();
    }

    /** 第几年（从 1 开始）。 */
    public static long getYear(World world) {
        return SERVICES.get(SeasonManager.class).getYear(world);
    }

    /** 年内第几天（从 1 开始）。 */
    public static long getDayInYear(World world) {
        return SERVICES.get(SeasonManager.class).getDayInYear(world);
    }
}

