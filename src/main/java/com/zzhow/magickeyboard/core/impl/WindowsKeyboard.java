package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;
import com.zzhow.magickeyboard.core.ControlCenter;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟键盘输入-Windows 实现类
 * <p>
 * 兼容模式：虚拟键码 + Shift 组合键；极速模式：KEYEVENTF_UNICODE 事件。
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2026/11/23
 */
public class WindowsKeyboard extends AbstractKeyboard {

    // Windows 虚拟键码（WinUser 未定义的部分）
    private static final int VK_RETURN = 0x0D;
    private static final int VK_TAB = 0x09;
    private static final int VK_ESCAPE = 0x1B;
    private static final int VK_CONTROL = 0x11;
    private static final int VK_MENU = 0x12;
    private static final int VK_SHIFT = WinUser.VK_SHIFT;

    private interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        int SendInput(int nInputs, WinUser.INPUT[] pInputs, int cbSize);
    }

    // Windows 虚拟键码映射（包级私有，便于单元测试）
    static final Map<Character, Integer> KEY_MAP = new HashMap<>();
    static final Map<Character, Boolean> NEEDS_SHIFT = new HashMap<>();

    static {
        // 数字键
        for (char c = '0'; c <= '9'; c++) {
            KEY_MAP.put(c, 0x30 + (c - '0'));
            NEEDS_SHIFT.put(c, false);
        }

        // 数字键的Shift符号
        KEY_MAP.put(')', 0x30);
        NEEDS_SHIFT.put(')', true);  // Shift+0
        KEY_MAP.put('!', 0x31);
        NEEDS_SHIFT.put('!', true);  // Shift+1
        KEY_MAP.put('@', 0x32);
        NEEDS_SHIFT.put('@', true);  // Shift+2
        KEY_MAP.put('#', 0x33);
        NEEDS_SHIFT.put('#', true);  // Shift+3
        KEY_MAP.put('$', 0x34);
        NEEDS_SHIFT.put('$', true);  // Shift+4
        KEY_MAP.put('%', 0x35);
        NEEDS_SHIFT.put('%', true);  // Shift+5
        KEY_MAP.put('^', 0x36);
        NEEDS_SHIFT.put('^', true);  // Shift+6
        KEY_MAP.put('&', 0x37);
        NEEDS_SHIFT.put('&', true);  // Shift+7
        KEY_MAP.put('*', 0x38);
        NEEDS_SHIFT.put('*', true);  // Shift+8
        KEY_MAP.put('(', 0x39);
        NEEDS_SHIFT.put('(', true);  // Shift+9

        // 字母键
        for (char c = 'A'; c <= 'Z'; c++) {
            KEY_MAP.put(c, 0x41 + (c - 'A'));
            KEY_MAP.put(Character.toLowerCase(c), 0x41 + (c - 'A'));
        }

        // 符号键
        KEY_MAP.put(' ', 0x20);
        NEEDS_SHIFT.put(' ', false);  // 空格
        KEY_MAP.put('-', 0xBD);
        NEEDS_SHIFT.put('-', false);
        KEY_MAP.put('_', 0xBD);
        NEEDS_SHIFT.put('_', true);   // Shift+-
        KEY_MAP.put('=', 0xBB);
        NEEDS_SHIFT.put('=', false);
        KEY_MAP.put('+', 0xBB);
        NEEDS_SHIFT.put('+', true);   // Shift+=
        KEY_MAP.put('[', 0xDB);
        NEEDS_SHIFT.put('[', false);
        KEY_MAP.put('{', 0xDB);
        NEEDS_SHIFT.put('{', true);   // Shift+[
        KEY_MAP.put(']', 0xDD);
        NEEDS_SHIFT.put(']', false);
        KEY_MAP.put('}', 0xDD);
        NEEDS_SHIFT.put('}', true);   // Shift+]
        KEY_MAP.put('\\', 0xDC);
        NEEDS_SHIFT.put('\\', false);
        KEY_MAP.put('|', 0xDC);
        NEEDS_SHIFT.put('|', true);   // Shift+\
        KEY_MAP.put(';', 0xBA);
        NEEDS_SHIFT.put(';', false);
        KEY_MAP.put(':', 0xBA);
        NEEDS_SHIFT.put(':', true);   // Shift+;
        KEY_MAP.put('\'', 0xDE);
        NEEDS_SHIFT.put('\'', false);
        KEY_MAP.put('"', 0xDE);
        NEEDS_SHIFT.put('"', true);   // Shift+'
        KEY_MAP.put(',', 0xBC);
        NEEDS_SHIFT.put(',', false);
        KEY_MAP.put('<', 0xBC);
        NEEDS_SHIFT.put('<', true);   // Shift+,
        KEY_MAP.put('.', 0xBE);
        NEEDS_SHIFT.put('.', false);
        KEY_MAP.put('>', 0xBE);
        NEEDS_SHIFT.put('>', true);   // Shift+.
        KEY_MAP.put('/', 0xBF);
        NEEDS_SHIFT.put('/', false);
        KEY_MAP.put('?', 0xBF);
        NEEDS_SHIFT.put('?', true);   // Shift+/
        KEY_MAP.put('`', 0xC0);
        NEEDS_SHIFT.put('`', false);
        KEY_MAP.put('~', 0xC0);
        NEEDS_SHIFT.put('~', true);   // Shift+`
    }

    @Override
    protected void sendNewLine() {
        sendVirtualKey(VK_RETURN, false);
        sendVirtualKey(VK_RETURN, true);
    }

    @Override
    protected void sendTab() {
        sendVirtualKey(VK_TAB, false);
        sendVirtualKey(VK_TAB, true);
    }

    @Override
    protected void sendEscapeKey() {
        sendVirtualKey(VK_ESCAPE, false);
        sendVirtualKey(VK_ESCAPE, true);
    }

    @Override
    protected void sendComboKey(java.util.Set<KeyMarkers.Modifier> modifiers, char key) {
        int[] modifierVks = modifierVirtualKeys(modifiers);
        int target = targetVirtualKey(key);

        for (int vk : modifierVks) {
            sendVirtualKey(vk, false);
        }
        sendVirtualKey(target, false);
        sendVirtualKey(target, true);
        for (int i = modifierVks.length - 1; i >= 0; i--) {
            sendVirtualKey(modifierVks[i], true);
        }
    }

    private static int[] modifierVirtualKeys(java.util.Set<KeyMarkers.Modifier> modifiers) {
        java.util.List<Integer> vks = new java.util.ArrayList<>();
        if (modifiers.contains(KeyMarkers.Modifier.CTRL)) {
            vks.add(VK_CONTROL);
        }
        if (modifiers.contains(KeyMarkers.Modifier.ALT)) {
            vks.add(VK_MENU);
        }
        if (modifiers.contains(KeyMarkers.Modifier.SHIFT)) {
            vks.add(VK_SHIFT);
        }
        return vks.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int targetVirtualKey(char key) {
        if (key >= 'A' && key <= 'Z') {
            return 0x41 + (key - 'A');
        }
        return 0x30 + (key - '0'); // 0-9，解析器保证取值范围
    }

    @Override
    protected void sendCodePoint(int codePoint, ControlCenter.Mode mode) {
        if (Character.isSupplementaryCodePoint(codePoint)) {
            // Windows 的 KEYEVENTF_UNICODE 按 UTF-16 编码单元发送，补充字符需拆成高、低位代理两次事件
            char[] units = Character.toChars(codePoint);
            sendUnicodeChar(units[0]);
            sendUnicodeChar(units[1]);
            return;
        }

        char c = (char) codePoint;
        if (mode == ControlCenter.Mode.RAPID_MODE) {
            // 极速模式：所有字符都使用Unicode方式
            sendUnicodeChar(c);
        } else {
            // 兼容模式：使用虚拟键码方式
            sendChar(c);
        }
    }

    private void sendChar(char c) {
        Integer virtualKey = KEY_MAP.get(c);

        if (virtualKey == null) {
            // 如果没有映射,使用Unicode方式
            sendUnicodeChar(c);
            return;
        }

        boolean needsShift = Character.isUpperCase(c)
                || Boolean.TRUE.equals(NEEDS_SHIFT.get(c));

        if (needsShift) {
            // 按下Shift
            sendVirtualKey(VK_SHIFT, false);
            sleepInterval();
        }

        // 按下按键
        sendVirtualKey(virtualKey, false);
        // 释放按键
        sendVirtualKey(virtualKey, true);

        if (needsShift) {
            sleepInterval();
            // 释放Shift
            sendVirtualKey(VK_SHIFT, true);
        }
    }

    private void sendUnicodeChar(char c) {
        sendUnicodeKey(c, false);
        sendUnicodeKey(c, true);
    }

    private void sendUnicodeKey(char c, boolean keyUp) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input = new WinUser.INPUT.INPUT_UNION();
        input.input.ki = new WinUser.KEYBDINPUT();

        int flags = WinUser.KEYBDINPUT.KEYEVENTF_UNICODE;
        if (keyUp) {
            flags |= WinUser.KEYBDINPUT.KEYEVENTF_KEYUP;
        }

        input.input.ki.wVk = new WinDef.WORD(0);
        input.input.ki.wScan = new WinDef.WORD(c);
        input.input.ki.dwFlags = new WinDef.DWORD(flags);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = null;
        input.input.setType(WinUser.KEYBDINPUT.class);
        input.write();

        User32.INSTANCE.SendInput(1, new WinUser.INPUT[]{input}, input.size());
    }

    private void sendVirtualKey(int vk, boolean keyUp) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input = new WinUser.INPUT.INPUT_UNION();
        input.input.ki = new WinUser.KEYBDINPUT();

        input.input.ki.wVk = new WinDef.WORD(vk);
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.dwFlags = new WinDef.DWORD(keyUp ? WinUser.KEYBDINPUT.KEYEVENTF_KEYUP : 0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = null;
        input.input.setType(WinUser.KEYBDINPUT.class);
        input.write();

        User32.INSTANCE.SendInput(1, new WinUser.INPUT[]{input}, input.size());
    }
}
