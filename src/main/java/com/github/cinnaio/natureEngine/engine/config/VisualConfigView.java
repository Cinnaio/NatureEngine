package com.github.cinnaio.natureEngine.engine.config;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
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

    public int getRadiusChunks() {
        return Math.max(1, config.getInt("visual.radius-chunks", 4));
    }

    public int getMaxColumnsPerTick() {
        return Math.max(1, config.getInt("visual.max-columns-per-tick", 600));
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
}

