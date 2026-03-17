package com.github.cinnaio.natureEngine.engine.config;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.regex.Pattern;

/**
 * biome-titles.yml 视图：Biome -> Title 文本映射。
 */
public final class BiomeTitleConfigView {

    private final FileConfiguration config;
    private boolean enabled;
    private long cooldownMillis;
    private Set<String> worlds;
    private final Map<String, String> titlesByKey = new HashMap<>();
    private final List<GroupRule> groups = new ArrayList<>();

    public BiomeTitleConfigView(FileConfiguration config) {
        this.config = config;
        reload();
    }

    public void reload() {
        titlesByKey.clear();
        groups.clear();
        this.enabled = config.getBoolean("biome-titles.enabled", true);
        long cooldownTicks = Math.max(0L, config.getLong("biome-titles.cooldown-ticks", 80L));
        this.cooldownMillis = cooldownTicks * 50L;

        List<String> ws = config.getStringList("biome-titles.worlds");
        Set<String> set = new HashSet<>();
        if (ws != null) {
            for (String w : ws) {
                if (w == null) continue;
                String s = w.trim();
                if (!s.isEmpty()) set.add(s);
            }
        }
        this.worlds = Collections.unmodifiableSet(set);

        ConfigurationSection sec = config.getConfigurationSection("biome-titles.titles");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                if (key == null) continue;
                String raw = sec.getString(key, null);
                if (raw == null || raw.isBlank()) continue;
                String norm = normalizeBiomeKey(key);
                if (norm != null) {
                    titlesByKey.put(norm, raw);
                }
            }
        }

        var list = config.getMapList("biome-titles.groups");
        if (list != null) {
            for (Map<?, ?> m : list) {
                if (m == null) continue;
                Object idObj = m.get("id");
                Object titleObj = m.get("title");
                Object matchObj = m.get("match");
                if (idObj == null || titleObj == null || matchObj == null) continue;
                String id = String.valueOf(idObj).trim();
                String title = String.valueOf(titleObj);
                if (id.isEmpty() || title.isBlank()) continue;

                List<String> patterns = new ArrayList<>();
                if (matchObj instanceof List<?> l) {
                    for (Object o : l) {
                        if (o == null) continue;
                        String s = String.valueOf(o).trim();
                        if (!s.isEmpty()) patterns.add(s);
                    }
                } else {
                    String s = String.valueOf(matchObj).trim();
                    if (!s.isEmpty()) patterns.add(s);
                }
                if (patterns.isEmpty()) continue;
                groups.add(GroupRule.of(id, title, patterns));
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public boolean isWorldEnabled(World world) {
        if (world == null) return false;
        if (worlds == null || worlds.isEmpty()) return true;
        return worlds.contains(world.getName());
    }

    public Optional<String> getTitle(Biome biome) {
        if (biome == null) return Optional.empty();
        try {
            NamespacedKey k = biome.getKey();
            if (k != null) {
                String v = titlesByKey.get(k.toString());
                if (v != null) return Optional.of(v);
            }
        } catch (Throwable ignored) {
        }
        // fallback：兼容配置里写的旧 enum 名（例如 PLAINS）
        String v2 = titlesByKey.get(biome.toString().toUpperCase(Locale.ROOT));
        return Optional.ofNullable(v2);
    }

    /**
     * 解析当前 biome 属于哪个 group。返回 null 表示无匹配。
     */
    public String resolveGroupId(Biome biome) {
        GroupResult r = resolveGroup(biome);
        return r != null ? r.id : null;
    }

    /**
     * 解析当前 biome 的 group 标题（若匹配），否则 empty。
     */
    public Optional<String> getGroupTitle(Biome biome) {
        GroupResult r = resolveGroup(biome);
        return r == null ? Optional.empty() : Optional.ofNullable(r.title);
    }

    private GroupResult resolveGroup(Biome biome) {
        if (biome == null) return null;
        String key = null;
        try {
            NamespacedKey k = biome.getKey();
            if (k != null) key = k.toString();
        } catch (Throwable ignored) {
        }
        if (key == null || key.isBlank()) {
            key = biome.toString().toLowerCase(Locale.ROOT);
        } else {
            key = key.toLowerCase(Locale.ROOT);
        }

        for (GroupRule g : groups) {
            if (g.matches(key)) {
                return new GroupResult(g.id, g.title);
            }
        }
        return null;
    }

    public int getTitleCount() {
        return titlesByKey.size();
    }

    public int getGroupCount() {
        return groups.size();
    }

    private static String normalizeBiomeKey(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        // 支持 namespaced key：minecraft:plains
        if (s.indexOf(':') > 0) {
            return s.toLowerCase(Locale.ROOT);
        }
        // 支持旧写法：PLAINS
        return s.toUpperCase(Locale.ROOT);
    }

    private record GroupResult(String id, String title) {}

    private static final class GroupRule {
        final String id;
        final String title;
        final List<Pattern> matchers;

        private GroupRule(String id, String title, List<Pattern> matchers) {
            this.id = id;
            this.title = title;
            this.matchers = matchers;
        }

        static GroupRule of(String id, String title, List<String> patterns) {
            List<Pattern> ps = new ArrayList<>(patterns.size());
            for (String p : patterns) {
                Pattern compiled = compileGlob(p);
                if (compiled != null) ps.add(compiled);
            }
            return new GroupRule(id, title, ps);
        }

        boolean matches(String biomeKeyLower) {
            if (biomeKeyLower == null) return false;
            for (Pattern p : matchers) {
                if (p.matcher(biomeKeyLower).matches()) return true;
            }
            return false;
        }

        private static Pattern compileGlob(String raw) {
            if (raw == null) return null;
            String s = raw.trim();
            if (s.isEmpty()) return null;
            // 将 glob 转成 regex。大小写不敏感：统一转小写匹配
            s = s.toLowerCase(Locale.ROOT);
            StringBuilder re = new StringBuilder("^");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '*') {
                    re.append(".*");
                } else if (c == '?') {
                    re.append('.');
                } else {
                    if ("\\.[]{}()+-^$|".indexOf(c) >= 0) re.append('\\');
                    re.append(c);
                }
            }
            re.append("$");
            try {
                return Pattern.compile(re.toString());
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}

