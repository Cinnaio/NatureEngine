package com.github.cinnaio.natureEngine.integration.placeholderapi;

import com.github.cinnaio.natureEngine.api.EnvironmentAPI;
import com.github.cinnaio.natureEngine.api.SeasonAPI;
import com.github.cinnaio.natureEngine.api.WeatherAPI;
import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.season.SolarTerm;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.engine.text.I18n;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * PlaceholderAPI 扩展，提供季节、天气、环境相关占位符。
 * <p>
 * 占位符：
 * <ul>
 *   <li>%natureengine_season% - 季节枚举名</li>
 *   <li>%natureengine_season_display% - 本地化季节显示名</li>
 *   <li>%natureengine_weather% - 天气枚举名</li>
 *   <li>%natureengine_weather_display% - 本地化天气显示名</li>
 *   <li>%natureengine_season_progress% - 季节进度 0~100</li>
 *   <li>%natureengine_days_until_next_season% - 距下一季节天数</li>
 *   <li>%natureengine_next_season% - 下一季节枚举名</li>
 *   <li>%natureengine_next_season_display% - 下一季节本地化名</li>
 *   <li>%natureengine_season_duration% - 当前季节持续天数</li>
 *   <li>%natureengine_temperature% - 玩家所在位置温度</li>
 *   <li>%natureengine_humidity% - 湿度</li>
 *   <li>%natureengine_soil_moisture% - 土壤湿度</li>
 *   <li>%natureengine_light% - 光照等级</li>
 * </ul>
 */
public final class NatureEngineExpansion extends PlaceholderExpansion {

    private final Plugin plugin;

    public NatureEngineExpansion(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "natureengine";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offline, @NotNull String params) {
        if (offline == null || !offline.isOnline()) {
            return resolveWithoutPlayer(params);
        }
        Player player = offline.getPlayer();
        if (player == null) return resolveWithoutPlayer(params);

        String code = getLocaleCode(player);
        return resolve(player.getWorld(), player, code, params);
    }

    private String resolveWithoutPlayer(String params) {
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "season", "season_display", "weather", "weather_display",
                 "season_progress", "season_duration", "days_until_next_season",
                 "next_season", "next_season_display",
                 "solar_term", "solar_term_display", "solar_term_progress", "days_until_next_solar_term",
                 "world_day", "year", "day_in_year", "year_length",
                 "temperature", "humidity", "soil_moisture", "light" -> "";
            default -> null;
        };
    }

    private String resolve(org.bukkit.World world, Player player, String localeCode, String params) {
        I18n i18n = ServiceLocator.getInstance().get(I18n.class);
        if (i18n == null) {
            return resolveFallback(world, player, params);
        }

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "season" -> SeasonAPI.getCurrentSeason(world).name();
            case "season_display" -> i18n.trRaw(localeCode, "season.display." + SeasonAPI.getCurrentSeason(world).name().toLowerCase(Locale.ROOT));
            case "weather" -> WeatherAPI.getCurrentWeather(world).name();
            case "weather_display" -> i18n.trRaw(localeCode, "weather.display." + WeatherAPI.getCurrentWeather(world).name().toLowerCase(Locale.ROOT));
            case "season_progress" -> String.format("%.1f", SeasonAPI.getSeasonProgress(world) * 100);
            case "season_duration" -> String.valueOf(SeasonAPI.getCurrentSeasonLengthDays(world));
            case "days_until_next_season" -> String.valueOf(SeasonAPI.getDaysUntilNextSeason(world));
            case "next_season" -> SeasonAPI.getNextSeasonType(world).name();
            case "next_season_display" -> i18n.trRaw(localeCode, "season.display." + SeasonAPI.getNextSeasonType(world).name().toLowerCase(Locale.ROOT));
            case "solar_term" -> {
                SolarTerm t = SeasonAPI.getCurrentSolarTerm(world);
                yield t != null ? t.name() : "";
            }
            case "solar_term_display" -> {
                SolarTerm t = SeasonAPI.getCurrentSolarTerm(world);
                yield t != null ? i18n.trRaw(localeCode, "solar-term." + t.key()) : "";
            }
            case "solar_term_progress" -> String.format("%.1f", SeasonAPI.getSolarTermProgress(world) * 100);
            case "days_until_next_solar_term" -> String.valueOf(SeasonAPI.getDaysUntilNextSolarTerm(world));
            case "world_day" -> String.valueOf(SeasonAPI.getWorldDay(world) + 1L);
            case "year" -> String.valueOf(SeasonAPI.getYear(world));
            case "day_in_year" -> String.valueOf(SeasonAPI.getDayInYear(world));
            case "year_length" -> String.valueOf(SeasonAPI.getYearLengthDays());
            case "temperature" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.format("%.1f", ctx.getTemperature());
            }
            case "humidity" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.format("%.2f", ctx.getHumidity());
            }
            case "soil_moisture" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.format("%.2f", ctx.getSoilMoisture());
            }
            case "light" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.valueOf(ctx.getLightLevel());
            }
            default -> null;
        };
    }

    private String resolveFallback(org.bukkit.World world, Player player, String params) {
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "season" -> SeasonAPI.getCurrentSeason(world).name();
            case "season_display" -> {
                SeasonType s = SeasonAPI.getCurrentSeason(world);
                yield s.name().charAt(0) + s.name().substring(1).toLowerCase(Locale.ROOT);
            }
            case "weather" -> WeatherAPI.getCurrentWeather(world).name();
            case "weather_display" -> {
                WeatherType w = WeatherAPI.getCurrentWeather(world);
                yield w.name().charAt(0) + w.name().substring(1).toLowerCase(Locale.ROOT);
            }
            case "season_progress" -> String.format("%.1f", SeasonAPI.getSeasonProgress(world) * 100);
            case "season_duration" -> String.valueOf(SeasonAPI.getCurrentSeasonLengthDays(world));
            case "days_until_next_season" -> String.valueOf(SeasonAPI.getDaysUntilNextSeason(world));
            case "next_season" -> SeasonAPI.getNextSeasonType(world).name();
            case "next_season_display" -> {
                SeasonType next = SeasonAPI.getNextSeasonType(world);
                yield next.name().charAt(0) + next.name().substring(1).toLowerCase(Locale.ROOT);
            }
            case "solar_term" -> {
                SolarTerm t = SeasonAPI.getCurrentSolarTerm(world);
                yield t != null ? t.name() : "";
            }
            case "solar_term_display" -> {
                SolarTerm t = SeasonAPI.getCurrentSolarTerm(world);
                yield t != null ? t.key() : "";
            }
            case "solar_term_progress" -> String.format("%.1f", SeasonAPI.getSolarTermProgress(world) * 100);
            case "days_until_next_solar_term" -> String.valueOf(SeasonAPI.getDaysUntilNextSolarTerm(world));
            case "world_day" -> String.valueOf(SeasonAPI.getWorldDay(world) + 1L);
            case "year" -> String.valueOf(SeasonAPI.getYear(world));
            case "day_in_year" -> String.valueOf(SeasonAPI.getDayInYear(world));
            case "year_length" -> String.valueOf(SeasonAPI.getYearLengthDays());
            case "temperature" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.format("%.1f", ctx.getTemperature());
            }
            case "humidity" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.format("%.2f", ctx.getHumidity());
            }
            case "soil_moisture" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.format("%.2f", ctx.getSoilMoisture());
            }
            case "light" -> {
                EnvironmentContext ctx = EnvironmentAPI.getContext(player.getLocation().getBlock());
                yield String.valueOf(ctx.getLightLevel());
            }
            default -> null;
        };
    }

    private String getLocaleCode(Player player) {
        try {
            java.util.Locale loc = player.locale();
            if (loc != null) {
                String s = loc.toString().toLowerCase(Locale.ROOT).replace('-', '_');
                if (s.startsWith("zh")) return "zh_cn";
                if (s.startsWith("en")) return "en_us";
            }
        } catch (Throwable ignored) {
        }
        return "zh_cn";
    }
}
