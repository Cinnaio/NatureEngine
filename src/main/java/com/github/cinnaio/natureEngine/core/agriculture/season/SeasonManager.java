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

