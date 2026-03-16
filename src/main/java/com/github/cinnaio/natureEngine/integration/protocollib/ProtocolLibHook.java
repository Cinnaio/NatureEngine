package com.github.cinnaio.natureEngine.integration.protocollib;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 强制依赖 ProtocolLib 的集成入口（仅检测与对外暴露存在性）。
 */
public final class ProtocolLibHook {

    private final Plugin protocolLib;

    public ProtocolLibHook(JavaPlugin owner) {
        PluginManager pm = owner.getServer().getPluginManager();
        this.protocolLib = pm.getPlugin("ProtocolLib");
        if (this.protocolLib == null || !this.protocolLib.isEnabled()) {
            throw new IllegalStateException("需要安装并启用 ProtocolLib 才能使用视觉季节系统。");
        }
    }

    public Plugin getProtocolLib() {
        return protocolLib;
    }
}

