package com.github.cinnaio.natureEngine.core.agriculture.season;

import com.github.cinnaio.natureEngine.engine.config.SeasonConfigView;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 对外暴露季节相关查询与控制。
 * 目前实现为基于世界时间的纯计算，后续可接 DataManager 做持久化与手动覆写。
 */
public final class SeasonManager implements SeasonCycle.SeasonSettingsProvider {

    private static final long TICKS_PER_DAY = 24000L;

    private final SeasonCycle seasonCycle;

    private final SeasonConfigView configView;

    private final Map<UUID, SeasonType> overrides = new HashMap<>();

    public SeasonManager(SeasonConfigView configView) {
        this.configView = configView;
        this.seasonCycle = new SeasonCycle(this);
    }

    public SeasonType getCurrentSeason(World world) {
        SeasonType overridden = overrides.get(world.getUID());
        if (overridden != null) {
            return overridden;
        }
        long worldDay = world.getFullTime() / TICKS_PER_DAY;
        return seasonCycle.getSeasonForWorld(world, worldDay).getType();
    }

    public double getSeasonProgress(World world) {
        if (overrides.containsKey(world.getUID())) {
            return 0.0D;
        }
        long worldDay = world.getFullTime() / TICKS_PER_DAY;
        return seasonCycle.getSeasonForWorld(world, worldDay).getProgress();
    }

    public SolarTerm getCurrentSolarTerm(World world) {
        if (overrides.containsKey(world.getUID())) {
            // 只有季节覆写：节气按该季第 1 个处理
            return SolarTerm.bySeasonIndex(getCurrentSeason(world), 0);
        }
        long worldDay = world.getFullTime() / TICKS_PER_DAY;
        SolarTerm t = seasonCycle.getSeasonForWorld(world, worldDay).getSolarTerm();
        return t != null ? t : SolarTerm.bySeasonIndex(getCurrentSeason(world), 0);
    }

    public double getSolarTermProgress(World world) {
        if (overrides.containsKey(world.getUID())) {
            return 0.0D;
        }
        long worldDay = world.getFullTime() / TICKS_PER_DAY;
        return seasonCycle.getSeasonForWorld(world, worldDay).getSolarTermProgress();
    }

    /** 距离下一节气的天数（向上取整为整天）。 */
    public long getDaysUntilNextSolarTerm(World world) {
        if (overrides.containsKey(world.getUID())) {
            return getCurrentSeasonLengthDays(world);
        }
        long seasonLen = getCurrentSeasonLengthDays(world);
        if (seasonLen <= 0) return 1;
        double termLen = (double) seasonLen / 6.0;
        double progress = getSolarTermProgress(world);
        double remaining = (1.0 - progress) * termLen;
        return Math.max(1, (long) Math.ceil(remaining));
    }

    /** 当前季节将持续的天数（来自配置）。 */
    public long getCurrentSeasonLengthDays(World world) {
        return configView.getSettings(getCurrentSeason(world)).getLengthInDays();
    }

    /** 下一季节类型。 */
    public SeasonType getNextSeasonType(World world) {
        return getNextSeasonType(getCurrentSeason(world));
    }

    private static SeasonType getNextSeasonType(SeasonType current) {
        return switch (current) {
            case SPRING -> SeasonType.SUMMER;
            case SUMMER -> SeasonType.AUTUMN;
            case AUTUMN -> SeasonType.WINTER;
            case WINTER -> SeasonType.SPRING;
        };
    }

    /** 距离下一季节的天数（含小数，向上取整为整天）。 */
    public long getDaysUntilNextSeason(World world) {
        double progress = getSeasonProgress(world);
        long length = getCurrentSeasonLengthDays(world);
        double remaining = (1.0 - progress) * length;
        return Math.max(1, (long) Math.ceil(remaining));
    }

    public void setSeasonOverride(World world, SeasonType type) {
        overrides.put(world.getUID(), type);
    }

    public void clearSeasonOverride(World world) {
        overrides.remove(world.getUID());
    }

    public boolean hasOverride(World world) {
        return overrides.containsKey(world.getUID());
    }

    @Override
    public SeasonSettings getSettings(SeasonType type) {
        return configView.getSettings(type);
    }
}

