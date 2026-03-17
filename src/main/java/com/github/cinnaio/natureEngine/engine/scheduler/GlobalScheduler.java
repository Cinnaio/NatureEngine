package com.github.cinnaio.natureEngine.engine.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装对 Folia/Paper 任务系统的基础访问。
 * 在 onDisable 时取消所有已注册的全局任务，避免 reload 后旧 ClassLoader 仍被调度导致 NoClassDefFoundError。
 */
public final class GlobalScheduler {

    private final Plugin plugin;
    private final List<ScheduledTask> tasks = new ArrayList<>();

    public GlobalScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // 预留初始化逻辑
    }

    /**
     * Folia: 使用 GlobalRegionScheduler 的固定频率任务；任务会在 shutdown() 时被取消。
     */
    public ScheduledTask runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> runnable.run(), delayTicks, periodTicks);
        tasks.add(task);
        return task;
    }

    public void shutdown() {
        for (ScheduledTask task : tasks) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
            }
        }
        tasks.clear();
    }
}

