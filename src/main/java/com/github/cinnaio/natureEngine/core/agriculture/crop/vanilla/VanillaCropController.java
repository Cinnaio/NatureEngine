package com.github.cinnaio.natureEngine.core.agriculture.crop.vanilla;

import com.github.cinnaio.natureEngine.core.agriculture.crop.CropManager;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropType;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthContext;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthResult;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentManager;
import com.github.cinnaio.natureEngine.engine.config.ConfigManager;
import com.github.cinnaio.natureEngine.engine.scheduler.GlobalScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原版作物接管的“主动 tick”控制器：
 * - 不依赖世界 randomTickSpeed 触发频率，按插件内部 randomTickSpeed 驱动作物尝试推进
 * - 通过 RegionScheduler 在正确线程读写方块，兼容 Folia/Luminol
 */
public final class VanillaCropController implements Listener {

    private static final int DEFAULT_PLUGIN_RTS = 3;
    private static final int MAX_BUDGET_PER_WORLD = 160;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final GlobalScheduler scheduler;
    private final SeasonManager seasonManager;
    private final WeatherManager weatherManager;
    private final EnvironmentManager environmentManager;
    private final CropManager cropManager;
    private final Random random = new Random();

    private final Map<UUID, Set<BlockKey>> tracked = new ConcurrentHashMap<>();

    public VanillaCropController(
            JavaPlugin plugin,
            ConfigManager configManager,
            GlobalScheduler scheduler,
            SeasonManager seasonManager,
            WeatherManager weatherManager,
            EnvironmentManager environmentManager,
            CropManager cropManager
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.seasonManager = seasonManager;
        this.weatherManager = weatherManager;
        this.environmentManager = environmentManager;
        this.cropManager = cropManager;
    }

    public void start() {
        scheduler.runTaskTimer(this::tick, 20L, 20L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void track(Block block) {
        if (block == null) return;
        tracked.computeIfAbsent(block.getWorld().getUID(), k -> ConcurrentHashMap.newKeySet())
                .add(BlockKey.of(block));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!(block.getBlockData() instanceof Ageable)) return;
        // 只追踪在 crops.yml 注册过的作物
        if (cropManager.getCropDataForLocation(block.getLocation()).isEmpty()) return;
        track(block);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Set<BlockKey> set = tracked.get(block.getWorld().getUID());
        if (set != null) {
            set.remove(BlockKey.of(block));
        }
    }

    private void tick() {
        if (configManager == null || configManager.getCropConfig() == null) return;
        if (!configManager.getCropConfig().isGlobalEnabled()) return;

        int pluginRts = configManager.getGrowthConfig().getRandomTickSpeed();
        if (pluginRts <= 0) return;
        double rtsScale = (double) pluginRts / (double) DEFAULT_PLUGIN_RTS;

        for (World world : Bukkit.getWorlds()) {
            Set<BlockKey> set = tracked.get(world.getUID());
            if (set == null || set.isEmpty()) continue;

            int budget = Math.min(MAX_BUDGET_PER_WORLD, set.size());
            List<BlockKey> batch = new ArrayList<>(budget);
            Iterator<BlockKey> it = set.iterator();
            while (it.hasNext() && batch.size() < budget) {
                batch.add(it.next());
            }

            for (BlockKey key : batch) {
                Bukkit.getRegionScheduler().execute(plugin, world, key.x >> 4, key.z >> 4, () -> tickOne(world, key, rtsScale));
            }
        }
    }

    private void tickOne(World world, BlockKey key, double rtsScale) {
        Set<BlockKey> set = tracked.get(world.getUID());
        if (set == null) return;

        Block block = world.getBlockAt(key.x, key.y, key.z);
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            set.remove(key);
            return;
        }

        Location loc = block.getLocation();
        Optional<CropType> cropOpt = cropManager.getCropDataForLocation(loc);
        if (cropOpt.isEmpty()) {
            set.remove(key);
            return;
        }
        CropType crop = cropOpt.get();

        int currentAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        if (currentAge >= maxAge) return;

        // 按 baseTicksPerStage 计算每秒尝试概率，并受插件 rts 缩放
        double p = Math.min(1.0, 20.0 / Math.max(20.0, crop.getBaseTicksPerStage()));
        p = Math.min(1.0, p * Math.max(0.0, rtsScale));
        if (random.nextDouble() > p) return;

        EnvironmentContext env = environmentManager.getContext(block);
        GrowthContext ctx = new GrowthContext(
                loc,
                crop,
                currentAge,
                seasonManager.getCurrentSeason(world),
                seasonManager.getSeasonProgress(world),
                weatherManager.getCurrentWeather(world),
                env
        );

        GrowthResult result = cropManager.calculateGrowth(ctx);
        if (result.isShouldWither()) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
            return;
        }
        if (result.getStageDelta() > 0) {
            int newAge = Math.min(maxAge, currentAge + result.getStageDelta());
            ageable.setAge(newAge);
            block.setBlockData(ageable, false);
        }
    }

    private record BlockKey(int x, int y, int z) {
        static BlockKey of(Block b) {
            return new BlockKey(b.getX(), b.getY(), b.getZ());
        }
    }
}

