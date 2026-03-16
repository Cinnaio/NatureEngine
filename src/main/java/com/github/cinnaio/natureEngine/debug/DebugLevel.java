package com.github.cinnaio.natureEngine.debug;

public enum DebugLevel {
    OFF,
    INFO,
    DEBUG,
    TRACE;

    public boolean isEnabledAt(DebugLevel target) {
        return this.ordinal() >= target.ordinal();
    }

    public static DebugLevel fromString(String value) {
        if (value == null) {
            return INFO;
        }
        try {
            return DebugLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return INFO;
        }
    }
}

