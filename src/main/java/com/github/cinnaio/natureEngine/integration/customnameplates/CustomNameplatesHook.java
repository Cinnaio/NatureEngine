package com.github.cinnaio.natureEngine.integration.customnameplates;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 * CustomNameplates 软依赖检测。
 * 季节/天气信息通过 PlaceholderAPI 占位符在 CustomNameplates 中显示，
 * 需同时安装 PlaceholderAPI 与 NatureEngine 的 PAPI 扩展。
 *
 * @see <a href="https://momi.gtemc.cn/customnameplates">CustomNameplates 文档</a>
 */
public final class CustomNameplatesHook {

    private final boolean present;

    public CustomNameplatesHook(Plugin owner) {
        PluginManager pm = owner.getServer().getPluginManager();
        this.present = pm.getPlugin("CustomNameplates") != null;
    }

    public boolean isPresent() {
        return present;
    }
}
