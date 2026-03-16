package com.github.cinnaio.natureEngine.command;

import com.github.cinnaio.natureEngine.api.EnvironmentAPI;
import com.github.cinnaio.natureEngine.api.SeasonAPI;
import com.github.cinnaio.natureEngine.api.WeatherAPI;
import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.season.visual.SeasonVisualizer;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
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

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§a[NatureEngine] 可用子命令:");
        sender.sendMessage("§e/ne debug §7- 显示当前世界季节、天气与环境信息");
        sender.sendMessage("§e/ne season info §7- 显示当前世界季节信息");
        sender.sendMessage("§e/ne season next §7- 切换到下一季节");
        sender.sendMessage("§e/ne season set <spring|summer|autumn|winter> §7- 设置当前世界季节");
        sender.sendMessage("§e/ne season clear §7- 清除手动季节覆盖");
        sender.sendMessage("§e/ne season apply §7- 重新应用当前季节的视觉效果（biome tint）");
        sender.sendMessage("§e/ne season restore §7- 恢复玩家周围区域的原始 biome");
    }

    private boolean handleDebug(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该调试命令只能由玩家执行。");
            return true;
        }
        Player player = (Player) sender;
        World world = player.getWorld();

        var season = SeasonAPI.getCurrentSeason(world);
        double progress = SeasonAPI.getSeasonProgress(world);
        var weather = WeatherAPI.getCurrentWeather(world);
        EnvironmentContext env = EnvironmentAPI.getContext(player.getLocation().getBlock());

        player.sendMessage("§a[NatureEngine Debug]");
        player.sendMessage("季节: " + season + " (" + String.format("%.1f", progress * 100) + "%)");
        player.sendMessage("天气: " + weather);
        player.sendMessage(String.format("环境: 温度=%.1f, 湿度=%.2f, 土壤湿度=%.2f, 光照=%d",
                env.getTemperature(),
                env.getHumidity(),
                env.getSoilMoisture(),
                env.getLightLevel()
        ));
        return true;
    }

    private boolean handleSeason(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return true;
        }
        Player player = (Player) sender;
        World world = player.getWorld();

        if (args.length == 0 || "info".equalsIgnoreCase(args[0])) {
            return handleSeasonInfo(player, world);
        }

        if (!sender.isOp()) {
            sender.sendMessage("你没有权限控制季节。");
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

    private boolean handleSeasonInfo(Player player, World world) {
        var season = SeasonAPI.getCurrentSeason(world);
        double progress = SeasonAPI.getSeasonProgress(world);
        boolean overridden = SeasonAPI.hasOverride(world);

        player.sendMessage("§a[NatureEngine] 季节信息");
        player.sendMessage("当前世界: " + world.getName());
        player.sendMessage("季节: " + season + " (" + String.format("%.1f", progress * 100) + "%)");
        player.sendMessage("季节覆盖状态: " + (overridden ? "§c已覆盖" : "§a自然推进"));
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
        player.sendMessage("§a已将当前世界季节设置为: " + next + "（手动覆盖）");
        SeasonVisualizer visualizer = SERVICES.get(SeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueApplyAround(player, next);
        }
        return true;
    }

    private boolean handleSeasonSet(Player player, World world, String[] args) {
        if (args.length < 2) {
            player.sendMessage("用法: /ne season set <spring|summer|autumn|winter>");
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
                player.sendMessage("未知季节: " + name + "，可用: spring, summer, autumn, winter");
                return true;
        }
        SeasonAPI.setSeasonOverride(world, target);
        player.sendMessage("§a已将当前世界季节设置为: " + target + "（手动覆盖）");
        SeasonVisualizer visualizer = SERVICES.get(SeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueApplyAround(player, target);
        }
        return true;
    }

    private boolean handleSeasonClear(Player player, World world) {
        SeasonAPI.clearSeasonOverride(world);
        player.sendMessage("§a已清除当前世界的季节覆盖，恢复自然季节推进。");
        SeasonVisualizer visualizer = SERVICES.get(SeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueRestoreAround(player);
        }
        return true;
    }

    private boolean handleSeasonApply(Player player, World world) {
        SeasonType current = SeasonAPI.getCurrentSeason(world);
        SeasonVisualizer visualizer = SERVICES.get(SeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueApplyAround(player, current);
            player.sendMessage("§a已对你周围区域重新应用季节视觉: " + current);
        } else {
            player.sendMessage("§c视觉系统未启用或未初始化。");
        }
        return true;
    }

    private boolean handleSeasonRestore(Player player, World world) {
        SeasonVisualizer visualizer = SERVICES.get(SeasonVisualizer.class);
        if (visualizer != null) {
            visualizer.enqueueRestoreAround(player);
            player.sendMessage("§a已开始恢复你周围区域的原始 biome（渐进式）。");
        } else {
            player.sendMessage("§c视觉系统未启用或未初始化。");
        }
        return true;
    }
}

