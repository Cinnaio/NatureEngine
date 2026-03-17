package com.github.cinnaio.natureEngine.integration.craftengine;

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
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CraftEngine 自定义植物接管：
 * - 监听自定义方块放置/破坏，追踪属于 crops.yml 的 craftengine 作物
 * - 以定时器模拟生长尝试，并使用 NatureEngine 的 GrowthCalculator 计算推进/枯萎
 */
public final class CraftEngineCropController implements Listener, CraftEngineTrackService {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final GlobalScheduler scheduler;
    private final SeasonManager seasonManager;
    private final WeatherManager weatherManager;
    private final EnvironmentManager environmentManager;
    private final CropManager cropManager;

    // 追踪的作物方块位置（按世界分组）
    private final Map<UUID, Set<BlockKey>> tracked = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public CraftEngineCropController(
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
        // 每秒扫描一小批 tracked 位置，进行一次“生长尝试”
        scheduler.runTaskTimer(this::tick, 20L, 20L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 手动把某个方块加入追踪（用于 debug/迁移：已存在于世界里的自定义作物也能被接管）。
     */
    public void track(Block block) {
        if (block == null) return;
        tracked.computeIfAbsent(block.getWorld().getUID(), k -> ConcurrentHashMap.newKeySet())
                .add(BlockKey.of(block));
    }

    @EventHandler
    public void onCustomBlockPlace(CustomBlockPlaceEvent event) {
        Block block = event.bukkitBlock();
        String id = event.customBlock().id().asString();
        if (configManager.getCropConfig().getCraftEngineType(id).isEmpty()) return;
        tracked.computeIfAbsent(block.getWorld().getUID(), k -> ConcurrentHashMap.newKeySet())
                .add(BlockKey.of(block));
    }

    @EventHandler
    public void onCustomBlockBreak(CustomBlockBreakEvent event) {
        Block block = event.bukkitBlock();
        Set<BlockKey> set = tracked.get(block.getWorld().getUID());
        if (set != null) {
            set.remove(BlockKey.of(block));
        }
    }

    private void tick() {
        if (!configManager.getCropConfig().isGlobalEnabled()) return;
        for (World world : Bukkit.getWorlds()) {
            Set<BlockKey> set = tracked.get(world.getUID());
            if (set == null || set.isEmpty()) continue;

            // 注意：CraftEngineBlocks.getCustomBlockState 会触发 NMS 读方块状态，
            // 在 Folia/Luminol 上必须在对应 Region 线程执行，否则可能抛 NPE/线程违规。
            int budget = Math.min(80, set.size());
            List<BlockKey> batch = new ArrayList<>(budget);
            Iterator<BlockKey> it = set.iterator();
            while (it.hasNext() && batch.size() < budget) {
                batch.add(it.next());
            }

            for (BlockKey key : batch) {
                // Folia/Luminol：这里需要传入 chunk 坐标（chunkX/chunkZ），不是方块坐标
                Bukkit.getRegionScheduler().execute(plugin, world, key.x >> 4, key.z >> 4, () -> tickOne(world, key));
            }
        }
    }

    private void tickOne(World world, BlockKey key) {
        Set<BlockKey> set = tracked.get(world.getUID());
        if (set == null) return;

        Block block = world.getBlockAt(key.x, key.y, key.z);
        String id;
        try {
            id = getCustomId(block);
        } catch (Throwable t) {
            // 读 NMS/区块状态异常：移除避免刷屏
            set.remove(key);
            return;
        }
        if (id == null) {
            set.remove(key);
            return;
        }

        Optional<CropType> cropOpt = configManager.getCropConfig().getCraftEngineType(id);
        if (cropOpt.isEmpty()) {
            set.remove(key);
            return;
        }

        CropType crop = cropOpt.get();
        // 概率控制：根据 baseTicksPerStage 粗略换算每秒尝试概率
        double p = Math.min(1.0, 20.0 / Math.max(20.0, crop.getBaseTicksPerStage()));
        // 插件内部 randomTickSpeed：与原版默认 3 对齐（3 表示不缩放）
        int pluginRts = configManager.getGrowthConfig().getRandomTickSpeed();
        double scale = pluginRts <= 0 ? 0.0 : (double) pluginRts / 3.0;
        p = Math.min(1.0, p * scale);
        if (random.nextDouble() > p) return;

        try {
            attemptGrowCraftEngineCrop(block, id, crop);
        } catch (Throwable t) {
            // 任何异常都不应中断调度，且避免反复刷屏
            set.remove(key);
        }
    }

    private void attemptGrowCraftEngineCrop(Block block, String customId, CropType crop) {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null || state.isEmpty()) return;

        Property<?> ageProp = resolveAgeProperty(customId, state);
        if (ageProp == null) return;

        Integer currentAge = extractIntProperty(state, ageProp);
        if (currentAge == null) return;

        int maxAge = maxIntProperty(ageProp);
        if (currentAge >= maxAge) return;

        Location loc = block.getLocation();
        EnvironmentContext env = environmentManager.getContext(block);
        GrowthContext ctx = new GrowthContext(
                loc,
                crop,
                currentAge,
                seasonManager.getCurrentSeason(block.getWorld()),
                seasonManager.getSeasonProgress(block.getWorld()),
                weatherManager.getCurrentWeather(block.getWorld()),
                env
        );

        GrowthResult result = cropManager.calculateGrowth(ctx);
        if (result.isShouldWither()) {
            // 枯萎：将 age 设为最小值（通常是 0）
            Object min = minPropertyValue(ageProp);
            ImmutableBlockState newState = state.with((Property) ageProp, (Comparable) min);
            CraftEngineBlocks.place(loc, newState, UpdateOption.UPDATE_ALL, false);
            return;
        }
        if (result.getStageDelta() > 0) {
            Object next = nextPropertyValue(ageProp, currentAge);
            if (next == null) return;
            ImmutableBlockState newState = state.with((Property) ageProp, (Comparable) next);
            CraftEngineBlocks.place(loc, newState, UpdateOption.UPDATE_ALL, false);
        }
    }

    private Property<?> resolveAgeProperty(String customId, ImmutableBlockState state) {
        Optional<String> configured = configManager.getCropConfig().getCraftEngineAgeProperty(customId);
        if (configured.isPresent()) {
            Property<?> p = findPropertyByName(state, configured.get());
            if (p != null) return p;
        }
        // 自动探测常见名称
        for (String name : new String[]{"age", "stage", "growth"}) {
            Property<?> p = findPropertyByName(state, name);
            if (p != null) return p;
        }
        return null;
    }

    private static Property<?> findPropertyByName(ImmutableBlockState state, String name) {
        for (Property<?> p : state.getProperties()) {
            if (p == null) continue;
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    private static Integer extractIntProperty(ImmutableBlockState state, Property<?> property) {
        try {
            Object v = state.getNullable((Property) property);
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int maxIntProperty(Property<?> property) {
        try {
            List<?> values = property.possibleValues();
            int max = Integer.MIN_VALUE;
            for (Object o : values) {
                if (o instanceof Number n) max = Math.max(max, n.intValue());
            }
            return max == Integer.MIN_VALUE ? 0 : max;
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static Object minPropertyValue(Property<?> property) {
        List<?> values = property.possibleValues();
        Object min = null;
        int minVal = Integer.MAX_VALUE;
        for (Object o : values) {
            if (o instanceof Number n) {
                int v = n.intValue();
                if (v < minVal) {
                    minVal = v;
                    min = o;
                }
            }
        }
        return min != null ? min : (values.isEmpty() ? 0 : values.get(0));
    }

    private static Object nextPropertyValue(Property<?> property, int current) {
        List<?> values = property.possibleValues();
        Object best = null;
        int bestVal = Integer.MAX_VALUE;
        for (Object o : values) {
            if (o instanceof Number n) {
                int v = n.intValue();
                if (v > current && v < bestVal) {
                    bestVal = v;
                    best = o;
                }
            }
        }
        return best;
    }


    private static String getCustomId(Block block) {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null || state.isEmpty()) return null;
        CustomBlock owner = (CustomBlock) state.owner().value();
        if (owner == null) return null;
        return owner.id().asString();
    }

    private record BlockKey(int x, int y, int z) {
        static BlockKey of(Block b) {
            return new BlockKey(b.getX(), b.getY(), b.getZ());
        }
    }
}

