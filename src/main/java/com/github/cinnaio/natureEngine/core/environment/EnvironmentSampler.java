package com.github.cinnaio.natureEngine.core.environment;

import org.bukkit.block.Block;

/**
 * 负责从世界中采样基础环境量，不做季节/天气等修正。
 */
public interface EnvironmentSampler {

    EnvironmentContext sampleBase(Block block);
}

