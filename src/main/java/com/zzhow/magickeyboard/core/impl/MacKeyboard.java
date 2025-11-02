package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.util.HashMap;
import java.util.Map;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.IKeyboard;

/**
 * 模拟键盘输入-macOS 实现类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/11/2
 */
public class MacKeyboard implements IKeyboard {

    private volatile boolean isStopped = false;
    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();


    public interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

        Pointer CGEventCreateKeyboardEvent(Pointer source, short virtualKey, boolean keyDown);

        void CGEventPost(int tap, Pointer event);

        void CFRelease(Pointer cf);

        void CGEventSetFlags(Pointer event, long flags);

        void CGEventKeyboardSetUnicodeString(Pointer event, long stringLength, char[] unicodeString);
    }

    private static final int kCGSessionEventTap = 1;
    private static final long kCGEventFlagMaskShift = 0x00020002;

    // macOS 虚拟键码映射
    private static final Map<Character, Short> KEY_MAP = new HashMap<>();
    private static final Map<Character, Boolean> NEEDS_SHIFT = new HashMap<>();

    static {
        // 数字键 (不需要Shift)
        KEY_MAP.put('1', (short) 18);
        NEEDS_SHIFT.put('1', false);
        KEY_MAP.put('2', (short) 19);
        NEEDS_SHIFT.put('2', false);
        KEY_MAP.put('3', (short) 20);
        NEEDS_SHIFT.put('3', false);
        KEY_MAP.put('4', (short) 21);
        NEEDS_SHIFT.put('4', false);
        KEY_MAP.put('5', (short) 23);
        NEEDS_SHIFT.put('5', false);
        KEY_MAP.put('6', (short) 22);
        NEEDS_SHIFT.put('6', false);
        KEY_MAP.put('7', (short) 26);
        NEEDS_SHIFT.put('7', false);
        KEY_MAP.put('8', (short) 28);
        NEEDS_SHIFT.put('8', false);
        KEY_MAP.put('9', (short) 25);
        NEEDS_SHIFT.put('9', false);
        KEY_MAP.put('0', (short) 29);
        NEEDS_SHIFT.put('0', false);

        // 数字键对应的Shift符号
        KEY_MAP.put('!', (short) 18);
        NEEDS_SHIFT.put('!', true);  // Shift+1
        KEY_MAP.put('@', (short) 19);
        NEEDS_SHIFT.put('@', true);  // Shift+2
        KEY_MAP.put('#', (short) 20);
        NEEDS_SHIFT.put('#', true);  // Shift+3
        KEY_MAP.put('$', (short) 21);
        NEEDS_SHIFT.put('$', true);  // Shift+4
        KEY_MAP.put('%', (short) 23);
        NEEDS_SHIFT.put('%', true);  // Shift+5
        KEY_MAP.put('^', (short) 22);
        NEEDS_SHIFT.put('^', true);  // Shift+6
        KEY_MAP.put('&', (short) 26);
        NEEDS_SHIFT.put('&', true);  // Shift+7
        KEY_MAP.put('*', (short) 28);
        NEEDS_SHIFT.put('*', true);  // Shift+8
        KEY_MAP.put('(', (short) 25);
        NEEDS_SHIFT.put('(', true);  // Shift+9
        KEY_MAP.put(')', (short) 29);
        NEEDS_SHIFT.put(')', true);  // Shift+0

        // 字母键 (小写不需要Shift, 大写需要)
        KEY_MAP.put('a', (short) 0);
        KEY_MAP.put('A', (short) 0);
        KEY_MAP.put('b', (short) 11);
        KEY_MAP.put('B', (short) 11);
        KEY_MAP.put('c', (short) 8);
        KEY_MAP.put('C', (short) 8);
        KEY_MAP.put('d', (short) 2);
        KEY_MAP.put('D', (short) 2);
        KEY_MAP.put('e', (short) 14);
        KEY_MAP.put('E', (short) 14);
        KEY_MAP.put('f', (short) 3);
        KEY_MAP.put('F', (short) 3);
        KEY_MAP.put('g', (short) 5);
        KEY_MAP.put('G', (short) 5);
        KEY_MAP.put('h', (short) 4);
        KEY_MAP.put('H', (short) 4);
        KEY_MAP.put('i', (short) 34);
        KEY_MAP.put('I', (short) 34);
        KEY_MAP.put('j', (short) 38);
        KEY_MAP.put('J', (short) 38);
        KEY_MAP.put('k', (short) 40);
        KEY_MAP.put('K', (short) 40);
        KEY_MAP.put('l', (short) 37);
        KEY_MAP.put('L', (short) 37);
        KEY_MAP.put('m', (short) 46);
        KEY_MAP.put('M', (short) 46);
        KEY_MAP.put('n', (short) 45);
        KEY_MAP.put('N', (short) 45);
        KEY_MAP.put('o', (short) 31);
        KEY_MAP.put('O', (short) 31);
        KEY_MAP.put('p', (short) 35);
        KEY_MAP.put('P', (short) 35);
        KEY_MAP.put('q', (short) 12);
        KEY_MAP.put('Q', (short) 12);
        KEY_MAP.put('r', (short) 15);
        KEY_MAP.put('R', (short) 15);
        KEY_MAP.put('s', (short) 1);
        KEY_MAP.put('S', (short) 1);
        KEY_MAP.put('t', (short) 17);
        KEY_MAP.put('T', (short) 17);
        KEY_MAP.put('u', (short) 32);
        KEY_MAP.put('U', (short) 32);
        KEY_MAP.put('v', (short) 9);
        KEY_MAP.put('V', (short) 9);
        KEY_MAP.put('w', (short) 13);
        KEY_MAP.put('W', (short) 13);
        KEY_MAP.put('x', (short) 7);
        KEY_MAP.put('X', (short) 7);
        KEY_MAP.put('y', (short) 16);
        KEY_MAP.put('Y', (short) 16);
        KEY_MAP.put('z', (short) 6);
        KEY_MAP.put('Z', (short) 6);

        // 符号键
        KEY_MAP.put('-', (short) 27);
        NEEDS_SHIFT.put('-', false);
        KEY_MAP.put('_', (short) 27);
        NEEDS_SHIFT.put('_', true);  // Shift+-
        KEY_MAP.put('=', (short) 24);
        NEEDS_SHIFT.put('=', false);
        KEY_MAP.put('+', (short) 24);
        NEEDS_SHIFT.put('+', true);  // Shift+=
        KEY_MAP.put('[', (short) 33);
        NEEDS_SHIFT.put('[', false);
        KEY_MAP.put('{', (short) 33);
        NEEDS_SHIFT.put('{', true);  // Shift+[
        KEY_MAP.put(']', (short) 30);
        NEEDS_SHIFT.put(']', false);
        KEY_MAP.put('}', (short) 30);
        NEEDS_SHIFT.put('}', true);  // Shift+]
        KEY_MAP.put('\\', (short) 42);
        NEEDS_SHIFT.put('\\', false);
        KEY_MAP.put('|', (short) 42);
        NEEDS_SHIFT.put('|', true);  // Shift+\
        KEY_MAP.put(';', (short) 41);
        NEEDS_SHIFT.put(';', false);
        KEY_MAP.put(':', (short) 41);
        NEEDS_SHIFT.put(':', true);  // Shift+;
        KEY_MAP.put('\'', (short) 39);
        NEEDS_SHIFT.put('\'', false);
        KEY_MAP.put('"', (short) 39);
        NEEDS_SHIFT.put('"', true);  // Shift+'
        KEY_MAP.put(',', (short) 43);
        NEEDS_SHIFT.put(',', false);
        KEY_MAP.put('<', (short) 43);
        NEEDS_SHIFT.put('<', true);  // Shift+,
        KEY_MAP.put('.', (short) 47);
        NEEDS_SHIFT.put('.', false);
        KEY_MAP.put('>', (short) 47);
        NEEDS_SHIFT.put('>', true);  // Shift+.
        KEY_MAP.put('/', (short) 44);
        NEEDS_SHIFT.put('/', false);
        KEY_MAP.put('?', (short) 44);
        NEEDS_SHIFT.put('?', true);  // Shift+/
        KEY_MAP.put('`', (short) 50);
        NEEDS_SHIFT.put('`', false);
        KEY_MAP.put('~', (short) 50);
        NEEDS_SHIFT.put('~', true);  // Shift+`
        KEY_MAP.put(' ', (short) 49);
        NEEDS_SHIFT.put(' ', false);  // 空格
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
                sendSpecialKey((short) 36); // kVK_Return
                continue;
            } else if (c == '\t') {
                sendSpecialKey((short) 48); // kVK_Tab
                continue;
            }

            // 根据模式选择发送方式
            if (mode == ControlCenter.Mode.RAPID_MODE) {
                // 极速模式：所有字符都使用Unicode方式
                sendCharUnicode(c);
            } else {
                // 兼容模式：使用原有的虚拟键码方式
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
        Short virtualKey = KEY_MAP.get(c);

        if (virtualKey == null) {
            // 如果没有映射,尝试使用Unicode方式(作为后备方案)
            sendCharUnicode(c);
            return;
        }

        boolean needsShift = Character.isUpperCase(c) ||
                (NEEDS_SHIFT.containsKey(c) && NEEDS_SHIFT.get(c));

        if (needsShift) {
            // 按下Shift
            Pointer shiftDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 56, true);
            CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, shiftDown);
            CoreGraphics.INSTANCE.CFRelease(shiftDown);

            try {
                Thread.sleep(ControlCenter.timeInterval); // 短暂延迟确保Shift被识别
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 按下按键
        Pointer keyDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, virtualKey, true);
        if (needsShift) {
            CoreGraphics.INSTANCE.CGEventSetFlags(keyDown, kCGEventFlagMaskShift);
        }
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyDown);
        CoreGraphics.INSTANCE.CFRelease(keyDown);

        // 释放按键
        Pointer keyUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, virtualKey, false);
        if (needsShift) {
            CoreGraphics.INSTANCE.CGEventSetFlags(keyUp, kCGEventFlagMaskShift);
        }
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyUp);
        CoreGraphics.INSTANCE.CFRelease(keyUp);

        if (needsShift) {
            try {
                Thread.sleep(ControlCenter.timeInterval); // 短暂延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 释放Shift
            Pointer shiftUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 56, false);
            CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, shiftUp);
            CoreGraphics.INSTANCE.CFRelease(shiftUp);
        }
    }

    // Unicode方式（用于极速模式或没有映射的特殊字符）
    private void sendCharUnicode(char c) {
        Pointer keyDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 0, true);
        char[] chars = {c};
        CoreGraphics.INSTANCE.CGEventKeyboardSetUnicodeString(keyDown, 1, chars);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyDown);
        CoreGraphics.INSTANCE.CFRelease(keyDown);

        Pointer keyUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 0, false);
        CoreGraphics.INSTANCE.CGEventKeyboardSetUnicodeString(keyUp, 1, chars);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyUp);
        CoreGraphics.INSTANCE.CFRelease(keyUp);
    }

    private void sendSpecialKey(short virtualKey) {
        Pointer keyDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, virtualKey, true);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyDown);
        CoreGraphics.INSTANCE.CFRelease(keyDown);

        Pointer keyUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, virtualKey, false);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyUp);
        CoreGraphics.INSTANCE.CFRelease(keyUp);
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