package com.github.cinnaio.natureEngine.api;

import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentManager;
import org.bukkit.block.Block;

public final class EnvironmentAPI {

    private static final ServiceLocator SERVICES = ServiceLocator.getInstance();

    private EnvironmentAPI() {
    }

    public static EnvironmentContext getContext(Block block) {
        return SERVICES.get(EnvironmentManager.class).getContext(block);
    }
}

