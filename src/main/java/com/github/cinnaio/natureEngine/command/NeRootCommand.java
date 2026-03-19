package com.github.cinnaio.natureEngine.command;

import com.github.cinnaio.natureEngine.api.EnvironmentAPI;
import com.github.cinnaio.natureEngine.api.SeasonAPI;
import com.github.cinnaio.natureEngine.api.WeatherAPI;
import com.github.cinnaio.natureEngine.bootstrap.ServiceLocator;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropManager;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropType;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonNotifier;
import com.github.cinnaio.natureEngine.core.agriculture.season.visual.PacketSeasonVisualizer;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherType;
import com.github.cinnaio.natureEngine.core.agriculture.weather.WeatherManager;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthDebugInfo;
import com.github.cinnaio.natureEngine.core.agriculture.growth.GrowthContext;
import com.github.cinnaio.natureEngine.core.environment.EnvironmentContext;
import com.github.cinnaio.natureEngine.engine.config.ConfigManager;
import com.github.cinnaio.natureEngine.engine.config.CropConfigView;
import com.github.cinnaio.natureEngine.engine.text.I18n;
import com.github.cinnaio.natureEngine.engine.text.Text;
import com.github.cinnaio.natureEngine.integration.craftengine.CraftEngineTrackService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        // 权限策略：所有 /ne 子命令默认仅 OP 可用
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

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("debug".equals(sub)) {
            return handleDebug(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if ("season".equals(sub)) {
            return handleSeason(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if ("reload".equals(sub)) {
            return handleReload(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if ("sim".equals(sub)) {
            return handleSim(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if ("crop".equals(sub)) {
            return handleCrop(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        sendHelp(sender);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!sender.isOp()) return Collections.emptyList();
        if (args == null) return Collections.emptyList();
        if (args.length == 0) return Collections.emptyList();

        String a0 = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            List<String> root = new ArrayList<>();
            root.add("debug");
            root.add("sim");
            root.add("season");
            root.add("reload");
            root.add("crop");
            return filterPrefix(root, a0);
        }

        // 子命令补全
        switch (a0) {
            case "debug" -> {
                if (args.length == 2) {
                    return filterPrefix(List.of("crop"), lower(args[1]));
                }
            }
            case "sim" -> {
                if (args.length == 2) {
                    return filterPrefix(List.of("crop"), lower(args[1]));
                }
            }
            case "season" -> {
                if (args.length == 2) {
                    List<String> subs = new ArrayList<>();
                    subs.add("info");
                    if (sender.isOp()) {
                        subs.add("next");
                        subs.add("set");
                        subs.add("clear");
                        subs.add("apply");
                        subs.add("restore");
                    }
                    return filterPrefix(subs, lower(args[1]));
                }
                if (args.length == 3 && sender.isOp() && "set".equalsIgnoreCase(args[1])) {
                    List<String> seasons = new ArrayList<>();
                    for (SeasonType t : SeasonType.values()) seasons.add(t.name());
                    return filterPrefix(seasons, lower(args[2]));
                }
            }
            case "reload" -> {
                if (args.length == 2) {
                    return filterPrefix(List.of(
                            "all",
                            "season",
                            "seasons",
                            "growth",
                            "debug",
                            "config",
                            "weather",
                            "visual",
                            "crop",
                            "crops",
                            "environment",
                            "env",
                            "biome"
                            , "biomes"
                    ), lower(args[1]));
                }
            }
            case "crop" -> {
                if (args.length == 2) {
                    return filterPrefix(List.of("randomTickSpeed"), lower(args[1]));
                }
                if (args.length == 3 && "randomtickspeed".equalsIgnoreCase(args[1])) {
                    // 常用档位：0=禁用自然生长触发；3=默认；更大=更快
                    return filterPrefix(List.of("0", "1", "3", "10", "30", "100"), lower(args[2]));
                }
            }
            default -> {
            }
        }
        return Collections.emptyList();
    }

    private boolean handleCrop(CommandSender sender, String[] args) {
        I18n i18n = (sender instanceof Player p) ? SERVICES.get(I18n.class) : null;
        if (args == null || args.length == 0) {
            if (sender instanceof Player p && i18n != null) {
                sender.sendMessage(i18n.tr(p, "command.ne.crop-usage"));
            } else {
                sender.sendMessage(Text.parse("<color:#B8C7FF>用法: /ne crop randomTickSpeed [值]</>"));
            }
            return true;
        }
        if (!"randomtickspeed".equalsIgnoreCase(args[0])) {
            if (sender instanceof Player p && i18n != null) {
                sender.sendMessage(i18n.tr(p, "command.ne.crop-usage"));
            } else {
                sender.sendMessage(Text.parse("<color:#B8C7FF>用法: /ne crop randomTickSpeed [值]</>"));
            }
            return true;
        }
        ConfigManager cm = SERVICES.get(ConfigManager.class);
        if (cm == null) {
            sender.sendMessage(Text.parse("<color:#FFB4B4>ConfigManager 未初始化。</>"));
            return true;
        }

        int current = cm.getGrowthConfig().getRandomTickSpeed();
        if (args.length == 1) {
            if (sender instanceof Player p && i18n != null) {
                sender.sendMessage(i18n.tr(p, "command.ne.crop-rts-get", Map.of("value", String.valueOf(current))));
            } else {
                sender.sendMessage(Text.parse("<color:#B8C7FF>[NatureEngine] 插件 randomTickSpeed = <color:#FFFFFF>" + current + "</></>"));
            }
            return true;
        }

        int v;
        try {
            v = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            if (sender instanceof Player p && i18n != null) {
                sender.sendMessage(i18n.tr(p, "command.ne.crop-rts-invalid"));
            } else {
                sender.sendMessage(Text.parse("<color:#FFB4B4>请输入整数，例如: /ne crop randomTickSpeed 3</>"));
            }
            return true;
        }
        v = Math.max(0, v);
        boolean ok = cm.setPluginRandomTickSpeed(v);
        if (ok) {
            if (sender instanceof Player p && i18n != null) {
                sender.sendMessage(i18n.tr(p, "command.ne.crop-rts-set", Map.of("value", String.valueOf(v))));
            } else {
                sender.sendMessage(Text.parse("<color:#B8C7FF>[NatureEngine] 插件 randomTickSpeed 已设置为 <color:#FFFFFF>" + v + "</></>"));
            }
        } else {
            sender.sendMessage(Text.parse("<color:#FFB4B4>设置失败：无法保存 growth.yml。</>"));
        }
        return true;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static List<String> filterPrefix(List<String> options, String prefixLower) {
        if (options == null || options.isEmpty()) return Collections.emptyList();
        if (prefixLower == null) prefixLower = "";
        if (prefixLower.isEmpty()) return options;
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o == null) continue;
            if (o.toLowerCase(Locale.ROOT).startsWith(prefixLower)) out.add(o);
        }
        return out;
    }

    private void sendHelp(CommandSender sender) {
        if (sender instanceof Player p) {
            I18n i18n = SERVICES.get(I18n.class);
            if (i18n != null) {
                sender.sendMessage(i18n.tr(p, "command.ne.help-title"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-debug"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-debug-crop"));
                sender.sendMessage(i18n.tr(p, "command.ne.help-sim-crop"));
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

    private boolean handleDebug(CommandSender sender, String[] args) {
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

        // /ne debug crop：仅输出指向目标的作物生长调试
        if (args != null && args.length > 0 && "crop".equalsIgnoreCase(args[0])) {
            return handleDebugCrop(player, true);
        }

        var season = SeasonAPI.getCurrentSeason(world);
        double progress = SeasonAPI.getSeasonProgress(world);
        var solarTerm = SeasonAPI.getCurrentSolarTerm(world);
        double solarProgress = SeasonAPI.getSolarTermProgress(world);
        long solarDays = SeasonAPI.getDaysUntilNextSolarTerm(world);
        long seasonDays = SeasonAPI.getDaysUntilNextSeason(world);
        long worldDay = SeasonAPI.getWorldDay(world) + 1L;
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
            String termName = i18n.trRaw(player, "solar-term." + (solarTerm != null ? solarTerm.key() : "unknown"));
            player.sendMessage(i18n.tr(player, "debug.solar-term-line", Map.of(
                    "term", termName,
                    "progress", String.format("%.1f", solarProgress * 100),
                    "days", String.valueOf(solarDays)
            )));
            player.sendMessage(i18n.tr(player, "debug.day-line", Map.of(
                    "day", String.valueOf(worldDay),
                    "season_days", String.valueOf(seasonDays)
            )));
            String weatherName = i18n.trRaw(player, "weather.display." + weather.name().toLowerCase(Locale.ROOT));
            player.sendMessage(i18n.tr(player, "debug.weather-line", Map.of("weather", weatherName)));
            ConfigManager configManager = SERVICES.get(ConfigManager.class);
            boolean envEnabled = configManager != null
                    && configManager.getEnvironmentConfig() != null
                    && configManager.getEnvironmentConfig().isEnabled();
            java.util.Map<String, String> envArgs = new java.util.HashMap<>();
            envArgs.put("env_enabled", envEnabled ? "true" : "false");
            String envTypeKey = env.getEnvironmentType() != null ? env.getEnvironmentType().name().toLowerCase(Locale.ROOT) : "unknown";
            String envTypeName = i18n.trRaw(player, "environment.type." + envTypeKey);
            envArgs.put("env_type", envTypeName);
            envArgs.put("openness", String.format("%.2f", env.getOpennessScore()));
            envArgs.put("exposure", String.format("%.2f", env.getExposureScore()));
            envArgs.put("open_ratio", String.format("%.2f", env.getOpenRatio()));
            envArgs.put("roof_dy", String.valueOf(env.getRoofDy()));
            envArgs.put("temp", String.format("%.1f", env.getTemperature()));
            envArgs.put("humidity", String.format("%.2f", env.getHumidity()));
            envArgs.put("soil", String.format("%.2f", env.getSoilMoisture()));
            envArgs.put("light", String.valueOf(env.getLightLevel()));
            envArgs.put("sky_light", String.valueOf(env.getSkyLight()));
            envArgs.put("block_light", String.valueOf(env.getBlockLight()));
            envArgs.put("alt_y", String.valueOf(env.getAltitudeY()));
            envArgs.put("biome", env.getBiomeKey() != null
                    ? env.getBiomeKey().toString()
                    : (env.getBiome() != null ? env.getBiome().toString() : "unknown"));
            envArgs.put("biome_group", env.getBiomeGroupId() != null ? env.getBiomeGroupId() : "-");
            player.sendMessage(i18n.tr(player, "debug.env-line", envArgs));

            // /ne debug（默认）附带一行 crop 调试，行为与 /ne debug crop 相同
            handleDebugCrop(player, false);
        } else {
            player.sendMessage(Text.parse("<color:#B8C7FF>[NatureEngine Debug]"));
        }
        return true;
    }

    private boolean handleDebugCrop(Player player, boolean verbose) {
        World world = player.getWorld();
        var season = SeasonAPI.getCurrentSeason(world);
        double progress = SeasonAPI.getSeasonProgress(world);
        var weather = WeatherAPI.getCurrentWeather(world);

        I18n i18n = SERVICES.get(I18n.class);
        if (i18n == null) {
            player.sendMessage(Text.parse("<color:#FFB4B4>i18n 未初始化。</>"));
            return true;
        }

        CropManager cropManager = SERVICES.get(CropManager.class);
        if (cropManager == null) {
            player.sendMessage(Text.parse("<color:#FFB4B4>CropManager 未初始化。</>"));
            return true;
        }

        Block target = player.getTargetBlockExact(6);
        if (target == null) target = player.getLocation().getBlock();
        Location loc = target.getLocation();

        ConfigManager configManager = SERVICES.get(ConfigManager.class);
        double advanceTh = configManager != null ? configManager.getGrowthConfig().getAdvanceThreshold() : 0.8;
        double witherTh = configManager != null ? configManager.getGrowthConfig().getWitherThreshold() : 0.1;
        boolean cropsEnabled = configManager != null && configManager.getCropConfig() != null && configManager.getCropConfig().isGlobalEnabled();

        var cropOpt = cropManager.getCropDataForLocation(loc);
        if (cropOpt.isPresent()) {
            CropType crop = cropOpt.get();
            EnvironmentContext cropEnv = EnvironmentAPI.getContext(target);
            int age = (target.getBlockData() instanceof org.bukkit.block.data.Ageable a) ? a.getAge() : -1;
            int maxAge = (target.getBlockData() instanceof org.bukkit.block.data.Ageable a) ? a.getMaximumAge() : -1;
            GrowthContext ctx = new GrowthContext(
                    loc,
                    crop,
                    Math.max(0, age),
                    season,
                    progress,
                    weather,
                    cropEnv
            );
            var info = cropManager.calculateGrowthDebug(ctx);

            String decision = info.getResult().isShouldWither()
                    ? "WITHER"
                    : (info.getResult().getStageDelta() > 0 ? "ADVANCE" : "BLOCK");
            java.util.Map<String, String> ph = new java.util.HashMap<>();
            ph.put("block", target.getType().name());
            ph.put("crop_id", crop.getId());
            ph.put("crops_enabled", cropsEnabled ? "true" : "false");
            ph.put("crop_enabled", crop.isEnabled() ? "true" : "false");
            ph.put("age", String.valueOf(age));
            ph.put("max_age", String.valueOf(maxAge));
            ph.put("stages", String.valueOf(crop.getStages()));
            ph.put("min_light", String.valueOf(crop.getMinLight()));
            ph.put("opt_temp", String.format("%.1f", crop.getOptimalTemperature()));
            ph.put("temp_tol", String.format("%.1f", crop.getTemperatureTolerance()));
            ph.put("opt_hum", String.format("%.2f", crop.getOptimalHumidity()));
            ph.put("hum_tol", String.format("%.2f", crop.getHumidityTolerance()));
            ph.put("pref_seasons", crop.getPreferredSeasons().toString());
            ph.put("env_temp", String.format("%.1f", cropEnv.getTemperature()));
            ph.put("env_hum", String.format("%.2f", cropEnv.getHumidity()));
            ph.put("env_soil", String.format("%.2f", cropEnv.getSoilMoisture()));
            ph.put("env_light", String.valueOf(cropEnv.getLightLevel()));
            String cropEnvTypeKey = cropEnv.getEnvironmentType() != null ? cropEnv.getEnvironmentType().name().toLowerCase(Locale.ROOT) : "unknown";
            String cropEnvTypeName = i18n.trRaw(player, "environment.type." + cropEnvTypeKey);
            ph.put("env_type", cropEnvTypeName);
            ph.put("openness", String.format("%.2f", cropEnv.getOpennessScore()));
            ph.put("exposure", String.format("%.2f", cropEnv.getExposureScore()));
            ph.put("open_ratio", String.format("%.2f", cropEnv.getOpenRatio()));
            ph.put("roof_dy", String.valueOf(cropEnv.getRoofDy()));
            ph.put("advance_th", String.format("%.2f", advanceTh));
            ph.put("wither_th", String.format("%.2f", witherTh));
            ph.put("decision", decision);
            ph.put("total", String.format("%.3f", info.getTotal()));
            ph.put("tf", String.format("%.2f", info.getTemperatureFactor()));
            ph.put("hf", String.format("%.2f", info.getHumidityFactor()));
            ph.put("lf", String.format("%.2f", info.getLightFactor()));
            ph.put("sf", String.format("%.2f", info.getSeasonFactor()));
            ph.put("wf", String.format("%.2f", info.getWeatherFactor()));
            ph.put("total_before", String.format("%.3f", info.getTotalBeforeEnv()));
            player.sendMessage(i18n.tr(player, "debug.crop-summary", ph));
            if (verbose) {
                player.sendMessage(i18n.tr(player, "debug.crop-factors", ph));
                player.sendMessage(i18n.tr(player, "debug.crop-env", ph));
                ph.put("bad_penalty", String.format("%.2f", info.getBadEnvPenalty()));
                ph.put("stability", String.format("%.2f", info.getStabilityFactor()));
                ph.put("mitigation", String.format("%.2f", info.getPenaltyMitigation()));
                ph.put("boost", String.format("%.2f", info.getEnvAdvanceBoost()));
                ph.put("total_after", String.format("%.3f", info.getTotal()));
                player.sendMessage(i18n.tr(player, "debug.crop-env-bonus", ph));
                player.sendMessage(i18n.tr(player, "debug.crop-thresholds", ph));
                player.sendMessage(i18n.tr(player, "debug.crop-params", ph));
            }
        } else {
            boolean handled = tryDebugCraftEngineCrop(player, verbose, i18n, cropManager, configManager, target, season, progress, weather, advanceTh, witherTh, cropsEnabled);
            if (!handled) {
                // 如果目标是 CraftEngine 自定义方块，但未在 crops.yml 注册，也给出明确提示（避免用户误以为“无法查看”）
                CraftEngineState ce = CraftEngineState.tryResolve(target);
                if (ce != null && ce.customId != null && !ce.customId.isBlank()) {
                    player.sendMessage(i18n.tr(player, "debug.crop-ce-unregistered", Map.of(
                            "custom_id", ce.customId,
                            "crops_enabled", cropsEnabled ? "true" : "false"
                    )));
                } else {
                    player.sendMessage(i18n.tr(player, "debug.crop-none", Map.of(
                            "block", target.getType().name(),
                            "crops_enabled", cropsEnabled ? "true" : "false"
                    )));
                }
            }
        }
        return true;
    }

    private boolean tryDebugCraftEngineCrop(
            Player player,
            boolean verbose,
            I18n i18n,
            CropManager cropManager,
            ConfigManager configManager,
            Block target,
            SeasonType season,
            double progress,
            WeatherType weather,
            double advanceTh,
            double witherTh,
            boolean cropsEnabled
    ) {
        if (configManager == null || configManager.getCropConfig() == null) return false;
        if (!configManager.getCropConfig().isGlobalEnabled()) return false;

        CraftEngineState ce = CraftEngineState.tryResolve(target);
        if (ce == null) return false;
        String customId = ce.customId;

        var cropOpt = configManager.getCropConfig().getCraftEngineType(customId);
        if (cropOpt.isEmpty()) return false;
        CropType crop = cropOpt.get();

        Object ageProp = resolveCraftEngineAgeProperty(configManager, customId, ce.state);
        Integer ageV = ageProp != null ? extractCraftEngineInt(ce.state, ageProp) : null;
        int age = ageV != null ? ageV : 0;
        int maxAge = ageProp != null ? maxCraftEngineInt(ageProp) : -1;

        EnvironmentContext cropEnv = EnvironmentAPI.getContext(target);
        GrowthContext ctx = new GrowthContext(
                target.getLocation(),
                crop,
                Math.max(0, age),
                season,
                progress,
                weather,
                cropEnv
        );
        GrowthDebugInfo info = cropManager.calculateGrowthDebug(ctx);

        // 识别到 CraftEngine 作物时，顺手加入追踪，确保后续能被 tick 接管（避免“只监听放置事件”的漏追踪）
        CraftEngineTrackService controller = SERVICES.get(CraftEngineTrackService.class);
        if (controller != null) {
            controller.track(target);
        }

        String decision = info.getResult().isShouldWither()
                ? "WITHER"
                : (info.getResult().getStageDelta() > 0 ? "ADVANCE" : "BLOCK");

        java.util.Map<String, String> ph = new java.util.HashMap<>();
        ph.put("block", customId); // 显示真实 custom id，而不是伪装 Material（如 BRICKS）
        ph.put("crop_id", crop.getId());
        ph.put("crops_enabled", cropsEnabled ? "true" : "false");
        ph.put("crop_enabled", crop.isEnabled() ? "true" : "false");
        ph.put("age", String.valueOf(age));
        ph.put("max_age", String.valueOf(maxAge));
        ph.put("stages", String.valueOf(crop.getStages()));
        ph.put("min_light", String.valueOf(crop.getMinLight()));
        ph.put("opt_temp", String.format("%.1f", crop.getOptimalTemperature()));
        ph.put("temp_tol", String.format("%.1f", crop.getTemperatureTolerance()));
        ph.put("opt_hum", String.format("%.2f", crop.getOptimalHumidity()));
        ph.put("hum_tol", String.format("%.2f", crop.getHumidityTolerance()));
        ph.put("pref_seasons", crop.getPreferredSeasons().toString());
        ph.put("env_temp", String.format("%.1f", cropEnv.getTemperature()));
        ph.put("env_hum", String.format("%.2f", cropEnv.getHumidity()));
        ph.put("env_soil", String.format("%.2f", cropEnv.getSoilMoisture()));
        ph.put("env_light", String.valueOf(cropEnv.getLightLevel()));
        String cropEnvTypeKey = cropEnv.getEnvironmentType() != null ? cropEnv.getEnvironmentType().name().toLowerCase(Locale.ROOT) : "unknown";
        String cropEnvTypeName = i18n.trRaw(player, "environment.type." + cropEnvTypeKey);
        ph.put("env_type", cropEnvTypeName);
        ph.put("openness", String.format("%.2f", cropEnv.getOpennessScore()));
        ph.put("exposure", String.format("%.2f", cropEnv.getExposureScore()));
        ph.put("open_ratio", String.format("%.2f", cropEnv.getOpenRatio()));
        ph.put("roof_dy", String.valueOf(cropEnv.getRoofDy()));
        ph.put("advance_th", String.format("%.2f", advanceTh));
        ph.put("wither_th", String.format("%.2f", witherTh));
        ph.put("decision", decision);
        ph.put("total", String.format("%.3f", info.getTotal()));
        ph.put("tf", String.format("%.2f", info.getTemperatureFactor()));
        ph.put("hf", String.format("%.2f", info.getHumidityFactor()));
        ph.put("lf", String.format("%.2f", info.getLightFactor()));
        ph.put("sf", String.format("%.2f", info.getSeasonFactor()));
        ph.put("wf", String.format("%.2f", info.getWeatherFactor()));
        ph.put("total_before", String.format("%.3f", info.getTotalBeforeEnv()));

        player.sendMessage(i18n.tr(player, "debug.crop-summary", ph));
        if (verbose) {
            player.sendMessage(i18n.tr(player, "debug.crop-factors", ph));
            player.sendMessage(i18n.tr(player, "debug.crop-env", ph));
            ph.put("bad_penalty", String.format("%.2f", info.getBadEnvPenalty()));
            ph.put("stability", String.format("%.2f", info.getStabilityFactor()));
            ph.put("mitigation", String.format("%.2f", info.getPenaltyMitigation()));
            ph.put("boost", String.format("%.2f", info.getEnvAdvanceBoost()));
            ph.put("total_after", String.format("%.3f", info.getTotal()));
            player.sendMessage(i18n.tr(player, "debug.crop-env-bonus", ph));
            player.sendMessage(i18n.tr(player, "debug.crop-thresholds", ph));
            player.sendMessage(i18n.tr(player, "debug.crop-params", ph));
        }
        return true;
    }

    private static Object resolveCraftEngineAgeProperty(ConfigManager cm, String customId, Object state) {
        var configured = cm.getCropConfig().getCraftEngineAgeProperty(customId);
        if (configured.isPresent()) {
            Object p = findCraftEnginePropertyByName(state, configured.get());
            if (p != null) return p;
        }
        for (String name : new String[]{"age", "stage", "growth"}) {
            Object p = findCraftEnginePropertyByName(state, name);
            if (p != null) return p;
        }
        return null;
    }

    private static Object findCraftEnginePropertyByName(Object state, String name) {
        try {
            Object props = invoke(state, "getProperties");
            if (!(props instanceof Iterable<?> it)) return null;
            for (Object p : it) {
                if (p == null) continue;
                Object n = invoke(p, "name");
                if (n != null && n.toString().equalsIgnoreCase(name)) return p;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Integer extractCraftEngineInt(Object state, Object property) {
        try {
            Object v = invokeSingleArgByName(state, "getNullable", property);
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int maxCraftEngineInt(Object property) {
        try {
            Object valuesObj = invoke(property, "possibleValues");
            if (!(valuesObj instanceof java.util.List<?> values)) return 0;
            int max = Integer.MIN_VALUE;
            for (Object o : values) {
                if (o instanceof Number n) max = Math.max(max, n.intValue());
            }
            return max == Integer.MIN_VALUE ? 0 : max;
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static final class CraftEngineState {
        final String customId;
        final Object state;

        private CraftEngineState(String customId, Object state) {
            this.customId = customId;
            this.state = state;
        }

        static CraftEngineState tryResolve(Block block) {
            try {
                Class<?> blocksClz = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");
                Object st = invokeStatic(blocksClz, "getCustomBlockState", new Class[]{Block.class}, new Object[]{block});
                if (st == null) return null;
                Object empty = invoke(st, "isEmpty");
                if (empty instanceof Boolean b && b) return null;

                Object ownerWrapper = invoke(st, "owner");
                if (ownerWrapper == null) return null;
                Object ownerVal = invoke(ownerWrapper, "value");
                if (ownerVal == null) return null;
                Object idObj = invoke(ownerVal, "id");
                if (idObj == null) return null;
                Object idStr = invoke(idObj, "asString");
                if (idStr == null) return null;
                return new CraftEngineState(idStr.toString(), st);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static Object invoke(Object target, String method, Class<?>[] paramTypes, Object[] args) throws Exception {
        java.lang.reflect.Method m = target.getClass().getMethod(method, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Object invoke(Object target, String method) throws Exception {
        java.lang.reflect.Method m = target.getClass().getMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static Object invokeStatic(Class<?> clz, String method, Class<?>[] paramTypes, Object[] args) throws Exception {
        java.lang.reflect.Method m = clz.getMethod(method, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private static Object invokeSingleArgByName(Object target, String methodName, Object arg) throws Exception {
        for (java.lang.reflect.Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (m.getParameterCount() != 1) continue;
            m.setAccessible(true);
            return m.invoke(target, arg);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName + "(*)");
    }

    private boolean handleSim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.parse("<color:#FFB4B4>该命令只能由玩家执行。</>"));
            return true;
        }
        if (args == null || args.length == 0) {
            sendHelp(sender);
            return true;
        }
        if ("crop".equalsIgnoreCase(args[0])) {
            return handleSimCrop(player);
        }
        sendHelp(sender);
        return true;
    }

    /**
     * 只输出计算结果：对目标作物在“同一基准环境”下模拟四季 × 天气的 total 与判定。
     */
    private boolean handleSimCrop(Player player) {
        I18n i18n = SERVICES.get(I18n.class);
        if (i18n == null) {
            player.sendMessage(Text.parse("<color:#FFB4B4>i18n 未初始化。</>"));
            return true;
        }
        ConfigManager cm = SERVICES.get(ConfigManager.class);
        CropManager cropManager = SERVICES.get(CropManager.class);
        if (cm == null || cropManager == null) {
            player.sendMessage(Text.parse("<color:#FFB4B4>系统未初始化。</>"));
            return true;
        }

        Block target = player.getTargetBlockExact(6);
        if (target == null) target = player.getLocation().getBlock();
        Location loc = target.getLocation();

        // 1) 先尝试原版作物
        var cropOpt = cropManager.getCropDataForLocation(loc);
        CropType crop;
        String displayBlock;
        if (cropOpt.isPresent()) {
            crop = cropOpt.get();
            displayBlock = target.getType().name();
        } else {
            // 2) 再尝试 CraftEngine 自定义作物
            CropConfigView cropCfg = cm.getCropConfig();
            CraftEngineState ce = cropCfg != null ? CraftEngineState.tryResolve(target) : null;
            if (ce != null && ce.customId != null && !ce.customId.isBlank()) {
                var ceTypeOpt = cropCfg.getCraftEngineType(ce.customId);
                if (ceTypeOpt.isEmpty()) {
                    player.sendMessage(i18n.tr(player, "sim.crop-ce-none", Map.of("custom_id", ce.customId)));
                    return true;
                }
                crop = ceTypeOpt.get();
                displayBlock = ce.customId;
            } else {
                player.sendMessage(i18n.tr(player, "sim.crop-none", Map.of("block", target.getType().name())));
                return true;
            }
        }
        EnvironmentContext envNow = EnvironmentAPI.getContext(target);
        SeasonType currentSeason = SeasonAPI.getCurrentSeason(player.getWorld());
        WeatherType currentWeather = WeatherAPI.getCurrentWeather(player.getWorld());

        double seasonTempNow = cm.getSeasonConfig().getTemperatureDelta(currentSeason);
        double seasonHumNow = cm.getSeasonConfig().getHumidityDelta(currentSeason);
        var weatherProfileNow = cm.getWeatherConfig().getProfile(currentWeather);

        // 反推基准：envNow = base + seasonDelta(now) + weatherDelta(now)
        double baseTemp = envNow.getTemperature() - seasonTempNow - weatherProfileNow.getTemperatureDelta();
        double baseHum = clamp01(envNow.getHumidity() - seasonHumNow - weatherProfileNow.getHumidityDelta());
        double baseSoil = clamp01(envNow.getSoilMoisture() - weatherProfileNow.getSoilMoistureDelta());
        int light = envNow.getLightLevel();

        double advanceTh = cm.getGrowthConfig().getAdvanceThreshold();
        double witherTh = cm.getGrowthConfig().getWitherThreshold();
        String envTypeKey = envNow.getEnvironmentType() != null
                ? envNow.getEnvironmentType().name().toLowerCase(Locale.ROOT)
                : "unknown";
        String envTypeName = i18n.trRaw(player, "environment.type." + envTypeKey);
        boolean envAffectGrowth = cm.getGrowthConfig().isEnvironmentAffectGrowthEnabled();

        player.sendMessage(i18n.tr(player, "sim.crop-header", Map.of(
                "block", displayBlock,
                "crop_id", crop.getId(),
                "base_t", String.format("%.2f", baseTemp),
                "base_h", String.format("%.2f", baseHum),
                "base_soil", String.format("%.2f", baseSoil),
                "light", String.valueOf(light),
                "advance_th", String.format("%.2f", advanceTh),
                "wither_th", String.format("%.2f", witherTh),
                "env_type", envTypeName,
                "env_growth", envAffectGrowth ? "ON" : "OFF"
        )));

        WeatherType[] weathers = new WeatherType[]{WeatherType.SUNNY, WeatherType.RAIN, WeatherType.STORM, WeatherType.SNOW, WeatherType.CLOUDY};
        for (SeasonType s : SeasonType.values()) {
            StringBuilder sb = new StringBuilder();
            sb.append(s.name()).append(": ");
            for (int i = 0; i < weathers.length; i++) {
                WeatherType w = weathers[i];
                var wp = cm.getWeatherConfig().getProfile(w);
                double t = baseTemp + cm.getSeasonConfig().getTemperatureDelta(s) + wp.getTemperatureDelta();
                double h = clamp01(baseHum + cm.getSeasonConfig().getHumidityDelta(s) + wp.getHumidityDelta());
                double soil = clamp01(baseSoil + wp.getSoilMoistureDelta());

                EnvironmentContext env = EnvironmentContext.builder()
                        .temperature(t)
                        .humidity(h)
                        .soilMoisture(soil)
                        .lightLevel(light)
                        .skyLight(envNow.getSkyLight())
                        .blockLight(envNow.getBlockLight())
                        .outdoor(envNow.isOutdoor())
                        .outdoorScore(envNow.getOutdoorScore())
                        .altitudeY(envNow.getAltitudeY())
                        .nearWaterScore(envNow.getNearWaterScore())
                        .greenhouseScore(envNow.getGreenhouseScore())
                        .inGreenhouse(envNow.isInGreenhouse())
                        .opennessScore(envNow.getOpennessScore())
                        .exposureScore(envNow.getExposureScore())
                        .openRatio(envNow.getOpenRatio())
                        .roofDy(envNow.getRoofDy())
                        .environmentType(envNow.getEnvironmentType())
                        .biome(envNow.getBiome())
                        .biomeKey(envNow.getBiomeKey())
                        .biomeGroupId(envNow.getBiomeGroupId())
                        .build();

                GrowthContext ctx = new GrowthContext(
                        loc,
                        crop,
                        0,
                        s,
                        0.0,
                        w,
                        env
                );
                var info = cropManager.calculateGrowthDebug(ctx);
                String decision = info.getResult().isShouldWither()
                        ? "W"
                        : (info.getResult().getStageDelta() > 0 ? "A" : "B");
                sb.append(w.name()).append("=").append(String.format("%.3f", info.getTotal())).append("(").append(decision).append(")");
                if (i != weathers.length - 1) sb.append("  ");
            }
            player.sendMessage(Text.parse("<color:#A9B3C3>" + sb));
        }
        player.sendMessage(i18n.tr(player, "sim.crop-legend"));
        return true;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
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
        String m = module == null ? "" : module.trim().toLowerCase(Locale.ROOT);
        boolean valid = switch (m) {
            case "all",
                 "season", "seasons",
                 "growth",
                 "debug", "config",
                 "weather",
                 "visual",
                 "crop", "crops",
                 "biome", "biomes",
                 "environment", "env" -> true;
            default -> false;
        };
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
        if (!valid) {
            if (sender instanceof Player p) {
                I18n i18n = SERVICES.get(I18n.class);
                if (i18n != null) {
                    sender.sendMessage(i18n.tr(p, "command.ne.reload-invalid", Map.of(
                            "module", String.valueOf(module),
                            "modules", "all, seasons, growth, config, weather, visual, crops, biome, environment"
                    )));
                    return true;
                }
            }
            sender.sendMessage(Text.parse("<color:#FFB4B4>未知模块：</> <color:#FFE2A9>" + module
                    + "</> <color:#A9B3C3>可用：all, seasons, growth, config, weather, visual, crops, biome, environment</>"));
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

