package com.zzhow.magickeyboard.core.hotkey;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 热键定义单元测试（纯逻辑，全平台可运行）
 *
 * @author ZZHow
 * create 2026/11/27
 */
class HotkeyTest {

    @Test
    void modifierMaskMatchesWindowsModConstants() {
        assertEquals(0x2, HotkeyModifier.CONTROL.mask());
        assertEquals(0x1, HotkeyModifier.ALT.mask());
        assertEquals(0x4, HotkeyModifier.SHIFT.mask());
    }

    @Test
    void combinesModifierMasks() {
        Hotkey hotkey = new Hotkey(1, Set.of(HotkeyModifier.CONTROL, HotkeyModifier.ALT), 'M');
        assertEquals(0x3, hotkey.modifierMask());

        Hotkey withShift = new Hotkey(2, Set.of(HotkeyModifier.CONTROL, HotkeyModifier.ALT, HotkeyModifier.SHIFT), 'S');
        assertEquals(0x7, withShift.modifierMask());

        Hotkey noModifier = new Hotkey(3, Set.of(), '5');
        assertEquals(0, noModifier.modifierMask());
    }

    @Test
    void defensiveCopiesModifierSet() {
        Hotkey hotkey = new Hotkey(1, Set.of(HotkeyModifier.CONTROL), 'M');
        assertEquals(Set.of(HotkeyModifier.CONTROL), hotkey.modifiers());
    }
}
