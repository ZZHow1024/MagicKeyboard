package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;
import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.IKeyboard;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟键盘输入-Windows 实现类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/11/2
 */
public class WindowsKeyboard implements IKeyboard {

    private volatile boolean isStopped = false;
    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        int INPUT_KEYBOARD = 1;
        int INPUT_MOUSE = 0;
        int INPUT_HARDWARE = 2;

        // 按键事件标志
        int KEYEVENTF_KEYUP = 0x0002;
        int KEYEVENTF_UNICODE = 0x0004;
        int KEYEVENTF_SCANCODE = 0x0008;

        // 虚拟键码
        int VK_SHIFT = 0x10;
        int VK_RETURN = 0x0D;
        int VK_TAB = 0x09;

        class INPUT extends Structure {
            public int type;
            public INPUT_UNION input;

            public INPUT() {
                super();
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("type", "input");
            }

            public static class INPUT_UNION extends Union {
                public MOUSEINPUT mi;
                public KEYBDINPUT ki;
                public HARDWAREINPUT hi;

                public INPUT_UNION() {
                    super();
                }
            }
        }

        class MOUSEINPUT extends Structure {
            public WinDef.LONG dx;
            public WinDef.LONG dy;
            public WinDef.DWORD mouseData;
            public WinDef.DWORD dwFlags;
            public WinDef.DWORD time;
            public Pointer dwExtraInfo;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("dx", "dy", "mouseData", "dwFlags", "time", "dwExtraInfo");
            }
        }

        class KEYBDINPUT extends Structure {
            public WinDef.WORD wVk;
            public WinDef.WORD wScan;
            public WinDef.DWORD dwFlags;
            public WinDef.DWORD time;
            public Pointer dwExtraInfo;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("wVk", "wScan", "dwFlags", "time", "dwExtraInfo");
            }
        }

        class HARDWAREINPUT extends Structure {
            public WinDef.DWORD uMsg;
            public WinDef.WORD wParamL;
            public WinDef.WORD wParamH;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("uMsg", "wParamL", "wParamH");
            }
        }

        int SendInput(int nInputs, INPUT[] pInputs, int cbSize);
    }

    // Windows 虚拟键码映射
    private static final Map<Character, Integer> KEY_MAP = new HashMap<>();
    private static final Map<Character, Boolean> NEEDS_SHIFT = new HashMap<>();

    static {
        // 数字键
        KEY_MAP.put('0', 0x30);
        NEEDS_SHIFT.put('0', false);
        KEY_MAP.put('1', 0x31);
        NEEDS_SHIFT.put('1', false);
        KEY_MAP.put('2', 0x32);
        NEEDS_SHIFT.put('2', false);
        KEY_MAP.put('3', 0x33);
        NEEDS_SHIFT.put('3', false);
        KEY_MAP.put('4', 0x34);
        NEEDS_SHIFT.put('4', false);
        KEY_MAP.put('5', 0x35);
        NEEDS_SHIFT.put('5', false);
        KEY_MAP.put('6', 0x36);
        NEEDS_SHIFT.put('6', false);
        KEY_MAP.put('7', 0x37);
        NEEDS_SHIFT.put('7', false);
        KEY_MAP.put('8', 0x38);
        NEEDS_SHIFT.put('8', false);
        KEY_MAP.put('9', 0x39);
        NEEDS_SHIFT.put('9', false);

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
    public void sendText(String text, ControlCenter.Mode mode) {
        isStopped = false;
        isPaused = false;

        for (int i = 0; i < text.length(); i++) {
            if (isStopped) {
                break;
            }

            while (isPaused) {
                synchronized (pauseLock) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (isStopped) {
                    break;
                }
            }

            if (isStopped) {
                break;
            }

            char c = text.charAt(i);

            // 处理特殊按键（两种模式都需要）
            if (c == '\n') {
                sendSpecialKey(User32.VK_RETURN);
                continue;
            } else if (c == '\t') {
                sendSpecialKey(User32.VK_TAB);
                continue;
            }

            // 根据模式选择发送方式
            if (mode == ControlCenter.Mode.RAPID_MODE) {
                // 极速模式：所有字符都使用Unicode方式
                // 处理代理对（Surrogate Pairs）
                if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                    char low = text.charAt(i + 1);
                    if (Character.isLowSurrogate(low)) {
                        sendUnicodeChar(c);
                        sendUnicodeChar(low);
                        i++;
                        continue;
                    }
                }
                sendUnicodeChar(c);
            } else {
                // 兼容模式：使用原有的虚拟键码方式
                // 处理代理对（Surrogate Pairs）
                if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                    char low = text.charAt(i + 1);
                    if (Character.isLowSurrogate(low)) {
                        sendUnicodeChar(c);
                        sendUnicodeChar(low);
                        i++;
                        continue;
                    }
                }
                sendChar(c);
            }

            try {
                Thread.sleep(ControlCenter.timeInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        isStopped = true;
    }

    private void sendChar(char c) {
        Integer virtualKey = KEY_MAP.get(c);

        if (virtualKey == null) {
            // 如果没有映射,使用Unicode方式
            sendUnicodeChar(c);
            return;
        }

        boolean needsShift = Character.isUpperCase(c) ||
                (NEEDS_SHIFT.containsKey(c) && NEEDS_SHIFT.get(c));

        if (needsShift) {
            // 按下Shift
            sendVirtualKey(User32.VK_SHIFT, false);
            try {
                Thread.sleep(ControlCenter.timeInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 按下按键
        sendVirtualKey(virtualKey, false);
        // 释放按键
        sendVirtualKey(virtualKey, true);

        if (needsShift) {
            try {
                Thread.sleep(ControlCenter.timeInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 释放Shift
            sendVirtualKey(User32.VK_SHIFT, true);
        }
    }

    private void sendUnicodeChar(char c) {
        sendUnicodeKey(c, false);
        sendUnicodeKey(c, true);
    }

    private void sendUnicodeKey(char c, boolean keyUp) {
        User32.INPUT input = new User32.INPUT();
        input.type = User32.INPUT_KEYBOARD;
        input.input = new User32.INPUT.INPUT_UNION();
        input.input.ki = new User32.KEYBDINPUT();

        input.input.ki.wVk = new WinDef.WORD(0);
        input.input.ki.wScan = new WinDef.WORD(c);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = null;

        int flags = User32.KEYEVENTF_UNICODE;
        if (keyUp) {
            flags |= User32.KEYEVENTF_KEYUP;
        }
        input.input.ki.dwFlags = new WinDef.DWORD(flags);

        input.input.setType(User32.KEYBDINPUT.class);
        input.write();

        User32.INSTANCE.SendInput(1, new User32.INPUT[]{input}, input.size());
    }

    private void sendSpecialKey(int virtualKeyCode) {
        sendVirtualKey(virtualKeyCode, false);
        sendVirtualKey(virtualKeyCode, true);
    }

    private void sendVirtualKey(int vk, boolean keyUp) {
        User32.INPUT input = new User32.INPUT();
        input.type = User32.INPUT_KEYBOARD;
        input.input = new User32.INPUT.INPUT_UNION();
        input.input.ki = new User32.KEYBDINPUT();

        input.input.ki.wVk = new WinDef.WORD(vk);
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = null;
        input.input.ki.dwFlags = new WinDef.DWORD(keyUp ? User32.KEYEVENTF_KEYUP : 0);

        input.input.setType(User32.KEYBDINPUT.class);
        input.write();

        User32.INSTANCE.SendInput(1, new User32.INPUT[]{input}, input.size());
    }

    @Override
    public void stop() {
        isStopped = true;
        isPaused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    @Override
    public void pause() {
        isPaused = true;
    }

    @Override
    public void resume() {
        isPaused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        while (!isStopped) {
            synchronized (pauseLock) {
                try {
                    pauseLock.wait(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}