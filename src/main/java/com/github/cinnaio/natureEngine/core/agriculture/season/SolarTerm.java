package com.github.cinnaio.natureEngine.core.agriculture.season;

import java.util.Locale;

/**
 * 24 节气（按四季划分：每季 6 个）。
 *
 * 说明：
 * - 这里只定义“年内顺序/季节归属”，不绑定现实日期。
 * - 具体“何时处于哪个节气”由 SeasonCycle 基于 MC 时间轴计算。
 */
public enum SolarTerm {
    // 春（立春 ~ 谷雨）
    LICHUN(SeasonType.SPRING, 0),
    YUSHUI(SeasonType.SPRING, 1),
    JINGZHE(SeasonType.SPRING, 2),
    CHUNFEN(SeasonType.SPRING, 3),
    QINGMING(SeasonType.SPRING, 4),
    GUYU(SeasonType.SPRING, 5),

    // 夏（立夏 ~ 大暑）
    LIXIA(SeasonType.SUMMER, 0),
    XIAOMAN(SeasonType.SUMMER, 1),
    MANGZHONG(SeasonType.SUMMER, 2),
    XIAZHI(SeasonType.SUMMER, 3),
    XIAOSHU(SeasonType.SUMMER, 4),
    DASHU(SeasonType.SUMMER, 5),

    // 秋（立秋 ~ 霜降）
    LIQIU(SeasonType.AUTUMN, 0),
    CHUSHU(SeasonType.AUTUMN, 1),
    BAILU(SeasonType.AUTUMN, 2),
    QIUFEN(SeasonType.AUTUMN, 3),
    HANLU(SeasonType.AUTUMN, 4),
    SHUANGJIANG(SeasonType.AUTUMN, 5),

    // 冬（立冬 ~ 大寒）
    LIDONG(SeasonType.WINTER, 0),
    XIAOXUE(SeasonType.WINTER, 1),
    DAXUE(SeasonType.WINTER, 2),
    DONGZHI(SeasonType.WINTER, 3),
    XIAOHAN(SeasonType.WINTER, 4),
    DAHAN(SeasonType.WINTER, 5);

    private final SeasonType season;
    private final int indexInSeason; // 0..5

    SolarTerm(SeasonType season, int indexInSeason) {
        this.season = season;
        this.indexInSeason = indexInSeason;
    }

    public SeasonType season() {
        return season;
    }

    public int indexInSeason() {
        return indexInSeason;
    }

    /** 0..23（按 SPRING/SUMMER/AUTUMN/WINTER 的季节顺序）。 */
    public int indexInYear() {
        int seasonBase = switch (season) {
            case SPRING -> 0;
            case SUMMER -> 6;
            case AUTUMN -> 12;
            case WINTER -> 18;
        };
        return seasonBase + indexInSeason;
    }

    /** 用于配置与 i18n key：例如 {@code guyu}。 */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SolarTerm bySeasonIndex(SeasonType season, int indexInSeason) {
        int idx = Math.max(0, Math.min(5, indexInSeason));
        for (SolarTerm t : values()) {
            if (t.season == season && t.indexInSeason == idx) return t;
        }
        // should never happen
        return switch (season) {
            case SPRING -> LICHUN;
            case SUMMER -> LIXIA;
            case AUTUMN -> LIQIU;
            case WINTER -> LIDONG;
        };
    }
}

