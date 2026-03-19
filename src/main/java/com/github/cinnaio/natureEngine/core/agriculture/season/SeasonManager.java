package com.github.cinnaio.natureEngine.core.agriculture.season;

import com.github.cinnaio.natureEngine.engine.config.SeasonConfigView;
import com.github.cinnaio.natureEngine.engine.config.TimeConfigView;
import org.bukkit.Bukkit;
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
    private final TimeConfigView timeConfig;

    private final Map<UUID, SeasonType> overrides = new HashMap<>();

    public SeasonManager(SeasonConfigView configView, TimeConfigView timeConfig) {
        this.configView = configView;
        this.timeConfig = timeConfig;
        this.seasonCycle = new SeasonCycle(this);
    }

    public SeasonType getCurrentSeason(World world) {
        World base = resolveBaseWorld(world);
        SeasonType overridden = overrides.get(base.getUID());
        if (overridden != null) {
            return overridden;
        }
        long worldDay = base.getFullTime() / TICKS_PER_DAY;
        return seasonCycle.getSeasonForWorld(base, worldDay).getType();
    }

    public double getSeasonProgress(World world) {
        World base = resolveBaseWorld(world);
        if (overrides.containsKey(base.getUID())) {
            return 0.0D;
        }
        long worldDay = base.getFullTime() / TICKS_PER_DAY;
        return seasonCycle.getSeasonForWorld(base, worldDay).getProgress();
    }

    public SolarTerm getCurrentSolarTerm(World world) {
        World base = resolveBaseWorld(world);
        if (overrides.containsKey(base.getUID())) {
            // 只有季节覆写：节气按该季第 1 个处理
            return SolarTerm.bySeasonIndex(getCurrentSeason(base), 0);
        }
        long worldDay = base.getFullTime() / TICKS_PER_DAY;
        SolarTerm t = seasonCycle.getSeasonForWorld(base, worldDay).getSolarTerm();
        return t != null ? t : SolarTerm.bySeasonIndex(getCurrentSeason(base), 0);
    }

    public double getSolarTermProgress(World world) {
        World base = resolveBaseWorld(world);
        if (overrides.containsKey(base.getUID())) {
            return 0.0D;
        }
        long worldDay = base.getFullTime() / TICKS_PER_DAY;
        return seasonCycle.getSeasonForWorld(base, worldDay).getSolarTermProgress();
    }

    /** 距离下一节气的天数（向上取整为整天）。 */
    public long getDaysUntilNextSolarTerm(World world) {
        World base = resolveBaseWorld(world);
        if (overrides.containsKey(base.getUID())) {
            return getCurrentSeasonLengthDays(base);
        }
        long seasonLen = getCurrentSeasonLengthDays(base);
        if (seasonLen <= 0) return 1;
        double termLen = (double) seasonLen / 6.0;
        double progress = getSolarTermProgress(base);
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
        World base = resolveBaseWorld(world);
        double progress = getSeasonProgress(world);
        long length = getCurrentSeasonLengthDays(base);
        double remaining = (1.0 - progress) * length;
        return Math.max(1, (long) Math.ceil(remaining));
    }

    /** 基于“时间基准世界”的第几天（从 0 开始）。 */
    public long getWorldDay(World world) {
        World base = resolveBaseWorld(world);
        return base.getFullTime() / TICKS_PER_DAY;
    }

    public void setSeasonOverride(World world, SeasonType type) {
        World base = resolveBaseWorld(world);
        overrides.put(base.getUID(), type);
    }

    public void clearSeasonOverride(World world) {
        World base = resolveBaseWorld(world);
        overrides.remove(base.getUID());
    }

    public boolean hasOverride(World world) {
        World base = resolveBaseWorld(world);
        return overrides.containsKey(base.getUID());
    }

    @Override
    public SeasonSettings getSettings(SeasonType type) {
        return configView.getSettings(type);
    }

    private World resolveBaseWorld(World world) {
        if (world == null) {
            World any = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (any != null) return any;
            throw new IllegalStateException("No worlds loaded");
        }
        if (timeConfig == null) return world;
        if (timeConfig.getBaseMode() == TimeConfigView.BaseMode.PER_WORLD) {
            return world;
        }
        String name = timeConfig.getPrimaryWorldName();
        World primary = Bukkit.getWorld(name);
        return primary != null ? primary : world;
    }
}

