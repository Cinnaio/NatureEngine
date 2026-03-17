package com.github.cinnaio.natureEngine.core.agriculture.season.visual.tint;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonManager;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 仿 AdvancedSeasons：拦截“注册表同步(Registry Data)”包，覆盖 biome effects 颜色（草/叶/水/天空/雾）。
 *
 * <p>注意：</p>
 * - 1.21.x 里该包通常在 configuration 阶段发送（进服时）。因此季节切换想“立刻变色”，需要额外做重发机制（后续再加）。
 */
public final class ProtocolBiomeTintListener {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final SeasonManager seasonManager;
    private final BiomeTintPalette palette;
    private final ProtocolManager protocolManager;

    private volatile boolean started;
    private long lastDebugAtMs;

    public ProtocolBiomeTintListener(JavaPlugin plugin, SeasonManager seasonManager, BiomeTintPalette palette) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.seasonManager = seasonManager;
        this.palette = palette;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        if (started) return;
        started = true;

        PacketType[] types = registryPacketTypes();
        if (types.length == 0) {
            logger.warning("[NatureEngine] biome tint: 未能找到 REGISTRY_DATA 包类型（ProtocolLib 映射差异），本功能暂不可用。");
            return;
        }

        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, types) {
            @Override
            public void onPacketSending(PacketEvent event) {
                tryRewrite(event);
            }
        };
        protocolManager.addPacketListener(adapter);
        logger.info("[NatureEngine] ProtocolLib biome tint listener enabled.");
    }

    private void tryRewrite(PacketEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        SeasonType season = seasonManager.getCurrentSeason(world);

        Object handle = event.getPacket().getHandle();
        if (handle == null) return;

        // 反射找 entries list（每个 entry = id + (optional) nbt）
        List<?> entries = findFirstList(handle, 0, 5);
        if (entries == null || entries.isEmpty()) return;

        int touched = 0;
        int replaced = 0;
        for (Object entry : entries) {
            if (entry == null) continue;
            String id = tryGetEntryId(entry);
            if (id == null || id.isBlank()) continue;

            NamespacedKey biomeKey = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
            if (biomeKey == null) continue;

            BiomeTintPalette.Colors c = palette.get(season, biomeKey);
            if (c == null) continue;

            Object tag = tryGetEntryTag(entry);
            if (tag == null) continue;

            touched++;
            if (applyEffectsColors(tag, c)) {
                replaced++;
            }
        }

        if (touched > 0) {
            maybeLogDebug(season, touched, replaced);
        }
    }

    private void maybeLogDebug(SeasonType season, int touched, int replaced) {
        long now = System.currentTimeMillis();
        if (now - lastDebugAtMs < 3000L) return;
        lastDebugAtMs = now;
        logger.info("[NatureEngine] biome-tint debug: season=" + season.name() + " touched=" + touched + " replaced=" + replaced);
    }

    /**
     * 尝试把 effects 颜色写进 biome 的 NBT（CompoundTag）。
     * biome NBT 结构：{ ..., effects: { fog_color, water_color, water_fog_color, sky_color, foliage_color, grass_color, ... } }
     */
    private boolean applyEffectsColors(Object biomeTag, BiomeTintPalette.Colors colors) {
        try {
            Object effects = getOrCreateCompound(biomeTag, "effects");
            if (effects == null) return false;

            // 只覆盖非 0 的颜色（方便用户留空）
            if (colors.fog() != 0) putInt(effects, "fog_color", colors.fog());
            if (colors.water() != 0) putInt(effects, "water_color", colors.water());
            if (colors.waterFog() != 0) putInt(effects, "water_fog_color", colors.waterFog());
            if (colors.sky() != 0) putInt(effects, "sky_color", colors.sky());
            if (colors.foliage() != 0) putInt(effects, "foliage_color", colors.foliage());
            if (colors.grass() != 0) putInt(effects, "grass_color", colors.grass());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // -------- PacketType discover --------

    private PacketType[] registryPacketTypes() {
        List<PacketType> out = new ArrayList<>();

        // 反射读取 PacketType.Configuration.Server.*（避免编译期引用导致 NoClassDef/NoSuchField）
        PacketType t1 = reflectPacketType("com.comphenix.protocol.PacketType$Configuration$Server", "REGISTRY_DATA");
        if (t1 != null) out.add(t1);
        PacketType t2 = reflectPacketType("com.comphenix.protocol.PacketType$Configuration$Server", "CLIENTBOUND_REGISTRY_DATA");
        if (t2 != null) out.add(t2);
        PacketType t3 = reflectPacketType("com.comphenix.protocol.PacketType$Configuration$Server", "REGISTRY");
        if (t3 != null) out.add(t3);

        // 一些 fork/映射可能放在 Play.Server（兜底）
        PacketType p1 = reflectPacketType("com.comphenix.protocol.PacketType$Play$Server", "REGISTRY_DATA");
        if (p1 != null) out.add(p1);
        PacketType p2 = reflectPacketType("com.comphenix.protocol.PacketType$Play$Server", "CLIENTBOUND_REGISTRY_DATA");
        if (p2 != null) out.add(p2);

        return out.toArray(new PacketType[0]);
    }

    private PacketType reflectPacketType(String ownerClassName, String fieldName) {
        try {
            Class<?> owner = Class.forName(ownerClassName);
            Field f = owner.getField(fieldName);
            Object v = f.get(null);
            return (v instanceof PacketType pt) ? pt : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // -------- Reflection helpers for packet handle --------

    private List<?> findFirstList(Object obj, int depth, int maxDepth) {
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
            if (v instanceof List<?> list && !list.isEmpty()) {
                // heuristics: list element likely has id/tag
                Object first = list.get(0);
                if (first != null && (tryGetEntryId(first) != null || tryGetEntryTag(first) != null)) {
                    return list;
                }
            }
            if (v == null) continue;
            if (v.getClass().isArray() || v.getClass().isEnum() || v.getClass().isPrimitive()) continue;
            List<?> nested = findFirstList(v, depth + 1, maxDepth);
            if (nested != null) return nested;
        }
        return null;
    }

    private String tryGetEntryId(Object entry) {
        // 先尝试 record accessor: id()
        Object id = invokeNoArg(entry, "id");
        if (id == null) id = invokeNoArg(entry, "key");
        if (id == null) id = invokeNoArg(entry, "name");

        if (id == null) {
            // field fallback
            id = readFieldByNameContains(entry, "id");
            if (id == null) id = readFieldByNameContains(entry, "key");
        }

        if (id == null) return null;
        return id.toString();
    }

    private Object tryGetEntryTag(Object entry) {
        Object tag = invokeNoArg(entry, "data");
        if (tag == null) tag = invokeNoArg(entry, "value");
        if (tag == null) tag = invokeNoArg(entry, "tag");
        if (tag == null) tag = readFieldByNameContains(entry, "data");
        if (tag == null) tag = readFieldByNameContains(entry, "value");

        if (tag instanceof Optional<?> opt) {
            return opt.orElse(null);
        }
        return tag;
    }

    private Object invokeNoArg(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object readFieldByNameContains(Object obj, String contains) {
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (!f.getName().toLowerCase(Locale.ROOT).contains(contains)) continue;
                try {
                    f.setAccessible(true);
                } catch (Throwable ignored) {
                    continue;
                }
                return f.get(obj);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // -------- NBT CompoundTag facade (reflection) --------

    private Object getOrCreateCompound(Object tag, String key) throws Exception {
        Object existing = invoke(tag, "getCompound", new Class[]{String.class}, new Object[]{key});
        if (existing != null && isCompoundTag(existing)) {
            return existing;
        }
        // new CompoundTag()
        Object child = newCompoundTag(tag.getClass().getClassLoader());
        if (child == null) return null;
        invoke(tag, "put", new Class[]{String.class, findTagBaseClass(tag)}, new Object[]{key, child});
        return child;
    }

    private void putInt(Object compound, String key, int value) throws Exception {
        invoke(compound, "putInt", new Class[]{String.class, int.class}, new Object[]{key, value});
    }

    private Object invoke(Object target, String name, Class<?>[] paramTypes, Object[] args) throws Exception {
        Method m = target.getClass().getMethod(name, paramTypes);
        return m.invoke(target, args);
    }

    private boolean isCompoundTag(Object obj) {
        return obj.getClass().getName().endsWith(".CompoundTag");
    }

    private Object newCompoundTag(ClassLoader cl) {
        try {
            Class<?> cls = Class.forName("net.minecraft.nbt.CompoundTag", true, cl);
            return cls.getConstructor().newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Class<?> findTagBaseClass(Object tag) {
        // net.minecraft.nbt.Tag
        try {
            return Class.forName("net.minecraft.nbt.Tag", true, tag.getClass().getClassLoader());
        } catch (Throwable ignored) {
            // fallback: use Object.class
            return Object.class;
        }
    }
}

