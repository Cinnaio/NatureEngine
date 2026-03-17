package com.github.cinnaio.natureEngine.core.agriculture.season;

import com.github.cinnaio.natureEngine.engine.config.SeasonConfigView;
import com.github.cinnaio.natureEngine.engine.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

/** 季节变化提示（目前用于 /ne season next|set|clear）。 */
public final class SeasonNotifier {

    private final SeasonConfigView config;

    public SeasonNotifier(SeasonConfigView config) {
        this.config = config;
    }

    public void notifySeasonChanged(World world, SeasonType season) {
        if (!config.isNotifyEnabled()) return;

        String mode = config.getNotifyMode();
        if (mode == null) mode = "actionbar";
        mode = mode.toLowerCase(Locale.ROOT);

        String msg = config.getNotifyMessageTemplate();
        if (msg == null) msg = "&a季节已切换为 &e{season}";
        // 占位符：
        // - {season}: 中文季节名（默认）
        // - {season_en}: 枚举名（SPRING/SUMMER/AUTUMN/WINTER）
        msg = msg.replace("{season}", toDisplayName(season));
        msg = msg.replace("{season_en}", season.name());

        Component c = Text.parse(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline()) continue;
            if (!p.getWorld().getUID().equals(world.getUID())) continue;
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

    private String toDisplayName(SeasonType season) {
        return switch (season) {
            case SPRING -> "春";
            case SUMMER -> "夏";
            case AUTUMN -> "秋";
            case WINTER -> "冬";
        };
    }
}

