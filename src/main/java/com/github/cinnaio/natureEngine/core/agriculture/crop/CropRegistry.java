package com.github.cinnaio.natureEngine.core.agriculture.crop;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

/**
 * 注册原版作物与（以后）CraftEngine 植物的基础数据。
 */
public final class CropRegistry {

    private final Map<Material, CropData> vanillaCrops = new EnumMap<>(Material.class);

    public CropRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        // 简单示例：小麦
        vanillaCrops.put(
                Material.WHEAT,
                new CropData(
                        "minecraft:wheat",
                        8,
                        2400L,
                        18.0,
                        10.0,
                        0.6,
                        0.3,
                        9,
                        EnumSet.of(SeasonType.SPRING, SeasonType.SUMMER)
                )
        );
        // 其他作物可按需继续补充或迁移到配置
    }

    public Optional<CropData> getForMaterial(Material material) {
        return Optional.ofNullable(vanillaCrops.get(material));
    }
}

