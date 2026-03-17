package com.github.cinnaio.natureEngine.command;

import com.github.cinnaio.natureEngine.api.EnvironmentAPI;
import com.github.cinnaio.natureEngine.api.SeasonAPI;
import com.github.cinnaio.natureEngine.api.WeatherAPI;
import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonNotifier;
import com.github.cinnaio.natureEngine.core.agriculture.season.visual.PacketSeasonVisualizer;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.engine.config.ConfigManager;
import com.github.cinnaio.natureEngine.engine.text.I18n;
import com.github.cinnaio.natureEngine.engine.text.Text;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.Locale;

public final class NeRootCommand extends Command {

    private static final ServiceLocator SERVICES = ServiceLocator.getInstance();

    public NeRootCommand() {
        super("ne");
        this.setDescription("NatureEngine main command");
        this.setUsage("/ne <subcommand>");
        this.setAliases(java.util.Arrays.asList("natureengine"));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("debug".equals(sub)) {
            return handleDebug(sender);
        }
        if ("season".equals(sub)) {
            return handleSeason(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if ("reload".equals(sub)) {
            return handleReload(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        if (sender instanceof Player p) {
            I18n i18n = SERVICES.get(I18n.class);
            if (i18n != null) {
                sender.sendMessage(i18n.tr(p, "command.ne.help-title"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-debug"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-season-info"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-season-next"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-season-set"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-season-clear"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-season-apply"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-reload"));
                return;
            }
        }
        // fallback
        sender.sendMessage(Text.parse("<color:#B8C7FF>[NatureEngine]</> <color:#A9B3C3>Commands:</>"));
    }

    private boolean handleDebug(CommandSender sender) {
        if (!(sender instanceof Player)) {
            if (sender instanceof Player p) {
                I18n i18n = SERVICES.get(I18n.class);
                if (i18n != null) {
                    sender.sendMessage(i18n.tr(p, "common.only-player"));
                    return true;
                }
            }
            sender.sendMessage(Text.parse("<color:#FFB4B4>该命令只能由玩家执行。</>"));
            return true;
        }
        Player player = (Player) sender;
        World world = player.getWorld();

        var season = SeasonAPI.getCurrentSeason(world);
        double progress = SeasonAPI.getSeasonProgress(world);
        var weather = WeatherAPI.getCurrentWeather(world);
        EnvironmentContext env = EnvironmentAPI.getContext(player.getLocation().getBlock());

        I18n i18n = SERVICES.get(I18n.class);
        if (i18n != null) {
            String seasonName = i18n.trRaw(player, "season.display." + season.name().toLowerCase(Locale.ROOT));
            player.sendMessage(i18n.tr(player, "debug.title"));
            player.sendMessage(i18n.tr(player, "debug.season-line", Map.of(
                    "season", seasonName,
                    "progress", String.format("%.1f", progress * 100)
            )));
            player.sendMessage(i18n.tr(player, "debug.weather-line", Map.of("weather", weather.name())));
            player.sendMessage(i18n.tr(player, "debug.env-line", Map.of(
                    "temp", String.format("%.1f", env.getTemperature()),
                    "humidity", String.format("%.2f", env.getHumidity()),
                    "soil", String.format("%.2f", env.getSoilMoisture()),
                    "light", String.valueOf(env.getLightLevel())
            )));
        } else {
            player.sendMessage(Text.parse("<color:#B8C7FF>[NatureEngine Debug]"));
        }
        return true;
    }

    private boolean handleSeason(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Text.parse("<color:#FFB4B4>该命令只能由玩家执行。</>"));
            return true;
        }
        Player player = (Player) sender;
        World world = player.getWorld();

        if (args.length == 0 || "info".equalsIgnoreCase(args[0])) {
            return handleSeasonInfo(player, world);
        }

        if (!sender.isOp()) {
            I18n i18n = SERVICES.get(I18n.class);
            if (i18n != null) {
                sender.sendMessage(i18n.tr(player, "season.no-permission-control"));
            } else {
                sender.sendMessage(Text.parse("<color:#FFB4B4>你没有权限控制季节。</>"));
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "next":
                return handleSeasonNext(player, world);
            case "set":
                return handleSeasonSet(player, world, args);
            case "clear":
                return handleSeasonClear(player, world);
            case "apply":
                return handleSeasonApply(player, world);
            case "restore":
                return handleSeasonRestore(player, world);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            if (sender instanceof Player p) {
                I18n i18n = SERVICES.get(I18n.class);
                if (i18n != null) {
                    sender.sendMessage(i18n.tr(p, "common.no-permission"));
                    return true;
                }
            }
            sender.sendMessage(Text.parse("<color:#FFB4B4>你没有权限执行该命令。</>"));
            return true;
        }
        String module = (args.length >= 1) ? args[0] : "all";
        ConfigManager cm = SERVICES.get(ConfigManager.class);
        if (cm == null) {
            if (sender instanceof Player p) {
                I18n i18n = SERVICES.get(I18n.class);
                if (i18n != null) {
                    sender.sendMessage(i18n.tr(p, "common.config-not-ready"));
                    return true;
                }
            }
            sender.sendMessage(Text.parse("<color:#FFB4B4>配置系统未初始化。</>"));
            return true;
        }
        cm.reloadModule(module);
        if (sender instanceof Player p) {
            I18n i18n = SERVICES.get(I18n.class);
            if (i18n != null) {
                sender.sendMessage(i18n.tr(p, "common.reloaded", Map.of("module", module)));
            } else {
                sender.sendMessage(Text.parse("<color:#B7F0C2>已重载配置模块：</> <color:#FFE2A9>" + module + "</>"));
            }
        } else {
            sender.sendMessage(Text.parse("<color:#B7F0C2>Reloaded module:</> <color:#FFE2A9>" + module + "</>"));
        }

        // 重载后即时应用
        String m = module.toLowerCase(Locale.ROOT);
        if ("all".equals(m) || "visual".equals(m)) {
            PacketSeasonVisualizer visualizer = SERVICES.get(PacketSeasonVisualizer.class);
            if (visualizer != null) {
                visualizer.enqueueApplyAllSilent();
            }
        }
        if ("all".equals(m) || "weather".equals(m)) {
            WeatherManager wm = SERVICES.get(WeatherManager.class);
            if (wm != null) {
                wm.reloadNow();
            }
        }
        return true;
    }

    private boolean handleSeasonInfo(Player player, World world) {
        var season = SeasonAPI.getCurrentSeason(world);
        double progress = SeasonAPI.getSeasonProgress(world);
        boolean overridden = SeasonAPI.hasOverride(world);

        I18n i18n = SERVICES.get(I18n.class);
        if (i18n != null) {
            String seasonName = i18n.trRaw(player, "season.display." + season.name().toLowerCase(Locale.ROOT));
            String status = overridden ? i18n.trRaw(player, "season.override-status-on") : i18n.trRaw(player, "season.override-status-off");
            player.sendMessage(i18n.tr(player, "season.info-title"));
            player.sendMessage(i18n.tr(player, "season.world-line", Map.of("world", world.getName())));
            player.sendMessage(i18n.tr(player, "season.season-line", Map.of(
                    "season", seasonName,
                    "progress", String.format("%.1f", progress * 100)
            )));
            player.sendMessage(i18n.tr(player, "season.override-status-line", Map.of("status", status)));
        }
        return true;
    }

    private boolean handleSeasonNext(Player player, World world) {
        SeasonType current = SeasonAPI.getCurrentSeason(world);
        SeasonType next;
        switch (current) {
            case SPRING:
                next = SeasonType.SUMMER;
                break;
            case SUMMER:
                next = SeasonType.AUTUMN;
                break;
            case AUTUMN:
                next = SeasonType.WINTER;
                break;
            case WINTER:
            default:
                next = SeasonType.SPRING;
                break;
        }
        SeasonAPI.setSeasonOverride(world, next);
        I18n i18n = SERVICES.get(I18n.class);
        if (i18n != null) {
            String seasonName = i18n.trRaw(player, "season.display." + next.name().toLowerCase(Locale.ROOT));
            player.sendMessage(i18n.tr(player, "season.override-set", Map.of("season", seasonName)));
        }
        SeasonNotifier notifier = SERVICES.get(SeasonNotifier.class);
        if (notifier != null) notifier.notifySeasonChanged(world, next);
        PacketSeasonVisualizer visualizer = SERVICES.get(PacketSeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueApply(player);
        }
        return true;
    }

    private boolean handleSeasonSet(Player player, World world, String[] args) {
        if (args.length < 2) {
            I18n i18n = SERVICES.get(I18n.class);
            if (i18n != null) player.sendMessage(i18n.tr(player, "season.set-usage"));
            return true;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        SeasonType target;
        switch (name) {
            case "spring":
                target = SeasonType.SPRING;
                break;
            case "summer":
                target = SeasonType.SUMMER;
                break;
            case "autumn":
            case "fall":
                target = SeasonType.AUTUMN;
                break;
            case "winter":
                target = SeasonType.WINTER;
                break;
            default:
                I18n i18n = SERVICES.get(I18n.class);
                if (i18n != null) player.sendMessage(i18n.tr(player, "season.unknown-season", Map.of("input", name)));
                return true;
        }
        SeasonAPI.setSeasonOverride(world, target);
        I18n i18n = SERVICES.get(I18n.class);
        if (i18n != null) {
            String seasonName = i18n.trRaw(player, "season.display." + target.name().toLowerCase(Locale.ROOT));
            player.sendMessage(i18n.tr(player, "season.override-set", Map.of("season", seasonName)));
        }
        SeasonNotifier notifier = SERVICES.get(SeasonNotifier.class);
        if (notifier != null) notifier.notifySeasonChanged(world, target);
        PacketSeasonVisualizer visualizer = SERVICES.get(PacketSeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueApply(player);
        }
        return true;
    }

    private boolean handleSeasonClear(Player player, World world) {
        SeasonAPI.clearSeasonOverride(world);
        player.sendMessage(Text.parse("&a已清除当前世界的季节覆盖，恢复自然季节推进。"));
        player.sendMessage(Text.parse("&7提示：发包视觉会在区块重新发送时自然恢复。"));
        SeasonNotifier notifier = SERVICES.get(SeasonNotifier.class);
        if (notifier != null) notifier.notifySeasonChanged(world, SeasonAPI.getCurrentSeason(world));
        return true;
    }

    private boolean handleSeasonApply(Player player, World world) {
        SeasonType current = SeasonAPI.getCurrentSeason(world);
        PacketSeasonVisualizer visualizer = SERVICES.get(PacketSeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueApply(player);
            I18n i18n = SERVICES.get(I18n.class);
            if (i18n != null) {
                String seasonName = i18n.trRaw(player, "season.display." + current.name().toLowerCase(Locale.ROOT));
                player.sendMessage(i18n.tr(player, "visual.refresh-started-season", Map.of("season", seasonName)));
            }
        } else {
            I18n i18n = SERVICES.get(I18n.class);
            if (i18n != null) player.sendMessage(i18n.tr(player, "visual.not-ready"));
        }
        return true;
    }

    private boolean handleSeasonRestore(Player player, World world) {
        I18n i18n = SERVICES.get(I18n.class);
        if (i18n != null) player.sendMessage(i18n.tr(player, "visual.restore-hint"));
        return true;
    }
}

