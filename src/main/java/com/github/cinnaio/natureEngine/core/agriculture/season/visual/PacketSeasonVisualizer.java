package com.github.cinnaio.natureEngine.core.agriculture.season.visual;

import com.github.cinnaio.natureEngine.core.agriculture.season.visual.protocol.ProtocolBiomeListener;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.engine.config.VisualConfigView;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 视觉季节（发包主导）占位实现：
 * 由于 1.21.4 的 Chunk Biome 发包结构较复杂，这里采用“临时改 biome -> refreshChunk -> 立即恢复”的方式
 * 来达到“视觉刷新”的效果，并确保不留下持久世界改动。
 *
 * 注意：该方式会向所有玩家刷新该 chunk（world.refreshChunk 的行为），适合作为过渡实现。
 */
public final class PacketSeasonVisualizer {

    private final JavaPlugin plugin;
    private final SeasonManager seasonManager;
    private final VisualConfigView configView;
    private final ProtocolBiomeListener protocolBiomeListener;

    // playerId -> chunk queue (chunkX, chunkZ packed)
    private final Map<UUID, Deque<Long>> pending = new HashMap<>();

    public PacketSeasonVisualizer(JavaPlugin plugin, SeasonManager seasonManager, VisualConfigView configView) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.configView = configView;
        this.protocolBiomeListener = new ProtocolBiomeListener(plugin, seasonManager, configView);
        this.protocolBiomeListener.start();
    }

    public void enqueueApply(Player player) {
        if (!isTargetWorld(player.getWorld())) {
            player.sendMessage("§c该命令仅对 world 生效。");
            return;
        }
        SeasonType season = seasonManager.getCurrentSeason(player.getWorld());
        enqueueChunks(player, season);
        player.sendMessage("§a已开始刷新你周围区域的季节视觉（20 chunk/秒）。");
    }

    private void enqueueChunks(Player player, SeasonType season) {
        int radius = configView.getRadiusChunks();
        int centerChunkX = player.getLocation().getBlockX() >> 4;
        int centerChunkZ = player.getLocation().getBlockZ() >> 4;
        Deque<Long> q = pending.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        q.clear();
        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            for (int cz = centerChunkZ - radius; cz <= centerChunkZ + radius; cz++) {
                q.add(packChunk(cx, cz));
            }
        }
    }

    /**
     * 每 tick 调用一次，按照 20 chunk/秒（≈每 tick 1 chunk）刷新队列。
     */
    public void tick() {
        if (!configView.isEnabled()) {
            pending.clear();
            return;
        }
        if (pending.isEmpty()) {
            return;
        }
        // 每 tick 为每个玩家最多处理 1 个 chunk，避免瞬间刷新过多
        for (var entry : pending.entrySet()) {
            UUID playerId = entry.getKey();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (!isTargetWorld(player.getWorld())) {
                continue;
            }
            Deque<Long> q = entry.getValue();
            Long packed = q.pollFirst();
            if (packed == null) {
                continue;
            }
            int chunkX = unpackChunkX(packed);
            int chunkZ = unpackChunkZ(packed);
            refreshChunk(player.getWorld(), chunkX, chunkZ);
        }
        pending.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    private void refreshChunk(World world, int chunkX, int chunkZ) {
        // 纯发包模式：这里只负责触发客户端重新接收 chunk 包；
        // biome 替换由 ProtocolLib 监听器在发包时完成。
        Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, () -> world.refreshChunk(chunkX, chunkZ));
    }

    private boolean isTargetWorld(World world) {
        return "world".equals(world.getName());
    }

    private long packChunk(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private int unpackChunkX(long packed) {
        return (int) (packed >> 32);
    }

    private int unpackChunkZ(long packed) {
        return (int) packed;
    }
}

