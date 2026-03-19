package com.github.cinnaio.natureEngine.engine.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 时间轴基准配置（用于季节/节气/年份等计算）。
 */
public final class TimeConfigView {

    public enum BaseMode {
        PRIMARY,
        PER_WORLD
    }

    private final FileConfiguration config;

    public TimeConfigView(FileConfiguration config) {
        this.config = config;
    }

    public BaseMode getBaseMode() {
        String raw = config.getString("time.base-mode", "primary");
        if (raw == null) return BaseMode.PRIMARY;
        return "per-world".equalsIgnoreCase(raw) ? BaseMode.PER_WORLD : BaseMode.PRIMARY;
    }

    /** 仅在 base-mode=primary 时使用。 */
    public String getPrimaryWorldName() {
        String w = config.getString("time.primary-world", "world");
        return (w == null || w.isBlank()) ? "world" : w.trim();
    }
}

