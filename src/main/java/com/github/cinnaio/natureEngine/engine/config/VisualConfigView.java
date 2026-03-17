package com.github.cinnaio.natureEngine.engine.config;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class VisualConfigView {

    private final FileConfiguration config;

    public VisualConfigView(FileConfiguration config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.getBoolean("visual.enabled", true);
    }

    public boolean isDebug() {
        return config.getBoolean("visual.debug", false);
    }

    public int getRadiusChunks() {
        return Math.max(1, config.getInt("visual.radius-chunks", 4));
    }

    public int getMaxColumnsPerTick() {
        return Math.max(1, config.getInt("visual.max-columns-per-tick", 600));
    }

    /** 玩家移动时自动入队刷新周围区块，使视觉跟随。 */
    public boolean isAutoFollowEnabled() {
        return config.getBoolean("visual.auto-follow", true);
    }

    /** 自动跟随检测间隔（tick），例如 40 = 约 2 秒。 */
    public long getAutoFollowIntervalTicks() {
        return Math.max(20L, config.getLong("visual.auto-follow-interval-ticks", 40L));
    }

    /** 玩家至少移动多少 chunk 才重新入队刷新，避免原地不动也刷。 */
    public int getAutoFollowMinMoveChunks() {
        return Math.max(0, config.getInt("visual.auto-follow-min-move-chunks", 1));
    }

    /** 每 tick 每玩家最多处理多少 chunk，越大刷新越快（例如 10 则半径 4 约 0.4 秒刷完）。 */
    public int getChunksPerTickPerPlayer() {
        return Math.max(1, config.getInt("visual.chunks-per-tick-per-player", 10));
    }

    /**
     * 每个季节的默认目标 biome（用于“全群系都生效”）。
     * 支持：
     * - "SNOWY_PLAINS"（视为 minecraft:snowy_plains）
     * - "minecraft:snowy_plains"
     * - "terralith:lavender_forest"
     */
    public NamespacedKey getDefaultBiomeKey(SeasonType season) {
        String raw = config.getString("visual.season-biome-default." + season.name());
        return parseBiomeKey(raw);
    }

    /**
     * NamespacedKey 映射：支持 Terralith 等命名空间 biome。
     * 同时会把旧的枚举配置（PLAINS: MEADOW）合并进来（转为 minecraft:*）。
     */
    public Map<NamespacedKey, NamespacedKey> getBiomeKeyMap(SeasonType season) {
        Map<NamespacedKey, NamespacedKey> out = new HashMap<>();

        // 新格式：visual.season-biome-key-map.<SEASON>
        ConfigurationSection keySection = config.getConfigurationSection("visual.season-biome-key-map." + season.name());
        if (keySection != null) {
            for (String fromKey : keySection.getKeys(false)) {
                String toKey = keySection.getString(fromKey);
                NamespacedKey from = parseBiomeKey(fromKey);
                NamespacedKey to = parseBiomeKey(toKey);
                if (from != null && to != null) {
                    out.put(from, to);
                }
            }
        }

        // 旧格式兼容：visual.season-biome-map.<SEASON>（枚举名）
        ConfigurationSection legacy = config.getConfigurationSection("visual.season-biome-map." + season.name());
        if (legacy != null) {
            for (String fromKey : legacy.getKeys(false)) {
                String toKey = legacy.getString(fromKey);
                if (toKey == null) continue;
                NamespacedKey from = parseBiomeKey(fromKey);
                NamespacedKey to = parseBiomeKey(toKey);
                if (from != null && to != null) {
                    out.put(from, to);
                }
            }
        }

        return out;
    }

    public Map<Biome, Biome> getBiomeMap(SeasonType season) {
        ConfigurationSection section = config.getConfigurationSection("visual.season-biome-map." + season.name());
        Map<Biome, Biome> map = new HashMap<>();
        if (section == null) {
            return map;
        }
        for (String fromKey : section.getKeys(false)) {
            String toKey = section.getString(fromKey);
            if (toKey == null) {
                continue;
            }
            try {
                Biome from = Biome.valueOf(fromKey.toUpperCase());
                Biome to = Biome.valueOf(toKey.toUpperCase());
                map.put(from, to);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return map;
    }

    public Map<SeasonType, Map<Biome, Biome>> getAllBiomeMaps() {
        Map<SeasonType, Map<Biome, Biome>> out = new EnumMap<>(SeasonType.class);
        for (SeasonType s : SeasonType.values()) {
            out.put(s, getBiomeMap(s));
        }
        return out;
    }

    private NamespacedKey parseBiomeKey(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (!s.contains(":")) {
            // 兼容旧的枚举名/大写写法
            s = "minecraft:" + s.toLowerCase();
        } else {
            // 统一小写 path（namespace 保持原样但也通常小写）
            String[] parts = s.split(":", 2);
            s = parts[0].toLowerCase() + ":" + parts[1].toLowerCase();
        }
        try {
            return NamespacedKey.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

