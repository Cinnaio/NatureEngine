package com.github.cinnaio.natureEngine.core.agriculture.crop.listener;

import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropData;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropManager;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthContext;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthResult;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.Optional;

/**
 * 接管原版作物生长事件，并委托到统一的 GrowthCalculator。
 */
public final class VanillaCropListener implements Listener {

    private final ServiceLocator services = ServiceLocator.getInstance();

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        handleGrowth(event.getBlock(), true);
    }

    @EventHandler
    public void onBlockFertilize(BlockFertilizeEvent event) {
        Block block = event.getBlock();
        handleGrowth(block, false);
    }

    private void handleGrowth(Block block, boolean cancelIfNoAdvance) {
        if (!(block.getBlockData() instanceof Ageable)) {
            return;
        }
        Location loc = block.getLocation();
        CropManager cropManager = services.get(CropManager.class);
        SeasonManager seasonManager = services.get(SeasonManager.class);
        WeatherManager weatherManager = services.get(WeatherManager.class);
        EnvironmentManager environmentManager = services.get(EnvironmentManager.class);

        Optional<CropData> cropDataOpt = cropManager.getCropDataForLocation(loc);
        if (!cropDataOpt.isPresent()) {
            return;
        }

        Ageable ageable = (Ageable) block.getBlockData();
        int currentAge = ageable.getAge();

        EnvironmentContext envContext = environmentManager.getContext(block);

        GrowthContext context = new GrowthContext(
                loc,
                cropDataOpt.get(),
                currentAge,
                seasonManager.getCurrentSeason(block.getWorld()),
                seasonManager.getSeasonProgress(block.getWorld()),
                weatherManager.getCurrentWeather(block.getWorld()),
                envContext
        );

        GrowthResult result = cropManager.calculateGrowth(context);

        if (result.isShouldWither()) {
            // 简化：直接将年龄设为 0 表示枯萎，后续可换成特定方块
            ageable.setAge(0);
            block.setBlockData(ageable, false);
            return;
        }

        if (result.getStageDelta() > 0) {
            int newAge = Math.min(ageable.getMaximumAge(), currentAge + result.getStageDelta());
            ageable.setAge(newAge);
            block.setBlockData(ageable, false);
        } else if (cancelIfNoAdvance) {
            // 没有推进阶段，阻止原版自己的生长
            block.getState().update(false, false);
        }
    }
}

