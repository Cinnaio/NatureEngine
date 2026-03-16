package com.github.cinnaio.natureEngine.core.agriculture.season.visual;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.engine.config.VisualConfigView;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 首版视觉系统：通过渐进式修改 biome 来驱动草地/树叶 tint 变化。
 * 仅影响在线玩家附近一定半径区域，支持恢复。
 */
public final class SeasonVisualizer {

    private final VisualConfigView configView;

    private final Map<UUID, Map<Long, Biome>> originalBiomeByWorld = new HashMap<>();
    private final Deque<BiomeUpdate> queue = new ArrayDeque<>();

    public SeasonVisualizer(VisualConfigView configView) {
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
                queue.add(new BiomeUpdate(world.getUID(), cx, cz, seasonType, biomeMap, BiomeUpdateMode.APPLY));
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
                queue.add(new BiomeUpdate(world.getUID(), cx, cz, null, null, BiomeUpdateMode.RESTORE));
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
        int budget = configView.getMaxColumnsPerTick();
        while (budget > 0 && !queue.isEmpty()) {
            BiomeUpdate update = queue.peek();
            int used = updateStep(update, budget);
            budget -= used;
            if (update.isDone()) {
                queue.poll();
            } else {
                break;
            }
        }
    }

    private int updateStep(BiomeUpdate update, int budget) {
        World world = Bukkit.getWorld(update.worldId);
        if (world == null) {
            update.done = true;
            return 0;
        }

        int used = 0;
        // 每个 chunk 16x16 列，逐列处理
        while (used < budget && update.index < 256) {
            int localX = update.index & 15;
            int localZ = (update.index >> 4) & 15;
            int x = (update.chunkX << 4) + localX;
            int z = (update.chunkZ << 4) + localZ;

            long key = packXZ(x, z);
            Map<Long, Biome> store = originalBiomeByWorld.computeIfAbsent(update.worldId, k -> new HashMap<>());

            if (update.mode == BiomeUpdateMode.APPLY) {
                Biome current = world.getBiome(x, world.getMinHeight(), z);
                store.putIfAbsent(key, current);
                Biome target = update.biomeMap != null ? update.biomeMap.getOrDefault(current, current) : current;
                // 设置整列（从 minHeight 到 maxHeight），保证草/叶都吃到相同 biome tint
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

            update.index++;
            used++;
        }

        if (update.index >= 256) {
            update.done = true;
        }

        return used;
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
        private final SeasonType seasonType;
        private final Map<Biome, Biome> biomeMap;
        private final BiomeUpdateMode mode;

        private int index = 0;
        private boolean done = false;

        private BiomeUpdate(UUID worldId, int chunkX, int chunkZ, SeasonType seasonType, Map<Biome, Biome> biomeMap, BiomeUpdateMode mode) {
            this.worldId = worldId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.seasonType = seasonType;
            this.biomeMap = biomeMap;
            this.mode = mode;
        }

        public boolean isDone() {
            return done;
        }
    }
}

