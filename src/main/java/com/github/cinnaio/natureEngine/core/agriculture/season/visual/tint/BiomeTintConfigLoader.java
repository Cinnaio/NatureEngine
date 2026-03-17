package com.github.cinnaio.natureEngine.core.agriculture.season.visual.tint;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * 读取 AdvancedSeasons 风格的 biomeConfiguration/*.yml（按地形类型分组）并合并成按 biome key 的颜色覆盖表。
 *
 * <p>支持 biomes 列表项：</p>
 * - "PLAINS" / "plains" -> minecraft:plains
 * - "minecraft:plains"
 * - "terralith:moonlight_valley"
 */
public final class BiomeTintConfigLoader {

    private final JavaPlugin plugin;
    private final Logger logger;

    public BiomeTintConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public BiomeTintPalette loadOrEmpty() {
        BiomeTintPalette palette = new BiomeTintPalette();
        File dir = new File(plugin.getDataFolder(), "biomeConfiguration");
        if (!dir.exists() || !dir.isDirectory()) {
            logger.info("[NatureEngine] biome tint: 未找到 biomeConfiguration 目录，跳过配色覆盖。路径=" + dir.getAbsolutePath());
            return palette;
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            logger.info("[NatureEngine] biome tint: biomeConfiguration 目录为空，跳过配色覆盖。");
            return palette;
        }

        for (File f : files) {
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                List<String> biomes = yml.getStringList("biomes");
                if (biomes == null || biomes.isEmpty()) continue;

                // seasons.* keys: spring/summer/fall/winter
                BiomeTintPalette.Colors spring = readColors(yml, "seasons.spring");
                BiomeTintPalette.Colors summer = readColors(yml, "seasons.summer");
                BiomeTintPalette.Colors autumn = readColors(yml, "seasons.fall");
                BiomeTintPalette.Colors winter = readColors(yml, "seasons.winter");

                for (String raw : biomes) {
                    NamespacedKey key = parseBiomeKey(raw);
                    if (key == null) continue;
                    if (spring != null) palette.put(SeasonType.SPRING, key, spring);
                    if (summer != null) palette.put(SeasonType.SUMMER, key, summer);
                    if (autumn != null) palette.put(SeasonType.AUTUMN, key, autumn);
                    if (winter != null) palette.put(SeasonType.WINTER, key, winter);
                }
            } catch (Throwable t) {
                logger.warning("[NatureEngine] biome tint: 加载失败 " + f.getName() + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        logger.info("[NatureEngine] biome tint: loaded colors: "
                + "spring=" + palette.size(SeasonType.SPRING)
                + " summer=" + palette.size(SeasonType.SUMMER)
                + " autumn=" + palette.size(SeasonType.AUTUMN)
                + " winter=" + palette.size(SeasonType.WINTER));
        return palette;
    }

    private BiomeTintPalette.Colors readColors(YamlConfiguration yml, String basePath) {
        String sky = yml.getString(basePath + ".sky");
        String fog = yml.getString(basePath + ".fog");
        String water = yml.getString(basePath + ".water");
        String waterFog = yml.getString(basePath + ".waterFog");
        String grass = yml.getString(basePath + ".grass");
        String tree = yml.getString(basePath + ".tree");
        if (sky == null && fog == null && water == null && waterFog == null && grass == null && tree == null) return null;

        return new BiomeTintPalette.Colors(
                parseHexColorOr0(sky),
                parseHexColorOr0(fog),
                parseHexColorOr0(water),
                parseHexColorOr0(waterFog),
                parseHexColorOr0(grass),
                parseHexColorOr0(tree)
        );
    }

    private int parseHexColorOr0(String hex) {
        if (hex == null) return 0;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.isBlank()) return 0;
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private NamespacedKey parseBiomeKey(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (!s.contains(":")) {
            s = "minecraft:" + s.toLowerCase(Locale.ROOT);
        } else {
            String[] parts = s.split(":", 2);
            s = parts[0].toLowerCase(Locale.ROOT) + ":" + parts[1].toLowerCase(Locale.ROOT);
        }
        try {
            return NamespacedKey.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

