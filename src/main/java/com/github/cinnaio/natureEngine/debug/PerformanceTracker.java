package com.github.cinnaio.natureEngine.debug;

public final class PerformanceTracker {

    private final DebugLogger logger;

    public PerformanceTracker(DebugLogger logger) {
        this.logger = logger;
    }

    public long start() {
        return System.nanoTime();
    }

    public void endAndLog(DebugModule module, String label, long startNano) {
        long durationMicros = (System.nanoTime() - startNano) / 1000L;
        logger.log(module, DebugLevel.TRACE, label + " took " + durationMicros + " µs");
    }
}

