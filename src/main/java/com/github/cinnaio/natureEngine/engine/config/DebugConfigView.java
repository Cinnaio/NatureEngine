package com.github.cinnaio.natureEngine.engine.config;

import com.github.cinnaio.natureEngine.debug.DebugLevel;
import com.github.cinnaio.natureEngine.debug.DebugModule;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Debug 配置读取封装。
 */
public final class DebugConfigView {

    private final FileConfiguration config;

    public DebugConfigView(FileConfiguration config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.getBoolean("debug.enabled", true);
    }

    public boolean isLogToFile() {
        return config.getBoolean("debug.log-to-file", false);
    }

    public DebugLevel getDefaultLevel() {
        String raw = config.getString("debug.default-level", "INFO");
        return DebugLevel.fromString(raw);
    }

    public DebugLevel getLevelForModule(DebugModule module) {
        String path = "debug.modules." + module.getConfigKey();
        String raw = config.getString(path, null);
        if (raw == null) {
            return getDefaultLevel();
        }
        return DebugLevel.fromString(raw);
    }
}

