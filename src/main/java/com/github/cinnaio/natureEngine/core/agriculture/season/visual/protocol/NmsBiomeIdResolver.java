package com.github.cinnaio.natureEngine.core.agriculture.season.visual.protocol;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 Bukkit Biome 映射为 NMS Registry 的 raw id（用于 chunk paletted container 序列化数据替换）。
 *
 * 仅使用反射，避免直接依赖 net.minecraft 包。
 */
public final class NmsBiomeIdResolver {

    private final World world;
    private final Map<Biome, Integer> cache = new ConcurrentHashMap<>();

    public NmsBiomeIdResolver(World world) {
        this.world = world;
    }

    public Integer resolveRawId(Biome biome) {
        return cache.computeIfAbsent(biome, this::resolveRawIdUncached);
    }

    public Integer resolveRawId(NamespacedKey biomeKey) {
        if (biomeKey == null) return null;
        try {
            Object registry = getBuiltInBiomeRegistry();
            if (registry == null) {
                Object reg2 = tryGetBiomeRegistryFromRegistryAccess();
                if (reg2 == null) return null;
                registry = reg2;
            }
            Object resourceLocation = createResourceLocation(biomeKey);
            if (resourceLocation == null) return null;
            Object value = tryGetRegistryValue(registry, resourceLocation);
            if (value == null) return null;
            Integer id = tryInvokeGetId(registry, value);
            return (id != null && id >= 0) ? id : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public String debugWhyNull(Biome biome) {
        try {
            NamespacedKey key = biomeKey(biome);
            if (key == null) return "key=null";

            Object registry = getBuiltInBiomeRegistry();
            if (registry == null) {
                Object reg2 = tryGetBiomeRegistryFromRegistryAccess();
                if (reg2 == null) return "BuiltInRegistries.BIOME=null & registryAccessBiomeRegistry=null";
                registry = reg2;
            }

            Object resourceLocation = createResourceLocation(key);
            if (resourceLocation == null) return "ResourceLocation=null key=" + key;

            Object value = tryGetRegistryValue(registry, resourceLocation);
            if (value == null) return "registryValue=null key=" + key;

            Integer id = tryInvokeGetId(registry, value);
            if (id == null) return "getId(value)=null key=" + key + " valueClass=" + value.getClass().getName();

            return "ok id=" + id;
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    private Integer resolveRawIdUncached(Biome biome) {
        try {
            NamespacedKey key = biomeKey(biome);
            if (key == null) return null;

            // 最稳方式：直接用 BuiltInRegistries.BIOME 查 value 并取 raw id
            Object registry = getBuiltInBiomeRegistry();
            if (registry == null) {
                Object reg2 = tryGetBiomeRegistryFromRegistryAccess();
                if (reg2 == null) return null;
                registry = reg2;
            }

            Object resourceLocation = createResourceLocation(key);
            if (resourceLocation == null) return null;

            // value = registry.getValue(ResourceLocation) / get(ResourceLocation)
            Object value = tryGetRegistryValue(registry, resourceLocation);
            if (value == null) return null;

            Integer id = tryInvokeGetId(registry, value);
            return (id != null && id >= 0) ? id : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object getBuiltInBiomeRegistry() {
        // 注意：Paper Plugins + 部分 fork 的类加载隔离会导致默认 Class.forName 走错 ClassLoader
        // 这里按优先级尝试：server classloader -> context classloader -> 当前 classloader
        String cn = "net.minecraft.core.registries.BuiltInRegistries";
        try {
            ClassLoader serverCl = Bukkit.getServer().getClass().getClassLoader();
            Class<?> builtIn = Class.forName(cn, false, serverCl);
            return builtIn.getField("BIOME").get(null);
        } catch (Throwable ignored) {
        }
        try {
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null) {
                Class<?> builtIn = Class.forName(cn, false, ctx);
                return builtIn.getField("BIOME").get(null);
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> builtIn = Class.forName(cn);
            return builtIn.getField("BIOME").get(null);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object tryGetBiomeRegistryFromRegistryAccess() {
        try {
            Object craftServer = Bukkit.getServer();
            Object nmsServer = invoke(craftServer, "getServer");
            Object registryAccess = invoke(nmsServer, "registryAccess");

            ClassLoader serverCl = Bukkit.getServer().getClass().getClassLoader();
            Class<?> registriesCls = Class.forName("net.minecraft.core.registries.Registries", false, serverCl);
            Object biomeRegistryKey = registriesCls.getField("BIOME").get(null);
            return invokeSingleArgByName(registryAccess, "lookupOrThrow", biomeRegistryKey);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object tryGetRegistryValue(Object registry, Object resourceLocation) {
        // 兼容不同实现的方法名
        try {
            Object v = tryInvokeSingle(registry, "getValue", resourceLocation);
            if (v != null) return v;
        } catch (Throwable ignored) {
        }
        try {
            Object v = tryInvokeSingle(registry, "get", resourceLocation);
            if (v != null) return v;
        } catch (Throwable ignored) {
        }
        // 有些实现会把 ResourceLocation 包在 Optional 里
        try {
            Object opt = tryInvokeSingle(registry, "getOptional", resourceLocation);
            if (opt != null && opt.getClass().getName().contains("Optional")) {
                Method orElse = opt.getClass().getMethod("orElse", Object.class);
                return orElse.invoke(opt, new Object[]{null});
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object createResourceLocation(NamespacedKey key) {
        // 1.21+ 的 ResourceLocation 构造方法在某些映射下可能不可用，优先用静态工厂
        String cn = "net.minecraft.resources.ResourceLocation";
        ClassLoader serverCl = Bukkit.getServer().getClass().getClassLoader();
        Class<?> cls;
        try {
            cls = Class.forName(cn, false, serverCl);
        } catch (Throwable t) {
            try {
                cls = Class.forName(cn);
            } catch (Throwable ignored) {
                return null;
            }
        }

        String full = key.toString(); // namespace:path
        String ns = key.getNamespace();
        String path = key.getKey();

        // try (String) ctor
        try {
            return cls.getConstructor(String.class).newInstance(full);
        } catch (Throwable ignored) {
        }
        // try (String, String) ctor
        try {
            return cls.getConstructor(String.class, String.class).newInstance(ns, path);
        } catch (Throwable ignored) {
        }
        // try static fromNamespaceAndPath(String,String)
        try {
            Method m = cls.getMethod("fromNamespaceAndPath", String.class, String.class);
            return m.invoke(null, ns, path);
        } catch (Throwable ignored) {
        }
        // try static tryParse(String)
        try {
            Method m = cls.getMethod("tryParse", String.class);
            return m.invoke(null, full);
        } catch (Throwable ignored) {
        }
        // try static parse(String)
        try {
            Method m = cls.getMethod("parse", String.class);
            return m.invoke(null, full);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private NamespacedKey biomeKey(Biome biome) {
        try {
            if (biome instanceof Keyed) {
                return ((Keyed) biome).getKey();
            }
        } catch (Throwable ignored) {
        }
        return NamespacedKey.minecraft(biome.name().toLowerCase());
    }

    private Integer tryInvokeGetId(Object registry, Object arg) {
        try {
            for (Method m : registry.getClass().getMethods()) {
                if (!m.getName().equals("getId")) continue;
                if (m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].isInstance(arg)) continue;
                Object out = m.invoke(registry, arg);
                if (out instanceof Integer) return (Integer) out;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object invoke(Object target, String methodName) throws Exception {
        Method m = target.getClass().getMethod(methodName);
        return m.invoke(target);
    }

    private Object invokeSingleArgByName(Object target, String methodName, Object arg) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != 1) continue;
            if (!m.getParameterTypes()[0].isInstance(arg)) continue;
            return m.invoke(target, arg);
        }
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != 1) continue;
            return m.invoke(target, arg);
        }
        throw new NoSuchMethodException(methodName);
    }

    private Object tryInvokeSingle(Object target, String methodName, Object arg) {
        try {
            for (Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].isInstance(arg)) continue;
                return m.invoke(target, arg);
            }
            for (Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (m.getParameterCount() != 1) continue;
                return m.invoke(target, arg);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}

