package com.github.cinnaio.natureEngine.core.agriculture.season;

import com.github.cinnaio.natureEngine.engine.config.SeasonConfigView;
import com.github.cinnaio.natureEngine.engine.text.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/** 季节变化提示（用于 /ne season next|set|clear）。 */
public final class SeasonNotifier {

    private final SeasonManager seasonManager;
    private final SeasonConfigView config;
    private final I18n i18n;

    public SeasonNotifier(SeasonManager seasonManager, SeasonConfigView config, I18n i18n) {
        this.seasonManager = seasonManager;
        this.config = config;
        this.i18n = i18n;
    }

    public void notifySeasonChanged(World world, SeasonType season) {
        if (!config.isNotifyEnabled()) return;

        long daysLeft = seasonManager.getDaysUntilNextSeason(world);
        long duration = seasonManager.getCurrentSeasonLengthDays(world);
        SeasonType nextSeason = seasonManager.getNextSeasonType(world);
        SeasonSettings settings = config.getSettings(season);

        String mode = config.getNotifyMode();
        if (mode == null) mode = "actionbar";
        mode = mode.toLowerCase(Locale.ROOT);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline()) continue;
            if (!p.getWorld().getUID().equals(world.getUID())) continue;

            String seasonName = i18n.trRaw(p, "season.display." + season.name().toLowerCase(Locale.ROOT));
            String nextSeasonName = i18n.trRaw(p, "season.display." + nextSeason.name().toLowerCase(Locale.ROOT));
            String features = buildFeatures(p, settings);

            Map<String, String> placeholders = Map.of(
                    "season", seasonName,
                    "season_en", season.name(),
                    "next_season", nextSeasonName,
                    "days_left", String.valueOf(daysLeft),
                    "duration", String.valueOf(duration),
                    "features", features
            );

            Component actionbarMsg = i18n.tr(p, "season.notify-actionbar", placeholders);
            switch (mode) {
                case "chat":
                    p.sendMessage(i18n.tr(p, "season.notify-chat-line1", placeholders));
                    p.sendMessage(i18n.tr(p, "season.notify-chat-line2", placeholders));
                    p.sendMessage(i18n.tr(p, "season.notify-chat-features", placeholders));
                    break;
                case "both":
                    p.sendMessage(i18n.tr(p, "season.notify-chat-line1", placeholders));
                    p.sendMessage(i18n.tr(p, "season.notify-chat-line2", placeholders));
                    p.sendMessage(i18n.tr(p, "season.notify-chat-features", placeholders));
                    p.sendActionBar(actionbarMsg);
                    break;
                case "actionbar":
                default:
                    p.sendActionBar(actionbarMsg);
                    break;
            }
        }
    }

    private String buildFeatures(Player p, SeasonSettings settings) {
        String growthStr = i18n.trRaw(p, "season.features-growth")
                .replace("{growth}", String.format("%.1f", settings.getGrowthMultiplier()));
        String yieldStr = i18n.trRaw(p, "season.features-yield")
                .replace("{yield}", String.format("%.1f", settings.getYieldMultiplier()));
        String witherPart = settings.isEasyToWither()
                ? " · " + i18n.trRaw(p, "season.features-wither")
                : "";
        return growthStr + " " + yieldStr + witherPart;
    }
}
