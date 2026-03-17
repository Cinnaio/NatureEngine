package com.github.cinnaio.natureEngine.core.agriculture.season.visual;

import com.github.cinnaio.natureEngine.core.agriculture.season.visual.protocol.ProtocolBiomeListener;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.engine.config.VisualConfigView;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.cinnaio.natureEngine.engine.text.Text;

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
    // 自动跟随：上次入队时的区块中心 (chunkX, chunkZ)，用于检测移动
    private final Map<UUID, int[]> lastEnqueuedChunk = new HashMap<>();

    public PacketSeasonVisualizer(JavaPlugin plugin, SeasonManager seasonManager, VisualConfigView configView) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.configView = configView;
        this.protocolBiomeListener = new ProtocolBiomeListener(plugin, seasonManager, configView);
        this.protocolBiomeListener.start();
    }

    public void enqueueApply(Player player) {
        if (!isTargetWorld(player.getWorld())) {
            player.sendMessage(Text.parse("&c该命令仅对 world 生效。"));
            return;
        }
        enqueueChunks(player, true);
        player.sendMessage(Text.parse("&a已开始刷新你周围区域的季节视觉。"));
    }

    /** 仅入队不发消息，供自动跟随使用。 */
    void enqueueChunksSilent(Player player) {
        if (!isTargetWorld(player.getWorld())) return;
        enqueueChunks(player, false);
    }

    private void enqueueChunks(Player player, boolean sendMessage) {
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
        lastEnqueuedChunk.put(player.getUniqueId(), new int[]{centerChunkX, centerChunkZ});
    }

    /**
     * 每 tick 调用：处理刷新队列，每玩家每 tick 最多处理 chunksPerTickPerPlayer 个 chunk。
     */
    public void tick() {
        if (!configView.isEnabled()) {
            pending.clear();
            return;
        }
        if (pending.isEmpty()) return;
        int perPlayer = configView.getChunksPerTickPerPlayer();
        for (var entry : pending.entrySet()) {
            UUID playerId = entry.getKey();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || !isTargetWorld(player.getWorld())) continue;
            Deque<Long> q = entry.getValue();
            World world = player.getWorld();
            for (int i = 0; i < perPlayer; i++) {
                Long packed = q.pollFirst();
                if (packed == null) break;
                refreshChunk(world, unpackChunkX(packed), unpackChunkZ(packed));
            }
        }
        pending.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * 自动跟随：每隔一段时间检测在线玩家是否移动，移动则入队刷新周围区块（不发消息）。
     */
    public void tickAutoFollow() {
        if (!configView.isEnabled() || !configView.isAutoFollowEnabled()) return;
        int minMove = configView.getAutoFollowMinMoveChunks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isTargetWorld(player.getWorld())) continue;
            int cx = player.getLocation().getBlockX() >> 4;
            int cz = player.getLocation().getBlockZ() >> 4;
            int[] last = lastEnqueuedChunk.get(player.getUniqueId());
            if (last == null) {
                enqueueChunksSilent(player);
                continue;
            }
            int dx = Math.abs(cx - last[0]);
            int dz = Math.abs(cz - last[1]);
            if (dx >= minMove || dz >= minMove) enqueueChunksSilent(player);
        }
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

