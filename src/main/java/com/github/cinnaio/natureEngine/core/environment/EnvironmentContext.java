package com.github.cinnaio.natureEngine.core.environment;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;

/**
 * 聚合某个坐标点的环境信息，供生长计算使用。
 *
 * 为兼容旧逻辑，保留原有四个核心字段（温度/湿度/土壤湿度/总光照），
 * 同时增加若干可选维度，用于更精细的生长控制与调试。
 */
public final class EnvironmentContext {

    // 核心字段：旧逻辑已经在使用
    private final double temperature;
    private final double humidity;
    private final double soilMoisture;
    private final int lightLevel;

    // 新增维度（可为“未知”或 -1 / null）
    private final int skyLight;
    private final int blockLight;
    private final boolean outdoor;
    private final double outdoorScore;
    private final int altitudeY;
    private final double nearWaterScore;
    private final double greenhouseScore;
    private final boolean inGreenhouse;
    private final Biome biome;
    private final NamespacedKey biomeKey;
    private final String biomeGroupId;

    private EnvironmentContext(Builder builder) {
        this.temperature = builder.temperature;
        this.humidity = builder.humidity;
        this.soilMoisture = builder.soilMoisture;
        this.lightLevel = builder.lightLevel;
        this.skyLight = builder.skyLight;
        this.blockLight = builder.blockLight;
        this.outdoor = builder.outdoor;
        this.outdoorScore = builder.outdoorScore;
        this.altitudeY = builder.altitudeY;
        this.nearWaterScore = builder.nearWaterScore;
        this.greenhouseScore = builder.greenhouseScore;
        this.inGreenhouse = builder.inGreenhouse;
        this.biome = builder.biome;
        this.biomeKey = builder.biomeKey;
        this.biomeGroupId = builder.biomeGroupId;
    }

    /**
     * 兼容旧代码的构造方法，仅设置核心字段。
     */
    public EnvironmentContext(double temperature, double humidity, double soilMoisture, int lightLevel) {
        this(new Builder()
                .temperature(temperature)
                .humidity(humidity)
                .soilMoisture(soilMoisture)
                .lightLevel(lightLevel)
        );
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getSoilMoisture() {
        return soilMoisture;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public int getSkyLight() {
        return skyLight;
    }

    public int getBlockLight() {
        return blockLight;
    }

    public boolean isOutdoor() {
        return outdoor;
    }

    public double getOutdoorScore() {
        return outdoorScore;
    }

    public int getAltitudeY() {
        return altitudeY;
    }

    public double getNearWaterScore() {
        return nearWaterScore;
    }

    public double getGreenhouseScore() {
        return greenhouseScore;
    }

    public boolean isInGreenhouse() {
        return inGreenhouse;
    }

    public Biome getBiome() {
        return biome;
    }

    public NamespacedKey getBiomeKey() {
        return biomeKey;
    }

    public String getBiomeGroupId() {
        return biomeGroupId;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 便于 debug 输出的简单键值视图，不做本地化。
     */
    public java.util.Map<String, Object> toDebugMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("temperature", temperature);
        map.put("humidity", humidity);
        map.put("soilMoisture", soilMoisture);
        map.put("lightLevel", lightLevel);
        map.put("skyLight", skyLight);
        map.put("blockLight", blockLight);
        map.put("outdoor", outdoor);
        map.put("outdoorScore", outdoorScore);
        map.put("altitudeY", altitudeY);
        map.put("nearWaterScore", nearWaterScore);
        map.put("greenhouseScore", greenhouseScore);
        map.put("inGreenhouse", inGreenhouse);
        if (biomeKey != null) {
            map.put("biomeKey", biomeKey.toString());
        } else if (biome != null) {
            map.put("biome", biome.toString());
        }
        if (biomeGroupId != null) {
            map.put("biomeGroupId", biomeGroupId);
        }
        return map;
    }

    public static final class Builder {
        private double temperature;
        private double humidity;
        private double soilMoisture;
        private int lightLevel;
        private int skyLight = -1;
        private int blockLight = -1;
        private boolean outdoor = false;
        private double outdoorScore = 0.0;
        private int altitudeY = -1;
        private double nearWaterScore = 0.0;
        private double greenhouseScore = 0.0;
        private boolean inGreenhouse = false;
        private Biome biome;
        private NamespacedKey biomeKey;
        private String biomeGroupId;

        public Builder temperature(double v) {
            this.temperature = v;
            return this;
        }

        public Builder humidity(double v) {
            this.humidity = v;
            return this;
        }

        public Builder soilMoisture(double v) {
            this.soilMoisture = v;
            return this;
        }

        public Builder lightLevel(int v) {
            this.lightLevel = v;
            return this;
        }

        public Builder skyLight(int v) {
            this.skyLight = v;
            return this;
        }

        public Builder blockLight(int v) {
            this.blockLight = v;
            return this;
        }

        public Builder outdoor(boolean v) {
            this.outdoor = v;
            return this;
        }

        public Builder outdoorScore(double v) {
            this.outdoorScore = v;
            return this;
        }

        public Builder altitudeY(int v) {
            this.altitudeY = v;
            return this;
        }

        public Builder nearWaterScore(double v) {
            this.nearWaterScore = v;
            return this;
        }

        public Builder greenhouseScore(double v) {
            this.greenhouseScore = v;
            return this;
        }

        public Builder inGreenhouse(boolean v) {
            this.inGreenhouse = v;
            return this;
        }

        public Builder biome(Biome v) {
            this.biome = v;
            return this;
        }

        public Builder biomeKey(NamespacedKey v) {
            this.biomeKey = v;
            return this;
        }

        public Builder biomeGroupId(String v) {
            this.biomeGroupId = v;
            return this;
        }

        public EnvironmentContext build() {
            return new EnvironmentContext(this);
        }
    }
}

