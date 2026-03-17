package com.github.cinnaio.natureEngine.core.environment;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.engine.config.SeasonConfigView;
import com.github.cinnaio.natureEngine.engine.config.WeatherConfigView;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Farmland;

/**
 * 计算指定方块位置的环境信息。
 * 当前实现偏简化，后续可引入温度/湿度/土壤系统的更复杂逻辑。
 */
public final class EnvironmentManager {

    private static final double HUMIDITY_MIN = 0.0;
    private static final double HUMIDITY_MAX = 1.0;
    private static final double SOIL_MIN = 0.0;
    private static final double SOIL_MAX = 1.0;

    private final SeasonManager seasonManager;
    private final SeasonConfigView seasonConfigView;
    private final WeatherManager weatherManager;
    private final WeatherConfigView weatherConfigView;

    public EnvironmentManager(
            SeasonManager seasonManager,
            SeasonConfigView seasonConfigView,
            WeatherManager weatherManager,
            WeatherConfigView weatherConfigView
    ) {
        this.seasonManager = seasonManager;
        this.seasonConfigView = seasonConfigView;
        this.weatherManager = weatherManager;
        this.weatherConfigView = weatherConfigView;
    }

    public EnvironmentContext getContext(Block block) {
        double temperature = estimateTemperatureBase(block);
        double humidity = estimateHumidityBase(block);
        double soilMoisture = estimateSoilMoisture(block);
        int light = block.getLightLevel();

        // 叠加季节增量（基于 vanilla 标尺）
        if (seasonManager != null && seasonConfigView != null) {
            var season = seasonManager.getCurrentSeason(block.getWorld());
            temperature += seasonConfigView.getTemperatureDelta(season);
            humidity = clamp(humidity + seasonConfigView.getHumidityDelta(season), HUMIDITY_MIN, HUMIDITY_MAX);
        }

        // 叠加天气配置的增量（用于更真实的环境变化）
        if (weatherManager != null && weatherConfigView != null) {
            WeatherType wt = weatherManager.getCurrentWeather(block.getWorld());
            var profile = weatherConfigView.getProfile(wt);
            temperature += profile.getTemperatureDelta();
            humidity = clamp(humidity + profile.getHumidityDelta(), HUMIDITY_MIN, HUMIDITY_MAX);
            soilMoisture = clamp(soilMoisture + profile.getSoilMoistureDelta(), SOIL_MIN, SOIL_MAX);
        }

        return new EnvironmentContext(temperature, humidity, soilMoisture, light);
    }

    private double estimateTemperatureBase(Block block) {
        // 优先使用 vanilla 气候采样（World#getTemperature(x,z)）
        World world = block.getWorld();
        int x = block.getX();
        int z = block.getZ();
        Double v = tryCallWorldDouble(world, "getTemperature", x, z);
        if (v != null) return v;
        // fallback：使用旧的简化估算，映射到 vanilla 标尺的大致区间
        String name = block.getBiome().name();
        if (name.contains("ICE") || name.contains("SNOW")) return 0.0;
        if (name.contains("DESERT") || name.contains("BADLANDS")) return 2.0;
        if (name.contains("SAVANNA")) return 1.6;
        return 0.8;
    }

    private double estimateHumidityBase(Block block) {
        World world = block.getWorld();
        int x = block.getX();
        int z = block.getZ();
        Double v = tryCallWorldDouble(world, "getHumidity", x, z);
        if (v != null) return clamp(v, HUMIDITY_MIN, HUMIDITY_MAX);
        String name = block.getBiome().name();
        if (name.contains("SWAMP")) return 0.9;
        if (name.contains("JUNGLE")) return 0.8;
        if (name.contains("DESERT") || name.contains("BADLANDS")) return 0.2;
        return 0.6;
    }

    private double estimateSoilMoisture(Block block) {
        // Folia/Luminol：避免跨 chunk/region 读取周围方块。
        // 这里仅读取“脚下方块”是否为耕地及其 moisture（同一 x/z，不会跨 region），保证安全。
        Block below = block.getRelative(BlockFace.DOWN);
        if (below.getBlockData() instanceof Farmland farmland) {
            int moist = farmland.getMoisture();
            int max = farmland.getMaximumMoisture();
            if (max <= 0) return 0.0;
            return clamp((double) moist / (double) max, SOIL_MIN, SOIL_MAX);
        }
        return 0.5;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Double tryCallWorldDouble(World world, String method, int x, int z) {
        try {
            var m = world.getClass().getMethod(method, int.class, int.class);
            Object r = m.invoke(world, x, z);
            if (r instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {
        }
        return null;
    }
}

