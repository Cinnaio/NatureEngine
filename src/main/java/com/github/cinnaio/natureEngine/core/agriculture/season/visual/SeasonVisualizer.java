package com.github.cinnaio.natureEngine.core.agriculture.season.visual;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.engine.config.VisualConfigView;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;

/**
 * 首版视觉系统：通过渐进式修改 biome 来驱动草地/树叶 tint 变化。
 * 仅影响在线玩家附近一定半径区域，支持恢复。
 */
public final class SeasonVisualizer {

    private final JavaPlugin plugin;
    private final VisualConfigView configView;

    private final Map<UUID, Map<Long, Biome>> originalBiomeByWorld = new ConcurrentHashMap<>();
    private final Deque<BiomeUpdate> queue = new ArrayDeque<>();

    public SeasonVisualizer(JavaPlugin plugin, VisualConfigView configView) {
        this.plugin = plugin;
        this.configView = configView;
    }

    public void enqueueApplyAround(Player player, SeasonType seasonType) {
        if (!configView.isEnabled()) {
            return;
        }
        World world = player.getWorld();
        int radiusChunks = configView.getRadiusChunks();
        Map<Biome, Biome> biomeMap = configView.getBiomeMap(seasonType);

        int centerChunkX = player.getLocation().getBlockX() >> 4;
        int centerChunkZ = player.getLocation().getBlockZ() >> 4;

        for (int cx = centerChunkX - radiusChunks; cx <= centerChunkX + radiusChunks; cx++) {
            for (int cz = centerChunkZ - radiusChunks; cz <= centerChunkZ + radiusChunks; cz++) {
                queue.add(new BiomeUpdate(world.getUID(), cx, cz, biomeMap, BiomeUpdateMode.APPLY));
            }
        }
    }

    public void enqueueRestoreAround(Player player) {
        World world = player.getWorld();
        int radiusChunks = configView.getRadiusChunks();

        int centerChunkX = player.getLocation().getBlockX() >> 4;
        int centerChunkZ = player.getLocation().getBlockZ() >> 4;

        for (int cx = centerChunkX - radiusChunks; cx <= centerChunkX + radiusChunks; cx++) {
            for (int cz = centerChunkZ - radiusChunks; cz <= centerChunkZ + radiusChunks; cz++) {
                queue.add(new BiomeUpdate(world.getUID(), cx, cz, null, BiomeUpdateMode.RESTORE));
            }
        }
    }

    /**
     * 在 tick loop 中调用，渐进式处理 biome 更新，避免卡服。
     */
    public void tick() {
        if (!configView.isEnabled()) {
            queue.clear();
            return;
        }
        int maxColumnsPerTick = configView.getMaxColumnsPerTick();
        int maxChunksPerTick = Math.max(1, maxColumnsPerTick / 256);
        for (int i = 0; i < maxChunksPerTick && !queue.isEmpty(); i++) {
            BiomeUpdate update = queue.poll();
            if (update != null) {
                scheduleChunkUpdate(update);
            }
        }
    }

    private void scheduleChunkUpdate(BiomeUpdate update) {
        World world = Bukkit.getWorld(update.worldId);
        if (world == null) {
            return;
        }
        // Folia 要求在对应区域线程修改 chunk/biome
        // 注意：该重载需要 chunk 坐标，而不是 block 坐标
        Bukkit.getRegionScheduler().execute(plugin, world, update.chunkX, update.chunkZ, () -> applyChunkUpdate(world, update));
    }

    private void applyChunkUpdate(World world, BiomeUpdate update) {
        Map<Long, Biome> store = originalBiomeByWorld.computeIfAbsent(update.worldId, k -> new ConcurrentHashMap<>());
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = (update.chunkX << 4) + localX;
                int z = (update.chunkZ << 4) + localZ;
                long key = packXZ(x, z);

                if (update.mode == BiomeUpdateMode.APPLY) {
                    Biome current = world.getBiome(x, world.getMinHeight(), z);
                    store.putIfAbsent(key, current);
                    Biome target = update.biomeMap != null ? update.biomeMap.getOrDefault(current, current) : current;
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 16) {
                        world.setBiome(x, y, z, target);
                    }
                } else {
                    Biome original = store.get(key);
                    if (original != null) {
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 16) {
                            world.setBiome(x, y, z, original);
                        }
                    }
                }
            }
        }
    }

    private long packXZ(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private enum BiomeUpdateMode {
        APPLY,
        RESTORE
    }

    private static final class BiomeUpdate {
        private final UUID worldId;
        private final int chunkX;
        private final int chunkZ;
        private final Map<Biome, Biome> biomeMap;
        private final BiomeUpdateMode mode;

        private BiomeUpdate(UUID worldId, int chunkX, int chunkZ, Map<Biome, Biome> biomeMap, BiomeUpdateMode mode) {
            this.worldId = worldId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.biomeMap = biomeMap;
            this.mode = mode;
        }
    }
}

