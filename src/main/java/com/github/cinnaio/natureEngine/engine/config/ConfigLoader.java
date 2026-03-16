package com.github.cinnaio.natureEngine.engine.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 通用 YAML 加载工具，支持从默认资源拷贝到磁盘再加载。
 */
public final class ConfigLoader {

    private final Plugin plugin;

    public ConfigLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration loadOrSaveDefault(String fileName) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("无法创建插件数据目录: " + dataFolder);
        }

        File target = new File(dataFolder, fileName);
        if (!target.exists()) {
            // 尝试从 jar 资源中复制默认文件
            try (InputStream in = plugin.getResource(fileName)) {
                if (in != null) {
                    YamlConfiguration defaultCfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                    defaultCfg.save(target);
                } else {
                    // 没有默认资源，则创建空文件
                    target.createNewFile();
                }
            } catch (Exception e) {
                throw new IllegalStateException("加载默认配置文件失败: " + fileName, e);
            }
        }

        return YamlConfiguration.loadConfiguration(target);
    }
}

