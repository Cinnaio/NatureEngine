package com.github.cinnaio.natureEngine.core.environment;

import org.bukkit.block.Block;

/**
 * 用于在基础环境之上计算额外维度（室内外、光照拆分、群系组等）。
 */
public interface EnvironmentDimension {

    EnvironmentContext enrich(Block block, EnvironmentContext base, EnvironmentQuery query);
}

