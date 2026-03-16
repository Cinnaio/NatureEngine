package com.github.cinnaio.natureEngine.engine.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * 封装对 Folia/Paper 任务系统的基础访问。
 * 目前先提供一个简单的全局调度接口，后续可扩展 RegionTask、EntityTask 等。
 */
public final class GlobalScheduler {

    private final Plugin plugin;

    public GlobalScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // 预留初始化逻辑
    }

    public BukkitTask runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        // 在 Folia/Paper 1.21.4 上，Bukkit.getGlobalRegionScheduler() 是推荐方式
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public void shutdown() {
        // 预留清理逻辑（如必要时取消任务）
    }
}

