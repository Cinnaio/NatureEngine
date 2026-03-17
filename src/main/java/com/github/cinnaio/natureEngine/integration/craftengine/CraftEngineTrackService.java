package com.github.cinnaio.natureEngine.integration.craftengine;

import org.bukkit.block.Block;

/**
 * CraftEngine 软依赖的最小追踪接口（不引用 CraftEngine API，避免无 CraftEngine 时类加载失败）。
 */
public interface CraftEngineTrackService {
    void track(Block block);
}

