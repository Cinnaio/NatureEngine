package com.github.cinnaio.natureEngine.core.agriculture.season.visual.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 纯发包 biome tint：拦截区块包并尽可能只替换 biome palette 的 entries。
 *
 * 说明：
 * - 1.21.4 的 chunk biome 存储是 paletted container（调色板 + 位图索引）。最安全策略是只改调色板 entries。
 * - 由于 Luminol/Paper 的 NMS 结构可能变化，这里使用反射 + 容错方式定位 PalettedContainer<Biome> 并替换其 palette 内的 Holder。
 */
public final class ChunkBiomePacketRewriter {

    private final Logger logger;
    private final String targetWorldName;
    private final BiomeHolderResolver holderResolver;
    private long lastDebugLogAtMs;
    private final ChunkSectionByteRewriter byteRewriter = new ChunkSectionByteRewriter();
    private final BiomeIdResolver biomeIdResolver;

    public ChunkBiomePacketRewriter(Logger logger, String targetWorldName, BiomeHolderResolver holderResolver, BiomeIdResolver biomeIdResolver) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.targetWorldName = Objects.requireNonNull(targetWorldName, "targetWorldName");
        this.holderResolver = Objects.requireNonNull(holderResolver, "holderResolver");
        this.biomeIdResolver = Objects.requireNonNull(biomeIdResolver, "biomeIdResolver");
    }

    public void rewriteIfApplicable(PacketEvent event, SeasonType season, Map<NamespacedKey, NamespacedKey> biomeKeyMap, NamespacedKey defaultToKey, boolean debug) {
        if ((biomeKeyMap == null || biomeKeyMap.isEmpty()) && defaultToKey == null) return;
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!targetWorldName.equals(world.getName())) {
            return;
        }
        PacketContainer packet = event.getPacket();
        PacketType type = event.getPacketType();
        // 只处理区块数据包，避免误伤其它包
        if (!isChunkPacket(type)) {
            return;
        }
        try {
            Object handle = packet.getHandle();
            if (handle == null) {
                return;
            }
            // 1) 先走字节重写（1.21.x 通常 chunk 包内部存的是序列化 byte[]）
            ByteRewriteStats byteStats = tryRewriteChunkBytes(handle, biomeKeyMap, defaultToKey);
            if (debug) {
                maybeLogByteDebug(byteStats, season, biomeKeyMap, defaultToKey);
            }
        } catch (Throwable t) {
            logger.warning("[NatureEngine] ProtocolLib biome rewrite failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private boolean isChunkPacket(PacketType type) {
        // ProtocolLib 5.4.0 在不同服务端上包名可能不同，这里用名称包含判断避免 NoSuchFieldError
        String name = type.name();
        return name.contains("MAP_CHUNK") || name.contains("LEVEL_CHUNK") || name.contains("CHUNK");
    }

    private Object findFirstFieldByNameContains(Object obj, String contains) throws IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getName().toLowerCase().contains(contains.toLowerCase())) {
                try {
                    f.setAccessible(true);
                    return f.get(obj);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Object findFirstArrayField(Object obj) throws IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            Object v;
            try {
                f.setAccessible(true);
                v = f.get(obj);
            } catch (Throwable ignored) {
                continue;
            }
            if (v != null && v.getClass().isArray()) {
                return v;
            }
        }
        return null;
    }

    private Object findFirstListLikeField(Object obj) throws IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            Object v;
            try {
                f.setAccessible(true);
                v = f.get(obj);
            } catch (Throwable ignored) {
                continue;
            }
            if (v != null && v.getClass().getName().contains("List")) {
                return v;
            }
        }
        return null;
    }

    private String tryExtractBiomeKey(Object entry) {
        if (entry == null) return null;
        // 常见：Holder#unwrapKey() -> Optional<ResourceKey>
        try {
            Method unwrapKey = entry.getClass().getMethod("unwrapKey");
            Object opt = unwrapKey.invoke(entry);
            if (opt instanceof Optional) {
                Object rk = ((Optional<?>) opt).orElse(null);
                if (rk != null) {
                    return resourceKeyToString(rk);
                }
            }
        } catch (Throwable ignored) {
        }
        // fallback：toString() 里包含 minecraft:xxx
        try {
            String s = entry.toString();
            int idx = s.indexOf("minecraft:");
            if (idx >= 0) {
                int end = s.indexOf(']', idx);
                if (end < 0) end = s.indexOf(')', idx);
                if (end < 0) end = Math.min(s.length(), idx + 64);
                String key = s.substring(idx, end);
                int space = key.indexOf(' ');
                if (space > 0) key = key.substring(0, space);
                int comma = key.indexOf(',');
                if (comma > 0) key = key.substring(0, comma);
                return key.trim();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String resourceKeyToString(Object resourceKey) {
        try {
            Method location = resourceKey.getClass().getMethod("location");
            Object rl = location.invoke(resourceKey);
            return rl != null ? rl.toString() : resourceKey.toString();
        } catch (Throwable ignored) {
            return resourceKey.toString();
        }
    }

    private Biome tryBukkitBiomeFromKey(String key) {
        // key 形如 minecraft:plains
        if (key == null) return null;
        String[] parts = key.split(":");
        String path = parts.length == 2 ? parts[1] : key;
        String enumName = path.toUpperCase().replace('/', '_');
        try {
            return Biome.valueOf(enumName);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Object tryConvertEntryToTarget(Object originalEntry, Biome target) {
        try {
            Object holder = holderResolver.resolve(target);
            if (holder == null) return null;
            // 确保类型可赋值（通常都是 Holder）
            if (originalEntry != null && !originalEntry.getClass().isInstance(holder)) {
                // holder 可能是同接口不同实现，允许直接替换
                return holder;
            }
            return holder;
        } catch (Throwable t) {
            return null;
        }
    }

    public interface BiomeHolderResolver {
        Object resolve(Biome biome);
    }

    public interface BiomeIdResolver {
        Integer resolveRawId(Biome biome);

        Integer resolveRawId(NamespacedKey biomeKey);

        /**
         * 当 resolveRawId 返回 null 时，用于输出定位信息（仅 debug 模式调用）。
         */
        default String debugWhyNull(Biome biome) {
            return null;
        }
    }

    private ByteRewriteStats tryRewriteChunkBytes(Object packetHandle, Map<NamespacedKey, NamespacedKey> biomeKeyMap, NamespacedKey defaultToKey) {
        ByteRewriteStats stats = new ByteRewriteStats();
        try {
            Object dataOwner = packetHandle;
            // 在对象图中查找 byte[]（chunkData buffer 通常在内层 data 对象里）
            byte[] bytes = findFirstByteArray(dataOwner, 0, 4);
            if (bytes == null || bytes.length == 0) {
                stats.noByteArrayFound = true;
                return stats;
            }
            stats.inputBytes = bytes.length;

            Map<Integer, Integer> idMap = new java.util.HashMap<>();
            if (biomeKeyMap != null) {
                for (var e : biomeKeyMap.entrySet()) {
                    Integer from = biomeIdResolver.resolveRawId(e.getKey());
                    Integer to = biomeIdResolver.resolveRawId(e.getValue());
                    if (from != null && to != null) {
                        idMap.put(from, to);
                    }
                }
            }
            Integer defaultToId = defaultToKey != null ? biomeIdResolver.resolveRawId(defaultToKey) : null;
            stats.defaultToId = defaultToId;
            stats.idMapSize = idMap.size();
            if (idMap.isEmpty() && defaultToId == null) {
                stats.idMapEmpty = true;
                return stats;
            }

            int sections = guessSectionCount();
            stats.sectionCount = sections;
            ChunkSectionByteRewriter.Result r = byteRewriter.rewrite(bytes, sections, idMap, defaultToId);
            stats.paletteEntriesReplaced = r.stats().paletteEntriesReplaced;
            stats.directPaletteSkipped = r.stats().directPaletteSkipped;
            stats.failed = r.stats().failed;
            stats.failReason = r.stats().failReason;

            if (!r.stats().failed && r.out() != bytes) {
                // 把新 byte[] 写回 packetHandle 对象图里的那个字段
                boolean wrote = replaceFirstByteArray(dataOwner, r.out(), 0, 4);
                stats.wroteBack = wrote;
                stats.outputBytes = r.out().length;
            }
        } catch (Throwable t) {
            stats.failed = true;
            stats.failReason = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
        return stats;
    }

    private int guessSectionCount() {
        // 默认世界高度：-64..320 -> 24 sections；如果以后要更精确可以从 World 读取，但这里不持有 world 实例。
        return 24;
    }

    private byte[] findFirstByteArray(Object obj, int depth, int maxDepth) throws Exception {
        if (obj == null || depth > maxDepth) return null;
        Class<?> cls = obj.getClass();
        String cn = cls.getName();
        if (cn.startsWith("java.") || cn.startsWith("javax.") || cn.startsWith("jdk.")) return null;
        for (Field f : cls.getDeclaredFields()) {
            try {
                f.setAccessible(true);
            } catch (Throwable ignored) {
                continue;
            }
            Object v;
            try {
                v = f.get(obj);
            } catch (Throwable ignored) {
                continue;
            }
            if (v == null) continue;
            if (v instanceof byte[] b && b.length > 0) {
                return b;
            }
            Class<?> fc = v.getClass();
            if (fc.isArray() || fc.isPrimitive() || fc.isEnum()) continue;
            byte[] nested = findFirstByteArray(v, depth + 1, maxDepth);
            if (nested != null) return nested;
        }
        return null;
    }

    private boolean replaceFirstByteArray(Object obj, byte[] replacement, int depth, int maxDepth) throws Exception {
        if (obj == null || depth > maxDepth) return false;
        Class<?> cls = obj.getClass();
        String cn = cls.getName();
        if (cn.startsWith("java.") || cn.startsWith("javax.") || cn.startsWith("jdk.")) return false;
        for (Field f : cls.getDeclaredFields()) {
            try {
                f.setAccessible(true);
            } catch (Throwable ignored) {
                continue;
            }
            Object v;
            try {
                v = f.get(obj);
            } catch (Throwable ignored) {
                continue;
            }
            if (v instanceof byte[] b && b.length > 0) {
                try {
                    f.set(obj, replacement);
                    return true;
                } catch (Throwable ignored) {
                    return false;
                }
            }
            if (v == null) continue;
            Class<?> fc = v.getClass();
            if (fc.isArray() || fc.isPrimitive() || fc.isEnum()) continue;
            if (replaceFirstByteArray(v, replacement, depth + 1, maxDepth)) return true;
        }
        return false;
    }

    private void maybeLogByteDebug(ByteRewriteStats stats, SeasonType season, Map<NamespacedKey, NamespacedKey> biomeKeyMap, NamespacedKey defaultToKey) {
        long now = System.currentTimeMillis();
        if (now - lastDebugLogAtMs < 3000L) return;
        lastDebugLogAtMs = now;
        logger.info("[NatureEngine] biome-rewrite debug: season=" + season.name()
                + " mapSize=" + (biomeKeyMap == null ? 0 : biomeKeyMap.size())
                + " idMap=" + stats.idMapSize
                + " defaultToId=" + stats.defaultToId
                + " bytesIn=" + stats.inputBytes
                + " bytesOut=" + stats.outputBytes
                + " sections=" + stats.sectionCount
                + " replaced=" + stats.paletteEntriesReplaced
                + " directSkip=" + stats.directPaletteSkipped
                + " wroteBack=" + stats.wroteBack
                + " noBytes=" + stats.noByteArrayFound
                + " idEmpty=" + stats.idMapEmpty
                + (stats.failed ? (" fail=" + stats.failReason) : ""));
    }

    private static final class ByteRewriteStats {
        int idMapSize;
        int inputBytes;
        int outputBytes;
        int sectionCount;
        int paletteEntriesReplaced;
        int directPaletteSkipped;
        Integer defaultToId;
        boolean wroteBack;
        boolean noByteArrayFound;
        boolean idMapEmpty;
        boolean failed;
        String failReason;
    }
}

