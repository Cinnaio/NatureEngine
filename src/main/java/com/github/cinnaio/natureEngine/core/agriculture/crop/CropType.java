package com.github.cinnaio.natureEngine.core.agriculture.crop;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.growth.CropEnvironmentPolicy;

import java.util.Set;

/**
 * 作物/植物的抽象类型定义。
 * 后续无论是原版作物还是来自 CraftEngine 的自定义植物，
 * 都应通过实现本接口接入生长管线。
 */
public interface CropType {

    String getId();

    int getStages();

    long getBaseTicksPerStage();

    double getOptimalTemperature();

    double getTemperatureTolerance();

    double getOptimalHumidity();

    double getHumidityTolerance();

    int getMinLight();

    Set<SeasonType> getPreferredSeasons();

    boolean isEnabled();

    /**
     * 作物级环境策略覆写（可选）。返回 null 表示使用全局默认。
     */
    default CropEnvironmentPolicy getEnvironmentPolicy() {
        return null;
    }
}

