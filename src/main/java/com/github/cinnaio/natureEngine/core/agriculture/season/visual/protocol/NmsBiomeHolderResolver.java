package com.github.cinnaio.natureEngine.core.agriculture.season.visual.protocol;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过反射从 NMS RegistryAccess 获取 Holder<Biome>，用于替换 chunk biome palette entries。
 *
 * 仅使用反射，避免直接依赖 net.minecraft 包。
 */
public final class NmsBiomeHolderResolver implements ChunkBiomePacketRewriter.BiomeHolderResolver {

    private final World world;
    private final Map<Biome, Object> cache = new ConcurrentHashMap<>();

    public NmsBiomeHolderResolver(World world) {
        this.world = world;
    }

    @Override
    public Object resolve(Biome biome) {
        return cache.computeIfAbsent(biome, this::resolveUncached);
    }

    private Object resolveUncached(Biome biome) {
        try {
            NamespacedKey key = biomeKey(biome);
            if (key == null) return null;

            Object handle = invoke(world, "getHandle");
            Object registryAccess = invoke(handle, "registryAccess");

            Class<?> registriesCls = Class.forName("net.minecraft.core.registries.Registries");
            Object biomeRegistryKey = registriesCls.getField("BIOME").get(null);

            Object registry = invokeSingleArgByName(registryAccess, "lookupOrThrow", biomeRegistryKey);

            Class<?> resourceLocationCls = Class.forName("net.minecraft.resources.ResourceLocation");
            Object resourceLocation = resourceLocationCls.getConstructor(String.class).newInstance(key.toString());

            Class<?> resourceKeyCls = Class.forName("net.minecraft.resources.ResourceKey");
            Method create = resourceKeyCls.getMethod("create", biomeRegistryKey.getClass(), resourceLocationCls);
            Object biomeResourceKey = create.invoke(null, biomeRegistryKey, resourceLocation);

            // Registry#getHolderOrThrow(ResourceKey)
            return invokeSingleArgByName(registry, "getHolderOrThrow", biomeResourceKey);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private NamespacedKey biomeKey(Biome biome) {
        try {
            if (biome instanceof Keyed) {
                return ((Keyed) biome).getKey();
            }
        } catch (Throwable ignored) {
        }
        // fallback：尽量构造 minecraft:<lowercase>
        return NamespacedKey.minecraft(biome.name().toLowerCase());
    }

    private Object invoke(Object target, String methodName) throws Exception {
        Method m = target.getClass().getMethod(methodName);
        return m.invoke(target);
    }

    private Object invoke(Object target, String methodName, Class<?>[] types, Object[] args) throws Exception {
        Method m = target.getClass().getMethod(methodName, types);
        return m.invoke(target, args);
    }

    private Object invokeSingleArgByName(Object target, String methodName, Object arg) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != 1) continue;
            if (!m.getParameterTypes()[0].isInstance(arg)) continue;
            return m.invoke(target, arg);
        }
        // fallback: first same-name 1-arg method
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != 1) continue;
            return m.invoke(target, arg);
        }
        throw new NoSuchMethodException(methodName);
    }
}

