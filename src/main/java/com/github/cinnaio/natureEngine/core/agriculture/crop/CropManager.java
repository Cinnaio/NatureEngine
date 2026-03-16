package com.github.cinnaio.natureEngine.core.agriculture.crop;

import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthCalculator;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthContext;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthResult;
import org.bukkit.Location;

import java.util.Optional;

/**
 * 作物相关的高层操作封装。
 */
public final class CropManager {

    private final CropRegistry registry;
    private final GrowthCalculator growthCalculator;

    public CropManager(CropRegistry registry, GrowthCalculator growthCalculator) {
        this.registry = registry;
        this.growthCalculator = growthCalculator;
    }

    public Optional<CropData> getCropDataForLocation(Location location) {
        if (location.getBlock() == null) {
            return Optional.empty();
        }
        return registry.getForMaterial(location.getBlock().getType());
    }

    public GrowthResult calculateGrowth(GrowthContext context) {
        return growthCalculator.calculate(context);
    }
}

