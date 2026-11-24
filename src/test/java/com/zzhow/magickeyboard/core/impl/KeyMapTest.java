package com.zzhow.magickeyboard.core.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平台键位映射单元测试
 *
 * @author ZZHow
 * create 2026/11/23
 */
class KeyMapTest {

    // ---------- Windows ----------

    @Test
    void windowsKeyMapCoversLettersAndDigits() {
        for (char c = 'a'; c <= 'z'; c++) {
            assertNotNull(WindowsKeyboard.KEY_MAP.get(c), "missing key for " + c);
            assertNotNull(WindowsKeyboard.KEY_MAP.get(Character.toUpperCase(c)), "missing key for " + Character.toUpperCase(c));
        }
        for (char c = '0'; c <= '9'; c++) {
            assertNotNull(WindowsKeyboard.KEY_MAP.get(c), "missing key for " + c);
        }
    }

    @Test
    void windowsLettersShareVirtualKeyWithUpperAndLower() {
        assertEquals(0x41, WindowsKeyboard.KEY_MAP.get('a'));
        assertEquals(0x41, WindowsKeyboard.KEY_MAP.get('A'));
        assertEquals(0x5A, WindowsKeyboard.KEY_MAP.get('z'));
        assertEquals(0x5A, WindowsKeyboard.KEY_MAP.get('Z'));
    }

    @Test
    void windowsShiftedSymbolsNeedShift() {
        assertTrue(WindowsKeyboard.NEEDS_SHIFT.get('!'));
        assertFalse(WindowsKeyboard.NEEDS_SHIFT.get('1'));
        assertTrue(WindowsKeyboard.NEEDS_SHIFT.get('('));
        assertFalse(WindowsKeyboard.NEEDS_SHIFT.get(' '));
    }

    @Test
    void windowsShiftSymbolsMapToBaseKey() {
        assertEquals(0x31, WindowsKeyboard.KEY_MAP.get('!'));  // Shift+1
        assertEquals(0x30, WindowsKeyboard.KEY_MAP.get(')'));  // Shift+0
        assertEquals(0xBD, WindowsKeyboard.KEY_MAP.get('_'));  // Shift+-
    }

    // ---------- Linux ----------

    @Test
    void linuxNeedsShiftKeyMatchesUsLayout() {
        assertTrue(LinuxKeyboard.needsShiftKey('A'));
        assertFalse(LinuxKeyboard.needsShiftKey('a'));
        assertTrue(LinuxKeyboard.needsShiftKey('!'));
        assertFalse(LinuxKeyboard.needsShiftKey('1'));
        assertTrue(LinuxKeyboard.needsShiftKey('~'));
        assertFalse(LinuxKeyboard.needsShiftKey('-'));
        assertFalse(LinuxKeyboard.needsShiftKey(' '));
        assertFalse(LinuxKeyboard.needsShiftKey('中'));
    }

    @Test
    void linuxCharToKeysymMapsShiftedSymbolsToBaseKey() {
        assertEquals('1', LinuxKeyboard.charToKeysym('!', true));
        assertEquals('2', LinuxKeyboard.charToKeysym('@', true));
        assertEquals('a', LinuxKeyboard.charToKeysym('A', true));
        assertEquals(';', LinuxKeyboard.charToKeysym(';', false));
        assertEquals('-', LinuxKeyboard.charToKeysym('-', false));
    }

    @Test
    void linuxCharToKeysymMapsPlainCharacters() {
        assertEquals('z', LinuxKeyboard.charToKeysym('z', false));
        assertEquals('5', LinuxKeyboard.charToKeysym('5', false));
        assertEquals(0xE9, LinuxKeyboard.charToKeysym(0xE9, false));   // Latin-1: é
        assertEquals(0x01000000L + 0x4E2D, LinuxKeyboard.charToKeysym(0x4E2D, false)); // 中
    }

    @Test
    void linuxCharToKeysymSupportsSupplementaryCodePoints() {
        // Emoji 😀 (U+1F600)：修复前按 char 截断，修复后保留完整码点
        assertEquals(0x01000000L + 0x1F600, LinuxKeyboard.charToKeysym(0x1F600, false));
    }
}
