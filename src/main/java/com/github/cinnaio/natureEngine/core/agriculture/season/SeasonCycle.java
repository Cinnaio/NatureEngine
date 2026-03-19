package com.github.cinnaio.natureEngine.core.agriculture.season;

import org.bukkit.World;

/**
 * 负责根据世界时间推进季节。
 */
public final class SeasonCycle {

    private final SeasonSettingsProvider settingsProvider;
    private static final int SOLAR_TERMS_PER_SEASON = 6;

    public SeasonCycle(SeasonSettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    public SeasonSnapshot getSeasonForWorld(World world, long worldDay) {
        SeasonSettings spring = settingsProvider.getSettings(SeasonType.SPRING);
        SeasonSettings summer = settingsProvider.getSettings(SeasonType.SUMMER);
        SeasonSettings autumn = settingsProvider.getSettings(SeasonType.AUTUMN);
        SeasonSettings winter = settingsProvider.getSettings(SeasonType.WINTER);

        long yearLength = spring.getLengthInDays()
                + summer.getLengthInDays()
                + autumn.getLengthInDays()
                + winter.getLengthInDays();

        long dayInYear = worldDay % yearLength;

        long cursor = 0;
        if (dayInYear < (cursor += spring.getLengthInDays())) {
            return snapshotWithSolarTerm(SeasonType.SPRING, dayInYear, spring.getLengthInDays());
        }
        if (dayInYear < (cursor += summer.getLengthInDays())) {
            long local = dayInYear - (cursor - summer.getLengthInDays());
            return snapshotWithSolarTerm(SeasonType.SUMMER, local, summer.getLengthInDays());
        }
        if (dayInYear < (cursor += autumn.getLengthInDays())) {
            long local = dayInYear - (cursor - autumn.getLengthInDays());
            return snapshotWithSolarTerm(SeasonType.AUTUMN, local, autumn.getLengthInDays());
        }
        long local = dayInYear - (cursor - winter.getLengthInDays());
        return snapshotWithSolarTerm(SeasonType.WINTER, local, winter.getLengthInDays());
    }

    private static SeasonSnapshot snapshotWithSolarTerm(SeasonType type, long localDay, long seasonLenDays) {
        double seasonProgress = seasonLenDays <= 0 ? 0.0 : (double) localDay / (double) seasonLenDays;
        double daysPerTerm = seasonLenDays <= 0 ? 1.0 : (double) seasonLenDays / (double) SOLAR_TERMS_PER_SEASON;

        int idxInSeason;
        if (daysPerTerm <= 0.0) {
            idxInSeason = 0;
        } else {
            idxInSeason = (int) Math.floor(localDay / daysPerTerm);
            if (idxInSeason < 0) idxInSeason = 0;
            if (idxInSeason >= SOLAR_TERMS_PER_SEASON) idxInSeason = SOLAR_TERMS_PER_SEASON - 1;
        }

        double termStart = idxInSeason * daysPerTerm;
        double termProgress = daysPerTerm <= 0.0 ? 0.0 : ((double) localDay - termStart) / daysPerTerm;
        if (termProgress < 0.0) termProgress = 0.0;
        if (termProgress > 1.0) termProgress = 1.0;

        SolarTerm term = SolarTerm.bySeasonIndex(type, idxInSeason);
        return new SeasonSnapshot(type, seasonProgress, term, termProgress);
    }

    public interface SeasonSettingsProvider {
        SeasonSettings getSettings(SeasonType type);
    }

    public static final class SeasonSnapshot {
        private final SeasonType type;
        private final double progress;
        private final SolarTerm solarTerm;
        private final double solarTermProgress;

        public SeasonSnapshot(SeasonType type, double progress) {
            this(type, progress, null, 0.0);
        }

        public SeasonSnapshot(SeasonType type, double progress, SolarTerm solarTerm, double solarTermProgress) {
            this.type = type;
            this.progress = progress;
            this.solarTerm = solarTerm;
            this.solarTermProgress = solarTermProgress;
        }

        public SeasonType getType() {
            return type;
        }

        /**
         * 当前季节进度，0.0-1.0。
         */
        public double getProgress() {
            return progress;
        }

        /** 当前节气（可能为 null：用于兼容旧调用或异常配置）。 */
        public SolarTerm getSolarTerm() {
            return solarTerm;
        }

        /** 当前节气进度，0.0-1.0。 */
        public double getSolarTermProgress() {
            return solarTermProgress;
        }
    }
}

