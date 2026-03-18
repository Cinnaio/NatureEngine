package com.github.cinnaio.natureEngine.core.agriculture.growth;

import com.github.cinnaio.natureEngine.core.environment.EnvironmentType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 作物级环境策略覆写（可选）。
 */
public final class CropEnvironmentPolicy {

    private final boolean enabled;
    private final Double mitigationStrengthOverride;
    private final Double exposureBoostRangeOverride;
    private final Map<EnvironmentType, Double> stabilityOverride;
    private final Map<EnvironmentType, Double> advanceBoostOverride;

    public CropEnvironmentPolicy(
            boolean enabled,
            Double mitigationStrengthOverride,
            Double exposureBoostRangeOverride,
            Map<EnvironmentType, Double> stabilityOverride,
            Map<EnvironmentType, Double> advanceBoostOverride
    ) {
        this.enabled = enabled;
        this.mitigationStrengthOverride = mitigationStrengthOverride;
        this.exposureBoostRangeOverride = exposureBoostRangeOverride;
        this.stabilityOverride = stabilityOverride;
        this.advanceBoostOverride = advanceBoostOverride;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Double getMitigationStrengthOverride() {
        return mitigationStrengthOverride;
    }

    public Double getExposureBoostRangeOverride() {
        return exposureBoostRangeOverride;
    }

    public Double getStabilityOverride(EnvironmentType type) {
        return stabilityOverride != null ? stabilityOverride.get(type) : null;
    }

    public Double getAdvanceBoostOverride(EnvironmentType type) {
        return advanceBoostOverride != null ? advanceBoostOverride.get(type) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean enabled = true;
        private Double mitigationStrengthOverride;
        private Double exposureBoostRangeOverride;
        private final EnumMap<EnvironmentType, Double> stabilityOverride = new EnumMap<>(EnvironmentType.class);
        private final EnumMap<EnvironmentType, Double> advanceBoostOverride = new EnumMap<>(EnvironmentType.class);

        public Builder enabled(boolean v) {
            this.enabled = v;
            return this;
        }

        public Builder mitigationStrengthOverride(Double v) {
            this.mitigationStrengthOverride = v;
            return this;
        }

        public Builder exposureBoostRangeOverride(Double v) {
            this.exposureBoostRangeOverride = v;
            return this;
        }

        public Builder stabilityOverride(EnvironmentType type, Double v) {
            if (type != null && v != null) stabilityOverride.put(type, v);
            return this;
        }

        public Builder advanceBoostOverride(EnvironmentType type, Double v) {
            if (type != null && v != null) advanceBoostOverride.put(type, v);
            return this;
        }

        public CropEnvironmentPolicy build() {
            Map<EnvironmentType, Double> st = stabilityOverride.isEmpty() ? null : new EnumMap<>(stabilityOverride);
            Map<EnvironmentType, Double> ab = advanceBoostOverride.isEmpty() ? null : new EnumMap<>(advanceBoostOverride);
            return new CropEnvironmentPolicy(enabled, mitigationStrengthOverride, exposureBoostRangeOverride, st, ab);
        }
    }
}

