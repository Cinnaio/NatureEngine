package com.github.cinnaio.natureEngine.core.agriculture.growth;

/**
 * 表示一次生长 Tick 对该植物的结果。
 */
public final class GrowthResult {

    private final int stageDelta;
    private final boolean shouldWither;
    private final double effectiveGrowthMultiplier;

    private GrowthResult(int stageDelta, boolean shouldWither, double effectiveGrowthMultiplier) {
        this.stageDelta = stageDelta;
        this.shouldWither = shouldWither;
        this.effectiveGrowthMultiplier = effectiveGrowthMultiplier;
    }

    public static GrowthResult noChange() {
        return new GrowthResult(0, false, 1.0);
    }

    public static GrowthResult advanceOneStage(double multiplier) {
        return new GrowthResult(1, false, multiplier);
    }

    public static GrowthResult wither() {
        return new GrowthResult(0, true, 0.0);
    }

    public int getStageDelta() {
        return stageDelta;
    }

    public boolean isShouldWither() {
        return shouldWither;
    }

    public double getEffectiveGrowthMultiplier() {
        return effectiveGrowthMultiplier;
    }
}

