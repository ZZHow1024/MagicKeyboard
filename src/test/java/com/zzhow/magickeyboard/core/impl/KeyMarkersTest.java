package com.zzhow.magickeyboard.core.impl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 特殊按键标记解析器单元测试
 *
 * @author ZZHow
 * create 2026/11/27
 */
class KeyMarkersTest {

    @Test
    void parsesMarkersBetweenLiterals() {
        List<Object> segments = KeyMarkers.parse("a{ENTER}b{ESC}");
        assertEquals(4, segments.size());
        assertEquals("a", segments.get(0));
        assertInstanceOf(KeyMarkers.EnterMarker.class, segments.get(1));
        assertEquals("b", segments.get(2));
        assertInstanceOf(KeyMarkers.EscapeMarker.class, segments.get(3));
    }

    @Test
    void parsesDelayMarkerCaseInsensitively() {
        List<Object> segments = KeyMarkers.parse("a{Delay=500}b");
        assertEquals(3, segments.size());
        KeyMarkers.DelayMarker delay = assertInstanceOf(KeyMarkers.DelayMarker.class, segments.get(1));
        assertEquals(500, delay.millis());
    }

    @Test
    void parsesComboMarkersWithModifiers() {
        KeyMarkers.ComboMarker combo = assertInstanceOf(KeyMarkers.ComboMarker.class,
                KeyMarkers.parse("{CTRL+S}").get(0));
        assertEquals(Set.of(KeyMarkers.Modifier.CTRL), combo.modifiers());
        assertEquals('S', combo.key());

        combo = assertInstanceOf(KeyMarkers.ComboMarker.class, KeyMarkers.parse("{ctrl + alt + q}").get(0));
        assertEquals(Set.of(KeyMarkers.Modifier.CTRL, KeyMarkers.Modifier.ALT), combo.modifiers());
        assertEquals('Q', combo.key());

        combo = assertInstanceOf(KeyMarkers.ComboMarker.class, KeyMarkers.parse("{SHIFT+5}").get(0));
        assertEquals(Set.of(KeyMarkers.Modifier.SHIFT), combo.modifiers());
        assertEquals('5', combo.key());
    }

    @Test
    void typesUnknownMarkersLiterally() {
        for (String text : new String[]{"{FOO}", "{CTRL}", "{CTRL++S}", "{DELAY=}", "{DELAY=0}",
                "{DELAY=99999999}", "{ENTER", "{", "{ENTER)", "{A B}"}) {
            List<Object> segments = KeyMarkers.parse(text);
            assertEquals(1, segments.size(), "literal expected for: " + text);
            assertEquals(text, segments.get(0), "literal expected for: " + text);
        }
    }

    @Test
    void keepsLiteralBracesAroundMarkers() {
        List<Object> segments = KeyMarkers.parse("code {a} and {ENTER} end");
        // "{a}" 无法识别，按原文并入前后的字面段
        assertEquals(3, segments.size());
        assertEquals("code {a} and ", segments.get(0));
        assertEquals(" end", segments.get(2));
        assertInstanceOf(KeyMarkers.EnterMarker.class, segments.get(1));
    }

    @Test
    void handlesPlainTextAndEmptyText() {
        assertEquals(List.of("plain text"), KeyMarkers.parse("plain text"));
        assertEquals(List.of(), KeyMarkers.parse(""));
    }
}
