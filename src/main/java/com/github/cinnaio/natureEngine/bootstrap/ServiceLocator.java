package com.github.cinnaio.natureEngine.bootstrap;

import java.util.HashMap;
import java.util.Map;

/**
 * 非侵入式的简单 ServiceLocator，用于在没有完整 DI 框架的前提下管理核心服务。
 */
public final class ServiceLocator {

    private static final ServiceLocator INSTANCE = new ServiceLocator();

    private final Map<Class<?>, Object> services = new HashMap<>();

    private ServiceLocator() {
    }

    public static ServiceLocator getInstance() {
        return INSTANCE;
    }

    public <T> void register(Class<T> type, T impl) {
        services.put(type, impl);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        return (T) services.get(type);
    }

    public void clear() {
        services.clear();
    }
}

