package com.github.cinnaio.natureEngine.core.environment.biome;

import com.github.cinnaio.natureEngine.engine.config.BiomeTitleConfigView;
import com.github.cinnaio.natureEngine.engine.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Biome 变化的地域欢迎 Title（RPG 风格）。
 */
public final class BiomeWelcomeTitleListener implements Listener {

    private final Plugin plugin;
    private final BiomeTitleConfigView config;

    private final Map<UUID, String> lastGroup = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextAllowedAtMs = new ConcurrentHashMap<>();

    public BiomeWelcomeTitleListener(Plugin plugin, BiomeTitleConfigView config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 进服延迟 1 tick，避免部分情况下位置/区块未就绪
        runNextTick(player, () -> checkAndShow(player, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        // 传送后下一 tick 再判定
        runNextTick(player, () -> checkAndShow(player, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        // 只在 X/Z 方块坐标变化时才检查，避免转头刷屏
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        checkAndShow(event.getPlayer(), false);
    }

    private void checkAndShow(Player player, boolean force) {
        if (player == null || !player.isOnline()) return;
        if (config == null || !config.isEnabled()) return;
        World world = player.getWorld();
        if (!config.isWorldEnabled(world)) return;

        Biome biome = player.getLocation().getBlock().getBiome();
        String biomeKey = safeBiomeKey(biome).toLowerCase(java.util.Locale.ROOT);
        String groupId = config.resolveGroupId(biome);
        if (groupId == null) groupId = "biome:" + biomeKey;

        UUID id = player.getUniqueId();
        boolean first = !lastGroup.containsKey(id);
        String prev = lastGroup.get(id);
        if (!first && groupId.equals(prev)) return;

        long now = System.currentTimeMillis();
        long next = nextAllowedAtMs.getOrDefault(id, 0L);
        // force 也尊重冷却（除非玩家第一次进入没有历史记录）
        if (!first && now < next) {
            lastGroup.put(id, groupId);
            return;
        }

        String rawTitle = config.getGroupTitle(biome)
                .or(() -> config.getTitle(biome))
                .orElseGet(() -> safeBiomeKey(biome));
        Component title = Text.parse(rawTitle);
        Title.Times times = Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1400), Duration.ofMillis(300));
        Title t = Title.title(title, Component.empty(), times);

        // Folia/Luminol：使用 RegionScheduler 在玩家所在区域线程发送
        int chunkX = player.getLocation().getBlockX() >> 4;
        int chunkZ = player.getLocation().getBlockZ() >> 4;
        try {
            Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, () -> player.showTitle(t));
        } catch (Throwable ignored) {
            player.showTitle(t);
        }

        lastGroup.put(id, groupId);
        nextAllowedAtMs.put(id, now + config.getCooldownMillis());
    }

    private static String safeBiomeKey(Biome biome) {
        try {
            var k = biome.getKey();
            return k != null ? k.toString() : String.valueOf(biome);
        } catch (Throwable ignored) {
            return String.valueOf(biome);
        }
    }

    /**
     * Folia/Luminol：不能使用 BukkitScheduler（会抛 UnsupportedOperationException）。
     * 这里直接使用 RegionScheduler，把任务派发到玩家所在区块的 Region 线程执行。
     */
    private void runNextTick(Player player, Runnable task) {
        if (player == null || task == null) return;
        World world = player.getWorld();
        int chunkX = player.getLocation().getBlockX() >> 4;
        int chunkZ = player.getLocation().getBlockZ() >> 4;
        try {
            Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task);
        } catch (Throwable ignored) {
            // 非 Folia 环境兜底：直接运行（join/teleport 事件线程）
            task.run();
        }
    }
}

