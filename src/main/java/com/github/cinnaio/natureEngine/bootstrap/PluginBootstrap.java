package com.github.cinnaio.natureEngine.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * 插件启动入口的实际实现，由 {@link com.github.cinnaio.natureEngine.NatureEngine} 委托调用。
 */
public final class PluginBootstrap {

    private final LifecycleManager lifecycleManager;

    public PluginBootstrap(JavaPlugin plugin) {
        this.lifecycleManager = new LifecycleManager(plugin, ServiceLocator.getInstance());
    }

    public void onEnable() {
        lifecycleManager.onEnable();
    }

    public void onDisable() {
        lifecycleManager.onDisable();
    }
}

