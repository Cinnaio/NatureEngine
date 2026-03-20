package com.github.cinnaio.natureEngine.api;

import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentManager;
import org.bukkit.block.Block;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnvironmentAPI {

    private static final ServiceLocator SERVICES = ServiceLocator.getInstance();

    /**
     * 环境计算通常会被 PlaceholderAPI 同一刷新周期内多次请求同一坐标。
     * 这里做一个短 TTL 缓存以避免重复采样与修正计算。
     */
    private static final long CONTEXT_CACHE_TTL_MS = 1000L;
    private static final int CONTEXT_CACHE_MAX_ENTRIES = 5000;

    private static final ConcurrentHashMap<ContextKey, CachedContext> CONTEXT_CACHE = new ConcurrentHashMap<>();

    private EnvironmentAPI() {
    }

    public static EnvironmentContext getContext(Block block) {
        if (block == null) return null;

        long now = System.currentTimeMillis();
        UUID worldId = block.getWorld() != null ? block.getWorld().getUID() : new UUID(0L, 0L);
        ContextKey key = new ContextKey(worldId, block.getX(), block.getY(), block.getZ());

        CachedContext cached = CONTEXT_CACHE.get(key);
        if (cached != null && cached.expiresAtMs > now) {
            return cached.context;
        }

        if (CONTEXT_CACHE.size() > CONTEXT_CACHE_MAX_ENTRIES) {
            // 简单保护：缓存太大时直接清空（优先保证内存不无限增长）。
            CONTEXT_CACHE.clear();
        }

        EnvironmentContext ctx = SERVICES.get(EnvironmentManager.class).getContext(block);
        if (ctx != null) {
            CONTEXT_CACHE.put(key, new CachedContext(ctx, now + CONTEXT_CACHE_TTL_MS));
        }
        return ctx;
    }

    private record ContextKey(UUID worldId, int x, int y, int z) {}

    private static final class CachedContext {
        private final EnvironmentContext context;
        private final long expiresAtMs;

        private CachedContext(EnvironmentContext context, long expiresAtMs) {
            this.context = context;
            this.expiresAtMs = expiresAtMs;
        }
    }
}

