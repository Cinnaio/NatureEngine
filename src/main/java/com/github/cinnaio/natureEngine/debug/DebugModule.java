package com.github.cinnaio.natureEngine.debug;

public enum DebugModule {
    CORE_SEASON("core-season"),
    CORE_WEATHER("core-weather"),
    CORE_ENVIRONMENT("core-environment"),
    CORE_GROWTH("core-growth"),
    CORE_CROP("core-crop"),
    INTEGRATION_CRAFTENGINE("integration-craftengine"),
    SIMULATION("simulation"),
    CONFIG("config"),
    COMMAND("command"),
    DEBUG("debug");

    private final String configKey;

    DebugModule(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}

