package com.github.cinnaio.natureEngine.core.agriculture.season.visual.tint;

import com.github.cinnaio.natureEngine.core.agriculture.season.SeasonType;
import org.bukkit.NamespacedKey;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** 按季节为每个 biome key 提供颜色覆盖（仿 AdvancedSeasons）。 */
public final class BiomeTintPalette {

    public record Colors(int sky, int fog, int water, int waterFog, int grass, int foliage) {}

    private final EnumMap<SeasonType, Map<NamespacedKey, Colors>> colorsBySeason = new EnumMap<>(SeasonType.class);

    public BiomeTintPalette() {
        for (SeasonType s : SeasonType.values()) {
            colorsBySeason.put(s, new HashMap<>());
        }
    }

    public void put(SeasonType season, NamespacedKey biomeKey, Colors colors) {
        colorsBySeason.get(season).put(biomeKey, colors);
    }

    public Colors get(SeasonType season, NamespacedKey biomeKey) {
        return colorsBySeason.get(season).get(biomeKey);
    }

    public int size(SeasonType season) {
        return colorsBySeason.get(season).size();
    }
}

