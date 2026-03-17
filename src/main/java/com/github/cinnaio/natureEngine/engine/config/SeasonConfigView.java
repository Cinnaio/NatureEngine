package com.github.cinnaio.natureEngine.engine.config;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonSettings;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 负责从 seasons.yml 解析每个季节的 SeasonSettings。
 */
public final class SeasonConfigView {

    private final FileConfiguration config;

    public SeasonConfigView(FileConfiguration config) {
        this.config = config;
    }

    public SeasonSettings getSettings(SeasonType type) {
        String path = "seasons." + type.name().toLowerCase();
        long lengthInDays = config.getLong(path + ".length-days", 10L);
        double baseTemp = config.getDouble(path + ".base-temperature", 15.0);
        double baseHumidity = config.getDouble(path + ".base-humidity", 0.6);
        double growthMultiplier = config.getDouble(path + ".growth-multiplier", 1.0);
        double yieldMultiplier = config.getDouble(path + ".yield-multiplier", 1.0);
        boolean easyToWither = config.getBoolean(path + ".easy-to-wither", false);
        return new SeasonSettings(lengthInDays, baseTemp, baseHumidity, growthMultiplier, yieldMultiplier, easyToWither);
    }

    public boolean isNotifyEnabled() {
        return config.getBoolean("seasons.notify.enabled", true);
    }

    /** actionbar | chat | both */
    public String getNotifyMode() {
        return config.getString("seasons.notify.mode", "actionbar");
    }

    public String getNotifyMessageTemplate() {
        return config.getString("seasons.notify.message", "&a季节已切换为 &e{season}");
    }
}

