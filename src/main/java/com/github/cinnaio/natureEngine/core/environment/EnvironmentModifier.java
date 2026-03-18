package com.github.cinnaio.natureEngine.core.environment;

import org.bukkit.block.Block;

/**
 * 在基础 + 维度信息上叠加季节/天气/事件等修正。
 */
public interface EnvironmentModifier {

    EnvironmentContext apply(Block block, EnvironmentContext current, EnvironmentQuery query);
}

