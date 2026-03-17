package com.github.cinnaio.natureEngine.core.agriculture.crop;

import com.github.cinnaio.natureEngine.engine.config.CropConfigView;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

/**
 * 注册原版作物与（以后）CraftEngine 植物的基础数据。
 * 后续将扩展为从配置加载，并支持多种 CropType 实现。
 */
public final class CropRegistry {

    private final Map<Material, CropType> vanillaCrops = new EnumMap<>(Material.class);
    private final CropConfigView configView;

    public CropRegistry(CropConfigView configView) {
        this.configView = configView;
        registerDefaults();
    }

    private void registerDefaults() {
        // 先从配置加载 vanilla 定义
        for (Material material : Material.values()) {
            configView.getVanillaType(material).ifPresent(type -> vanillaCrops.put(material, type));
        }

        // 如配置缺失，可在此保留少量代码级默认值（例如 wheat），避免完全不可用
        if (!vanillaCrops.containsKey(Material.WHEAT)) {
            vanillaCrops.put(Material.WHEAT, new CropData(
                    "minecraft:wheat",
                    8,
                    2400L,
                    18.0,
                    10.0,
                    0.6,
                    0.3,
                    9,
                    EnumSet.of(com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType.SPRING,
                            com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType.SUMMER),
                    true
            ));
        }
    }

    public Optional<CropType> getForMaterial(Material material) {
        if (!configView.isGlobalEnabled()) {
            return Optional.empty();
        }
        CropType type = vanillaCrops.get(material);
        if (type == null || !type.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(type);
    }
}


