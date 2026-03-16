package com.github.cinnaio.natureEngine.core.environment;

import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.engine.config.WeatherConfigView;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Biome;

/**
 * 计算指定方块位置的环境信息。
 * 当前实现偏简化，后续可引入温度/湿度/土壤系统的更复杂逻辑。
 */
public final class EnvironmentManager {

    private final WeatherManager weatherManager;
    private final WeatherConfigView weatherConfigView;

    public EnvironmentManager(WeatherManager weatherManager, WeatherConfigView weatherConfigView) {
        this.weatherManager = weatherManager;
        this.weatherConfigView = weatherConfigView;
    }

    public EnvironmentContext getContext(Block block) {
        double temperature = estimateTemperature(block);
        double humidity = estimateHumidity(block);
        double soilMoisture = estimateSoilMoisture(block);
        int light = block.getLightFromBlocks();

        // 叠加天气配置的增量（用于更真实的环境变化）
        if (weatherManager != null && weatherConfigView != null) {
            WeatherType wt = weatherManager.getCurrentWeather(block.getWorld());
            var profile = weatherConfigView.getProfile(wt);
            temperature += profile.getTemperatureDelta();
            humidity += profile.getHumidityDelta();
            soilMoisture += profile.getSoilMoistureDelta();
        }

        return new EnvironmentContext(temperature, humidity, soilMoisture, light);
    }

    private double estimateTemperature(Block block) {
        Biome biome = block.getBiome();
        // 使用名称字符串判断，避免依赖特定版本的枚举常量
        String name = biome.name();
        if (name.contains("ICE") || name.contains("SNOW")) {
            return -5.0;
        }
        if (name.contains("DESERT") || name.contains("BADLANDS")) {
            return 30.0;
        }
        if (name.contains("SAVANNA")) {
            return 25.0;
        }
        return 15.0;
    }

    private double estimateHumidity(Block block) {
        Biome biome = block.getBiome();
        String name = biome.name();
        if (name.contains("SWAMP")) {
            return 0.9;
        }
        if (name.contains("JUNGLE")) {
            return 0.8;
        }
        if (name.contains("DESERT") || name.contains("BADLANDS")) {
            return 0.2;
        }
        return 0.6;
    }

    private double estimateSoilMoisture(Block block) {
        // 简化：检测方块上方是否暴露、周围是否有水
        boolean hasWaterNearby = false;
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (block.getRelative(face).isLiquid()) {
                hasWaterNearby = true;
                break;
            }
        }
        return hasWaterNearby ? 0.8 : 0.4;
    }
}

