package com.github.cinnaio.natureEngine.core.agriculture.season.visual.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.engine.config.VisualConfigView;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolBiomeListener {

    private final JavaPlugin plugin;
    private final SeasonManager seasonManager;
    private final VisualConfigView visualConfig;
    private final ProtocolManager protocolManager;
    private final Map<UUID, NmsBiomeHolderResolver> resolverByWorld = new ConcurrentHashMap<>();
    private final Map<UUID, NmsBiomeIdResolver> idResolverByWorld = new ConcurrentHashMap<>();

    private volatile boolean started;
    private ChunkBiomePacketRewriter rewriter;

    public ProtocolBiomeListener(JavaPlugin plugin, SeasonManager seasonManager, VisualConfigView visualConfig) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.visualConfig = visualConfig;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        if (started) return;
        started = true;

        // 目前需求：只对 world 生效
        this.rewriter = new ChunkBiomePacketRewriter(
                plugin.getLogger(),
                "world",
                biome -> {
            World w = plugin.getServer().getWorld("world");
            if (w == null) return null;
            return resolverByWorld.computeIfAbsent(w.getUID(), k -> new NmsBiomeHolderResolver(w)).resolve(biome);
        },
                new ChunkBiomePacketRewriter.BiomeIdResolver() {
                    @Override
                    public Integer resolveRawId(Biome biome) {
                        World w = plugin.getServer().getWorld("world");
                        if (w == null) return null;
                        return idResolverByWorld.computeIfAbsent(w.getUID(), k -> new NmsBiomeIdResolver(w)).resolveRawId(biome);
                    }

                    @Override
                    public Integer resolveRawId(NamespacedKey biomeKey) {
                        World w = plugin.getServer().getWorld("world");
                        if (w == null) return null;
                        return idResolverByWorld.computeIfAbsent(w.getUID(), k -> new NmsBiomeIdResolver(w)).resolveRawId(biomeKey);
                    }

                    @Override
                    public String debugWhyNull(Biome biome) {
                        World w = plugin.getServer().getWorld("world");
                        if (w == null) return "world=null";
                        return idResolverByWorld.computeIfAbsent(w.getUID(), k -> new NmsBiomeIdResolver(w)).debugWhyNull(biome);
                    }
                }
        );

        PacketAdapter adapter = new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                chunkPacketTypes()
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!visualConfig.isEnabled()) return;
                World world = event.getPlayer().getWorld();
                SeasonType season = seasonManager.getCurrentSeason(world);
                Map<NamespacedKey, NamespacedKey> map = visualConfig.getBiomeKeyMap(season);
                NamespacedKey def = visualConfig.getDefaultBiomeKey(season);
                rewriter.rewriteIfApplicable(event, season, map, def, visualConfig.isDebug());
            }
        };

        protocolManager.addPacketListener(adapter);
    }

    private PacketType[] chunkPacketTypes() {
        // 尽量覆盖不同映射名（不同 fork 可能存在差异）
        // ProtocolLib 5.4.0 + 1.21.x 通常有 MAP_CHUNK；如果不存在也不抛错。
        return new PacketType[]{PacketType.Play.Server.MAP_CHUNK};
    }
}

