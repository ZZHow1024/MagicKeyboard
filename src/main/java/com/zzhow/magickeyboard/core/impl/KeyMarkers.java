package com.zzhow.magickeyboard.core.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 特殊按键标记解析器
 * <p>
 * 将文本解析为「字面片段（String）」与「标记（Marker）」交替的序列：
 * <ul>
 *     <li>{@code {ENTER}} {@code {TAB}} {@code {ESC}}：功能键</li>
 *     <li>{@code {DELAY=500}}：暂停 500ms（1 ~ 60000ms）</li>
 *     <li>{@code {CTRL+S}} {@code {CTRL+ALT+Q}} 等：修饰键组合，主键为 A-Z 或 0-9</li>
 * </ul>
 * 标记不区分大小写，允许两侧空白；无法识别的标记按原文逐字符键入。
 *
 * @author ZZHow
 * create 2026/11/27
 */
final class KeyMarkers {

    /**
     * 单个延迟标记的时长上限（ms）
     */
    static final long MAX_DELAY_MS = 60000;

    enum Modifier {
        CTRL, ALT, SHIFT
    }

    sealed interface Marker permits EnterMarker, TabMarker, EscapeMarker, DelayMarker, ComboMarker {
    }

    record EnterMarker() implements Marker {
    }

    record TabMarker() implements Marker {
    }

    record EscapeMarker() implements Marker {
    }

    record DelayMarker(long millis) implements Marker {
    }

    record ComboMarker(Set<Modifier> modifiers, char key) implements Marker {
    }

    private KeyMarkers() {
    }

    /**
     * 解析文本
     *
     * @param text 原始文本
     * @return 字面片段（String）与标记（Marker）组成的有序序列
     */
    static List<Object> parse(String text) {
        List<Object> segments = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '{') {
                int close = text.indexOf('}', i + 1);
                if (close > i) {
                    Marker marker = parseMarker(text.substring(i + 1, close));
                    if (marker != null) {
                        if (literal.length() > 0) {
                            segments.add(literal.toString());
                            literal.setLength(0);
                        }
                        segments.add(marker);
                        i = close + 1;
                        continue;
                    }
                }
            }
            literal.append(c);
            i++;
        }
        if (literal.length() > 0) {
            segments.add(literal.toString());
        }
        return segments;
    }

    /**
     * 解析花括号内的内容，无法识别时返回 null（按原文键入）
     */
    private static Marker parseMarker(String content) {
        String name = content.trim().toUpperCase(Locale.ROOT);
        switch (name) {
            case "ENTER":
                return new EnterMarker();
            case "TAB":
                return new TabMarker();
            case "ESC":
                return new EscapeMarker();
            default:
                break;
        }

        if (name.startsWith("DELAY=")) {
            String digits = name.substring("DELAY=".length()).trim();
            if (digits.matches("\\d{1,6}")) {
                long millis = Long.parseLong(digits);
                if (millis >= 1 && millis <= MAX_DELAY_MS) {
                    return new DelayMarker(millis);
                }
            }
            return null;
        }

        // 组合键：至少一个修饰键，最后一项为主键（A-Z 或 0-9）
        String[] parts = name.split("\\+");
        if (parts.length < 2) {
            return null;
        }
        Set<Modifier> modifiers = new LinkedHashSet<>();
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i].trim();
            Modifier modifier = switch (part) {
                case "CTRL" -> Modifier.CTRL;
                case "ALT" -> Modifier.ALT;
                case "SHIFT" -> Modifier.SHIFT;
                default -> null;
            };
            if (modifier == null) {
                return null;
            }
            modifiers.add(modifier);
        }
        String key = parts[parts.length - 1].trim();
        if (key.length() != 1 || !isComboKey(key.charAt(0))) {
            return null;
        }
        return new ComboMarker(modifiers, key.charAt(0));
    }

    private static boolean isComboKey(char key) {
        return (key >= 'A' && key <= 'Z') || (key >= '0' && key <= '9');
    }
}
