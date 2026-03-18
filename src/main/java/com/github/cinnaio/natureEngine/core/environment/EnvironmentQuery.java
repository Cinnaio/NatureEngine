package com.github.cinnaio.natureEngine.core.environment;

/**
 * 预留的查询参数容器，目前仅作占位。
 * 未来可用于控制采样精度、禁用某些昂贵维度等。
 */
public final class EnvironmentQuery {

    public static final EnvironmentQuery DEFAULT = new EnvironmentQuery();

    private EnvironmentQuery() {
    }
}

