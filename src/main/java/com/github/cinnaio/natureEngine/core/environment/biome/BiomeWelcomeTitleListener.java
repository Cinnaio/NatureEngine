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
        Bukkit.getScheduler().runTask(plugin, () -> checkAndShow(player, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        // 传送后下一 tick 再判定
        Bukkit.getScheduler().runTask(plugin, () -> checkAndShow(player, true));
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
        String groupId = config.resolveGroupId(biome);
        if (groupId == null) {
            // 未命中 group：用 biome key 作为“临时 group”，避免同 biome 内反复触发
            groupId = config.getTitle(biome).isPresent() ? ("biome:" + config.getTitle(biome).get()) : "biome:" + safeBiomeKey(biome);
        }
        if (!force) {
            String prev = lastGroup.get(player.getUniqueId());
            if (groupId.equals(prev)) return;
        }

        long now = System.currentTimeMillis();
        long next = nextAllowedAtMs.getOrDefault(player.getUniqueId(), 0L);
        if (!force && now < next) {
            lastGroup.put(player.getUniqueId(), groupId);
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

        lastGroup.put(player.getUniqueId(), groupId);
        nextAllowedAtMs.put(player.getUniqueId(), now + config.getCooldownMillis());
    }

    private static String safeBiomeKey(Biome biome) {
        try {
            var k = biome.getKey();
            return k != null ? k.toString() : String.valueOf(biome);
        } catch (Throwable ignored) {
            return String.valueOf(biome);
        }
    }
}

