package com.github.cinnaio.natureEngine.engine.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一文本反序列化：
 * - 支持 MiniMessage（含 HEX，如 <#ffcc00>）
 * - 支持 legacy（&/§ 颜色码）
 * - 支持 legacy HEX：&#RRGGBB（会转换为 §x§R§R§G§G§B§B）
 */
public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)&?#([0-9a-f]{6})");
    private static final Pattern MM_COLOR_TAG = Pattern.compile("(?i)<color:#([0-9a-f]{6})>");

    private Text() {}

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();

        // MiniMessage 优先：只要包含尖括号，就按 mm 解析（避免把 <#...> 当普通字符）
        if (input.indexOf('<') >= 0 && input.indexOf('>') >= 0) {
            try {
                String mm = input;
                // 兼容用户写法：<color:#RRGGBB>
                mm = normalizeMiniMessageColor(mm);
                // 允许只写开标签：例如 "<red>文本"、"<#ffcc00>文本"
                // 若没有任何闭合标签，则在末尾补一个通用关闭 "</>"（重置样式）
                if (!mm.contains("</")) {
                    mm = mm + "</>";
                }
                return MINI.deserialize(mm);
            } catch (Throwable ignored) {
                // fallback to legacy
            }
        }

        String s = normalizeLegacyHex(input);

        // 兼容 & 与 § 两种 legacy
        if (s.indexOf('§') >= 0) {
            return LEGACY_SECTION.deserialize(s);
        }
        return LEGACY.deserialize(s);
    }

    private static String normalizeLegacyHex(String s) {
        // 把 &#RRGGBB 或 #RRGGBB 转成 §x§R§R§G§G§B§B
        Matcher m = LEGACY_HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            String rep = toSectionX(hex);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String normalizeMiniMessageColor(String s) {
        // 把 <color:#RRGGBB> 转成 MiniMessage 原生的 <#RRGGBB>
        Matcher m = MM_COLOR_TAG.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement("<#" + hex + ">"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String toSectionX(String hex6) {
        String h = hex6.toLowerCase();
        StringBuilder out = new StringBuilder("§x");
        for (int i = 0; i < 6; i++) {
            out.append('§').append(h.charAt(i));
        }
        return out.toString();
    }
}

