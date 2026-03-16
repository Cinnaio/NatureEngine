package com.github.cinnaio.natureEngine.debug;

import com.github.cinnaio.natureEngine.engine.config.DebugConfigView;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一的 Debug 日志入口。
 */
public final class DebugLogger {

    private final Plugin plugin;
    private DebugConfigView configView;

    public DebugLogger(Plugin plugin, DebugConfigView configView) {
        this.plugin = plugin;
        this.configView = configView;
    }

    public void updateConfig(DebugConfigView view) {
        this.configView = view;
    }

    public void log(DebugModule module, DebugLevel level, String message) {
        if (!configView.isEnabled()) {
            return;
        }
        DebugLevel moduleLevel = configView.getLevelForModule(module);
        if (!level.isEnabledAt(moduleLevel)) {
            return;
        }
        String prefix = "[" + module.name() + "/" + level.name() + "] ";
        String line = prefix + message;

        plugin.getLogger().info(line);

        if (configView.isLogToFile()) {
            writeToFile(line);
        }
    }

    private void writeToFile(String line) {
        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            return;
        }
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log";
        File target = new File(logDir, fileName);
        try (FileWriter writer = new FileWriter(target, true)) {
            writer.write(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + line + System.lineSeparator());
        } catch (IOException ignored) {
        }
    }
}

