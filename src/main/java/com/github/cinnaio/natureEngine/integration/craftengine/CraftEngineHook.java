package com.github.cinnaio.natureEngine.integration.craftengine;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 * CraftEngine 集成的占位实现。
 * 目前只检测 CraftEngine 是否存在，并预留后续对接其植物 API 的接口。
 */
public final class CraftEngineHook {

    private final boolean present;

    public CraftEngineHook(Plugin owner) {
        PluginManager pm = owner.getServer().getPluginManager();
        this.present = pm.getPlugin("CraftEngine") != null;
    }

    public boolean isPresent() {
        return present;
    }

    // 之后在这里补充与 CraftEngine 植物注册、生长 Tick 对接的方法
}

