package com.github.cinnaio.natureEngine.core.environment;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.engine.config.EnvironmentConfigView;
import com.github.cinnaio.natureEngine.engine.config.SeasonConfigView;
import com.github.cinnaio.natureEngine.engine.config.WeatherConfigView;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Farmland;

/**
 * 计算指定方块位置的环境信息。
 * 作为环境管线的编排器：基础采样 -> 维度扩展 -> 季节/天气修正。
 */
public final class EnvironmentManager implements EnvironmentProvider, EnvironmentSampler, EnvironmentDimension, EnvironmentModifier {

    private static final double HUMIDITY_MIN = 0.0;
    private static final double HUMIDITY_MAX = 1.0;
    private static final double SOIL_MIN = 0.0;
    private static final double SOIL_MAX = 1.0;

    private final SeasonManager seasonManager;
    private final SeasonConfigView seasonConfigView;
    private final WeatherManager weatherManager;
    private final WeatherConfigView weatherConfigView;
    private final EnvironmentConfigView environmentConfigView;

    public EnvironmentManager(
            SeasonManager seasonManager,
            SeasonConfigView seasonConfigView,
            WeatherManager weatherManager,
            WeatherConfigView weatherConfigView,
            EnvironmentConfigView environmentConfigView
    ) {
        this.seasonManager = seasonManager;
        this.seasonConfigView = seasonConfigView;
        this.weatherManager = weatherManager;
        this.weatherConfigView = weatherConfigView;
        this.environmentConfigView = environmentConfigView;
    }

    @Override
    public EnvironmentContext getContext(Block block, EnvironmentQuery query) {
        // environment.enabled=false 时退回 v1：仅输出旧版四字段（并保留季节/天气修正），不计算扩展维度
        if (environmentConfigView != null && !environmentConfigView.isEnabled()) {
            EnvironmentContext base = sampleBase(block);
            EnvironmentContext applied = apply(block, base, query);
            return new EnvironmentContext(
                    applied.getTemperature(),
                    applied.getHumidity(),
                    applied.getSoilMoisture(),
                    applied.getLightLevel()
            );
        }

        // 1) 基础采样
        EnvironmentContext base = sampleBase(block);
        // 2) 维度扩展（室内外/光照拆分/海拔/群系等）
        EnvironmentContext withDims = enrich(block, base, query);
        // 3) 季节/天气修正
        return apply(block, withDims, query);
    }

    @Override
    public EnvironmentContext sampleBase(Block block) {
        double temperature = estimateTemperatureBase(block);
        double humidity = estimateHumidityBase(block);
        double soilMoisture = estimateSoilMoisture(block);
        int light = block.getLightLevel();
        return EnvironmentContext.builder()
                .temperature(temperature)
                .humidity(humidity)
                .soilMoisture(soilMoisture)
                .lightLevel(light)
                .build();
    }

    @Override
    public EnvironmentContext enrich(Block block, EnvironmentContext base, EnvironmentQuery query) {
        // 光照拆分
        int skyLight = block.getLightFromSky();
        int blockLight = base.getLightLevel() - skyLight;
        if (blockLight < 0) blockLight = 0;

        // 室内/室外估算：
        // - 基础几何露天程度：ratio = skyLight / totalLight，用于布尔判定（是否露天）
        // - 室外得分：在 ratio 基础上再乘以时间/天气因子，用于物理强度（昼夜、风暴等）
        double outdoorScore;
        if (base.getLightLevel() <= 0) {
            outdoorScore = 0.0;
        } else {
            double ratio = (double) skyLight / (double) base.getLightLevel();
            ratio = clamp(ratio, 0.0, 1.0);

            World world = block.getWorld();

            // 时间因子：白天≈1.0，夜晚明显降低（只影响得分，不影响“是否露天”的几何判定）
            double timeFactor = 1.0;
            try {
                long t = world.getTime() % 24000L;
                if (t >= 12000L) {
                    // 夜晚：弱化露天效果
                    timeFactor = 0.3;
                }
            } catch (Throwable ignored) {
            }

            // 天气因子：默认使用 environment.yml 中的 weather-factor 配置（同样只影响得分）
            double weatherFactor = 1.0;
            try {
                WeatherType wt = weatherManager != null ? weatherManager.getCurrentWeather(world) : null;
                if (environmentConfigView != null && wt != null) {
                    weatherFactor = environmentConfigView.getOutdoorWeatherFactor(wt);
                } else if (environmentConfigView != null && wt == null && world.hasStorm()) {
                    weatherFactor = environmentConfigView.getOutdoorWeatherFactor(WeatherType.STORM);
                }
            } catch (Throwable ignored) {
            }

            outdoorScore = clamp(ratio * timeFactor * weatherFactor, 0.0, 1.0);

            // 布尔“室外”仅依据几何 ratio 与阈值，保证在夜晚/恶劣天气下仍能识别为露天
            double threshold = environmentConfigView != null ? environmentConfigView.getOutdoorThreshold() : 0.6;
            boolean outdoor = ratio >= threshold;

            // 海拔
            int y = block.getY();

            // 温室 / 封闭空间得分
            double greenhouseScore = computeGreenhouseScore(block, base, outdoor);

            // 群系与 biome key（groupId 暂不在此解析，仅提供原始 key）
            Biome biome = block.getBiome();
            NamespacedKey biomeKey = safeBiomeKey(biome);

            return EnvironmentContext.builder()
                    .temperature(base.getTemperature())
                    .humidity(base.getHumidity())
                    .soilMoisture(base.getSoilMoisture())
                    .lightLevel(base.getLightLevel())
                    .skyLight(skyLight)
                    .blockLight(blockLight)
                    .outdoor(outdoor)
                    .outdoorScore(outdoorScore)
                    .altitudeY(y)
                    .nearWaterScore(estimateNearWaterScore(base))
                    .greenhouseScore(greenhouseScore)
                    .inGreenhouse(isInGreenhouse(greenhouseScore))
                    .biome(biome)
                    .biomeKey(biomeKey)
                    .biomeGroupId(null)
                    .build();
        }
        // 完全无光时视为室内且得分为 0
        double threshold = environmentConfigView != null ? environmentConfigView.getOutdoorThreshold() : 0.6;
        boolean outdoor = false;
        int y = block.getY();
        Biome biome = block.getBiome();
        NamespacedKey biomeKey = safeBiomeKey(biome);
        return EnvironmentContext.builder()
                .temperature(base.getTemperature())
                .humidity(base.getHumidity())
                .soilMoisture(base.getSoilMoisture())
                .lightLevel(base.getLightLevel())
                .skyLight(skyLight)
                .blockLight(blockLight)
                .outdoor(outdoor)
                .outdoorScore(0.0)
                .altitudeY(y)
                .nearWaterScore(estimateNearWaterScore(base))
                 // 完全黑暗时默认不处于温室
                .greenhouseScore(0.0)
                .inGreenhouse(false)
                .biome(biome)
                .biomeKey(biomeKey)
                .biomeGroupId(null)
                .build();
    }

    @Override
    public EnvironmentContext apply(Block block, EnvironmentContext current, EnvironmentQuery query) {
        double temperature = current.getTemperature();
        double humidity = current.getHumidity();
        double soilMoisture = current.getSoilMoisture();

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

        return EnvironmentContext.builder()
                .temperature(temperature)
                .humidity(humidity)
                .soilMoisture(soilMoisture)
                .lightLevel(current.getLightLevel())
                .skyLight(current.getSkyLight())
                .blockLight(current.getBlockLight())
                .outdoor(current.isOutdoor())
                .outdoorScore(current.getOutdoorScore())
                .altitudeY(current.getAltitudeY())
                .nearWaterScore(current.getNearWaterScore())
                .biome(current.getBiome())
                .biomeKey(current.getBiomeKey())
                .biomeGroupId(current.getBiomeGroupId())
                .build();
    }

    private double estimateTemperatureBase(Block block) {
        // 优先使用 vanilla 气候采样（World#getTemperature(x,z)）
        World world = block.getWorld();
        int x = block.getX();
        int z = block.getZ();
        Double v = tryCallWorldDouble(world, "getTemperature", x, z);
        if (v != null) return v;
        // fallback：使用旧的简化估算，映射到 vanilla 标尺的大致区间
        String name = block.getBiome().toString();
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
        String name = block.getBiome().toString();
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

    /**
     * 目前 nearWaterScore 与土壤湿度保持一致，未来可按需扩展为邻近水源扫描。
     */
    private double estimateNearWaterScore(EnvironmentContext base) {
        return base.getSoilMoisture();
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

    private static NamespacedKey safeBiomeKey(Biome biome) {
        try {
            return biome != null ? biome.getKey() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private double computeGreenhouseScore(Block center, EnvironmentContext base, boolean outdoorFlag) {
        if (environmentConfigView == null || !environmentConfigView.isGreenhouseEnabled()) {
            return 0.0;
        }
        World world = center.getWorld();
        int radius = environmentConfigView.getGreenhouseRadius();
        int maxRoof = environmentConfigView.getGreenhouseMaxRoofHeight();
        double gamma = environmentConfigView.getGreenhouseGamma();

        // 屋顶评分：越低的屋顶分越高；完全无屋顶则 0
        double roofScore = 0.0;
        for (int dy = 1; dy <= maxRoof; dy++) {
            Block above = center.getRelative(0, dy, 0);
            if (isRoofBlock(above)) {
                // 线性衰减：closer roof -> higher score
                roofScore = 1.0 - ((double) (dy - 1) / (double) maxRoof);
                break;
            }
        }

        // 四周墙体比例（简单 8 方向）
        int[][] dirs = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        int hit = 0;
        int total = dirs.length;
        int baseY = center.getY();
        for (int[] d : dirs) {
            boolean foundWall = false;
            for (int step = 1; step <= radius; step++) {
                int dx = center.getX() + d[0] * step;
                int dz = center.getZ() + d[1] * step;
                Block candidate = world.getBlockAt(dx, baseY, dz);
                // 在 y-1..y+2 高度带内简单检查一列
                for (int dy = -1; dy <= 2; dy++) {
                    Block col = candidate.getRelative(0, dy, 0);
                    if (isWallBlock(col)) {
                        foundWall = true;
                        break;
                    }
                }
                if (foundWall) break;
            }
            if (foundWall) hit++;
        }
        double wallRatio = total > 0 ? (double) hit / (double) total : 0.0;

        double baseScore = roofScore * wallRatio;
        if (baseScore <= 0.0) {
            return 0.0;
        }
        double score = Math.pow(baseScore, gamma);

        // 如果本身就不被判定为露天（outdoor=false），则可适当抬高温室分数
        if (!outdoorFlag) {
            score = Math.max(score, 0.4);
        }

        return clamp(score, 0.0, 1.0);
    }

    private boolean isInGreenhouse(double greenhouseScore) {
        if (environmentConfigView == null || !environmentConfigView.isGreenhouseEnabled()) {
            return false;
        }
        double th = environmentConfigView.getGreenhouseThreshold();
        return greenhouseScore >= th;
    }

    private boolean isRoofBlock(Block block) {
        // 简化规则：非空气且非植物方块，或是玻璃类，视为可形成屋顶
        if (block.isEmpty()) return false;
        String type = block.getType().name();
        if (type.contains("GLASS")) return true;
        // 粗略排除植被
        if (type.contains("LEAVES") || type.contains("GRASS") || type.contains("FLOWER") || type.contains("SAPLING")) {
            return false;
        }
        return block.getType().isSolid();
    }

    private boolean isWallBlock(Block block) {
        // 与屋顶类似的判定，用于围墙
        return isRoofBlock(block);
    }
}

