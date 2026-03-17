package com.github.cinnaio.natureEngine.engine.text;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 简单本地化：
 * - 自动按玩家客户端语言选择（player.locale）
 * - 语言文件：plugins/NatureEngine/lang/zh_cn.yml、en_us.yml
 * - 文本值支持 MiniMessage/HEX/legacy（由 Text.parse 负责）
 */
public final class I18n {

    private final Plugin plugin;
    private final Map<String, FileConfiguration> byCode = new HashMap<>();
    private String defaultCode = "zh_cn";

    public I18n(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ensureDefaultLang("lang/zh_cn.yml", "lang/zh_cn.yml");
        ensureDefaultLang("lang/en_us.yml", "lang/en_us.yml");

        byCode.clear();
        byCode.put("zh_cn", loadYaml(new File(plugin.getDataFolder(), "lang/zh_cn.yml")));
        byCode.put("en_us", loadYaml(new File(plugin.getDataFolder(), "lang/en_us.yml")));
    }

    public Component tr(Player player, String key, Map<String, String> placeholders) {
        String code = detectCode(player);
        String raw = trRaw(code, key);
        if (placeholders != null) {
            for (var e : placeholders.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return Text.parse(raw);
    }

    public Component tr(Player player, String key) {
        return tr(player, key, null);
    }

    public String detectCode(Player player) {
        try {
            Locale loc = player.locale();
            if (loc != null) {
                String s = loc.toString().toLowerCase(Locale.ROOT);
                if (s.contains("_")) return s;
                // e.g. "en-us" or "en-us-x-lvariant"
                s = s.replace('-', '_');
                if (s.startsWith("zh")) return "zh_cn";
                if (s.startsWith("en")) return "en_us";
            }
        } catch (Throwable ignored) {
        }
        return defaultCode;
    }

    /** 取原始字符串（不做 parse），带默认语言 fallback。 */
    public String trRaw(Player player, String key) {
        return getLocalizedRaw(detectCode(player), key);
    }

    /** 按语言代码取原始字符串，供无玩家上下文（如 PAPI 离线）时使用。 */
    public String trRaw(String localeCode, String key) {
        return getLocalizedRaw(localeCode != null ? localeCode : defaultCode, key);
    }

    private String getLocalizedRaw(String code, String key) {
        String raw = getRaw(code, key);
        if (raw == null) raw = getRaw(defaultCode, key);
        if (raw == null) raw = key;
        return raw;
    }

    private String getRaw(String code, String key) {
        FileConfiguration cfg = byCode.get(code);
        if (cfg == null) return null;
        String path = "lang." + key;
        return cfg.getString(path);
    }

    private FileConfiguration loadYaml(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void ensureDefaultLang(String resourcePath, String dataRelativePath) {
        try {
            File out = new File(plugin.getDataFolder(), dataRelativePath);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (out.exists()) return;
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in == null) return;
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                cfg.save(out);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NatureEngine] i18n: failed to write default lang " + dataRelativePath + " -> " + e.getMessage());
        }
    }
}

