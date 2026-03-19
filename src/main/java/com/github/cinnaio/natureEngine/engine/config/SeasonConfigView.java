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

    /** vanilla 标尺环境温度增量（建议范围约 -0.25 ~ +0.25）。 */
    public double getTemperatureDelta(SeasonType type) {
        String path = "seasons." + type.name().toLowerCase();
        return config.getDouble(path + ".temperature-delta", 0.0);
    }

    /** 环境湿度增量（0..1），建议范围约 -0.10 ~ +0.10。 */
    public double getHumidityDelta(SeasonType type) {
        String path = "seasons." + type.name().toLowerCase();
        return config.getDouble(path + ".humidity-delta", 0.0);
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

    public int getNotifyTitleFadeInTicks() {
        return Math.max(0, config.getInt("seasons.notify.title.fade-in-ticks", 10));
    }

    public int getNotifyTitleStayTicks() {
        return Math.max(0, config.getInt("seasons.notify.title.stay-ticks", 50));
    }

    public int getNotifyTitleFadeOutTicks() {
        return Math.max(0, config.getInt("seasons.notify.title.fade-out-ticks", 20));
    }
}

