package com.github.cinnaio.natureEngine.core.environment;

import org.bukkit.block.Block;

/**
 * 统一的环境入口接口，供 EnvironmentAPI / Growth 等使用。
 */
public interface EnvironmentProvider {

    EnvironmentContext getContext(Block block, EnvironmentQuery query);

    default EnvironmentContext getContext(Block block) {
        return getContext(block, EnvironmentQuery.DEFAULT);
    }
}

