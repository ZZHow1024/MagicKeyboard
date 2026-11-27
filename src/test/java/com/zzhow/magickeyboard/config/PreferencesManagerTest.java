package com.zzhow.magickeyboard.config;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.util.OverlayCountdown;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置持久化管理器单元测试（注入内存存储，不触碰真实偏好系统）
 *
 * @author ZZHow
 * create 2026/11/27
 */
class PreferencesManagerTest {

    private final Map<String, String> data = new HashMap<>();
    private final PreferenceStore memory = new PreferenceStore() {
        @Override
        public String get(String key, String defaultValue) {
            return data.getOrDefault(key, defaultValue);
        }

        @Override
        public void put(String key, String value) {
            data.put(key, value);
        }
    };

    @AfterEach
    void resetStore() {
        PreferencesManager.setStore(memory);
        data.clear();
    }

    @Test
    void roundTripsAllPreferences() {
        PreferencesManager.setStore(memory);

        PreferencesManager.saveLanguage("zh_HANT");
        assertEquals("zh_HANT", PreferencesManager.loadLanguage());

        PreferencesManager.saveMode(ControlCenter.Mode.RAPID_MODE);
        assertEquals(ControlCenter.Mode.RAPID_MODE, PreferencesManager.loadMode());

        PreferencesManager.saveInterval(500);
        assertEquals(500, PreferencesManager.loadInterval());

        PreferencesManager.savePosition(OverlayCountdown.Corner.TOP_LEFT);
        assertEquals(OverlayCountdown.Corner.TOP_LEFT, PreferencesManager.loadPosition());

        PreferencesManager.saveIgnoreLeadingWhitespace(true);
        assertTrue(PreferencesManager.loadIgnoreLeadingWhitespace());

        PreferencesManager.saveWindowBounds(-8.0, 120.5, 800, 600);
        assertArrayEquals(new double[]{-8.0, 120.5, 800, 600}, PreferencesManager.loadWindowBounds());
    }

    @Test
    void fallsBackToDefaultsWhenNothingStored() {
        PreferencesManager.setStore(memory);

        assertEquals("", PreferencesManager.loadLanguage());
        assertEquals(ControlCenter.Mode.COMPATIBLE_MODE, PreferencesManager.loadMode());
        assertEquals(35, PreferencesManager.loadInterval());
        assertEquals(OverlayCountdown.Corner.BOTTOM_RIGHT, PreferencesManager.loadPosition());
        assertTrue(!PreferencesManager.loadIgnoreLeadingWhitespace());
        assertNull(PreferencesManager.loadWindowBounds());
    }

    @Test
    void fallsBackToDefaultsOnCorruptedValues() {
        PreferencesManager.setStore(memory);
        data.put("mode", "GARBAGE");
        data.put("interval", "abc");
        data.put("position", "NOT_A_CORNER");
        data.put("ignoreLeadingWhitespace", "not-a-boolean");
        data.put("windowBounds", "1,2,3");

        assertEquals(ControlCenter.Mode.COMPATIBLE_MODE, PreferencesManager.loadMode());
        assertEquals(35, PreferencesManager.loadInterval());
        assertEquals(OverlayCountdown.Corner.BOTTOM_RIGHT, PreferencesManager.loadPosition());
        assertTrue(!PreferencesManager.loadIgnoreLeadingWhitespace());
        assertNull(PreferencesManager.loadWindowBounds());
    }

    @Test
    void rejectsOutOfRangeIntervalAndBounds() {
        PreferencesManager.setStore(memory);
        data.put("interval", "-1");
        assertEquals(35, PreferencesManager.loadInterval());

        data.put("interval", "1001");
        assertEquals(35, PreferencesManager.loadInterval());

        data.put("windowBounds", "NaN,1,2,3");
        assertNull(PreferencesManager.loadWindowBounds());

        data.put("windowBounds", "1,1,0,600");
        assertNull(PreferencesManager.loadWindowBounds());
    }
}
