package com.github.cinnaio.natureEngine.engine.config;

import com.github.cinnaio.natureEngine.core.agriculture.crop.CropData;
import com.github.cinnaio.natureEngine.core.agriculture.crop.CropType;
import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * crops.yml 视图：负责将配置转换为 CropType 定义。
 */
public final class CropConfigView {

    private final FileConfiguration config;
    private final Map<Material, CropType> vanillaTypes = new EnumMap<>(Material.class);
    private boolean globalEnabled;

    public CropConfigView(FileConfiguration config) {
        this.config = config;
        reload();
    }

    public void reload() {
        vanillaTypes.clear();
        this.globalEnabled = config.getBoolean("crops.enabled", true);

        ConfigurationSection root = config.getConfigurationSection("crops.vanilla");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            Material mat = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
            if (mat == null) {
                continue;
            }
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            boolean enabled = sec.getBoolean("enabled", true);
            String id = "minecraft:" + key.toLowerCase(Locale.ROOT);
            int stages = sec.getInt("stages", 8);
            long baseTicks = sec.getLong("base-ticks-per-stage", 2400L);
            double optTemp = sec.getDouble("optimal-temperature", 18.0);
            double tempTol = sec.getDouble("temperature-tolerance", 10.0);
            double optHum = sec.getDouble("optimal-humidity", 0.6);
            double humTol = sec.getDouble("humidity-tolerance", 0.3);
            int minLight = sec.getInt("min-light", 9);
            Set<SeasonType> seasons = parseSeasons(sec.getStringList("preferred-seasons"));

            CropData data = new CropData(
                    id,
                    stages,
                    baseTicks,
                    optTemp,
                    tempTol,
                    optHum,
                    humTol,
                    minLight,
                    seasons,
                    enabled
            );
            vanillaTypes.put(mat, data);
        }
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public Optional<CropType> getVanillaType(Material material) {
        CropType type = vanillaTypes.get(material);
        if (type == null || !type.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(type);
    }

    private Set<SeasonType> parseSeasons(java.util.List<String> list) {
        EnumSet<SeasonType> set = EnumSet.noneOf(SeasonType.class);
        if (list == null) return set;
        for (String s : list) {
            if (s == null) continue;
            try {
                SeasonType type = SeasonType.valueOf(s.toUpperCase(Locale.ROOT));
                set.add(type);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (set.isEmpty()) {
            set.add(SeasonType.SPRING);
            set.add(SeasonType.SUMMER);
        }
        return set;
    }
}

