package com.github.cinnaio.natureEngine;

import com.github.cinnaio.natureEngine.bootstrap.PluginBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class NatureEngine extends JavaPlugin {

    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new PluginBootstrap(this);
        bootstrap.onEnable();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.onDisable();
        }
    }
}
