package com.github.cinnaio.natureEngine.core.agriculture.season;

import org.bukkit.World;

/**
 * 负责根据世界时间推进季节。
 */
public final class SeasonCycle {

    private final SeasonSettingsProvider settingsProvider;

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
            return new SeasonSnapshot(SeasonType.SPRING, (double) dayInYear / spring.getLengthInDays());
        }
        if (dayInYear < (cursor += summer.getLengthInDays())) {
            long local = dayInYear - (cursor - summer.getLengthInDays());
            return new SeasonSnapshot(SeasonType.SUMMER, (double) local / summer.getLengthInDays());
        }
        if (dayInYear < (cursor += autumn.getLengthInDays())) {
            long local = dayInYear - (cursor - autumn.getLengthInDays());
            return new SeasonSnapshot(SeasonType.AUTUMN, (double) local / autumn.getLengthInDays());
        }
        long local = dayInYear - (cursor - winter.getLengthInDays());
        return new SeasonSnapshot(SeasonType.WINTER, (double) local / winter.getLengthInDays());
    }

    public interface SeasonSettingsProvider {
        SeasonSettings getSettings(SeasonType type);
    }

    public static final class SeasonSnapshot {
        private final SeasonType type;
        private final double progress;

        public SeasonSnapshot(SeasonType type, double progress) {
            this.type = type;
            this.progress = progress;
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
    }
}

