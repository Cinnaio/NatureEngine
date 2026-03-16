package com.github.cinnaio.natureEngine.engine.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

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

    /**
     * Folia: 使用 GlobalRegionScheduler 的固定频率任务。
     */
    public ScheduledTask runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), delayTicks, periodTicks);
    }

    public void shutdown() {
        // 预留清理逻辑（如必要时取消任务）
    }
}

