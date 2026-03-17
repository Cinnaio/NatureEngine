package com.github.cinnaio.natureEngine.core.agriculture.season;

import com.github.cinnaio.natureEngine.engine.config.SeasonConfigView;
import com.github.cinnaio.natureEngine.engine.text.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/** 季节变化提示（目前用于 /ne season next|set|clear）。 */
public final class SeasonNotifier {

    private final SeasonConfigView config;
    private final I18n i18n;

    public SeasonNotifier(SeasonConfigView config, I18n i18n) {
        this.config = config;
        this.i18n = i18n;
    }

    public void notifySeasonChanged(World world, SeasonType season) {
        if (!config.isNotifyEnabled()) return;

        String mode = config.getNotifyMode();
        if (mode == null) mode = "actionbar";
        mode = mode.toLowerCase(Locale.ROOT);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline()) continue;
            if (!p.getWorld().getUID().equals(world.getUID())) continue;
            String seasonKey = "season.display." + season.name().toLowerCase(Locale.ROOT);
            String seasonName = i18n.trRaw(p, seasonKey);
            Component c = i18n.tr(p, "season.notify-actionbar", Map.of(
                    "season", seasonName,
                    "season_en", season.name()
            ));
            switch (mode) {
                case "chat":
                    p.sendMessage(c);
                    break;
                case "both":
                    p.sendMessage(c);
                    p.sendActionBar(c);
                    break;
                case "actionbar":
                default:
                    p.sendActionBar(c);
                    break;
            }
        }
    }
}

