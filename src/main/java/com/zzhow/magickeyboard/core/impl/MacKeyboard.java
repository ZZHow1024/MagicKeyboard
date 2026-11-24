package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.zzhow.magickeyboard.core.ControlCenter;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟键盘输入-macOS 实现类
 * <p>
 * 兼容模式：虚拟键码 + Shift 组合键；极速模式：CGEventKeyboardSetUnicodeString 事件。
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2026/11/23
 */
public class MacKeyboard extends AbstractKeyboard {

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
    private static final short kVK_Shift = 56;
    private static final short kVK_Return = 36;
    private static final short kVK_Tab = 48;

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
    protected void sendNewLine() {
        sendSpecialKey(kVK_Return);
    }

    @Override
    protected void sendTab() {
        sendSpecialKey(kVK_Tab);
    }

    @Override
    protected void sendCodePoint(int codePoint, ControlCenter.Mode mode) {
        if (Character.isSupplementaryCodePoint(codePoint)) {
            // 补充字符（如 Emoji）整个码点一次性发送，修复原先代理对被拆开无法输入的问题
            sendUnicodeString(Character.toChars(codePoint));
            return;
        }

        char c = (char) codePoint;
        if (mode == ControlCenter.Mode.RAPID_MODE) {
            // 极速模式：所有字符都使用Unicode方式
            sendCharUnicode(c);
        } else {
            // 兼容模式：使用虚拟键码方式
            sendChar(c);
        }
    }

    private void sendChar(char c) {
        Short virtualKey = KEY_MAP.get(c);

        if (virtualKey == null) {
            // 如果没有映射,尝试使用Unicode方式(作为后备方案)
            sendCharUnicode(c);
            return;
        }

        boolean needsShift = Character.isUpperCase(c)
                || Boolean.TRUE.equals(NEEDS_SHIFT.get(c));

        if (needsShift) {
            // 按下Shift
            Pointer shiftDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, kVK_Shift, true);
            CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, shiftDown);
            CoreGraphics.INSTANCE.CFRelease(shiftDown);

            sleepInterval(); // 短暂延迟确保Shift被识别
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
            sleepInterval(); // 短暂延迟

            // 释放Shift
            Pointer shiftUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, kVK_Shift, false);
            CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, shiftUp);
            CoreGraphics.INSTANCE.CFRelease(shiftUp);
        }
    }

    // Unicode方式（用于极速模式或没有映射的特殊字符）
    private void sendCharUnicode(char c) {
        sendUnicodeString(new char[]{c});
    }

    private void sendUnicodeString(char[] chars) {
        Pointer keyDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 0, true);
        CoreGraphics.INSTANCE.CGEventKeyboardSetUnicodeString(keyDown, chars.length, chars);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyDown);
        CoreGraphics.INSTANCE.CFRelease(keyDown);

        Pointer keyUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 0, false);
        CoreGraphics.INSTANCE.CGEventKeyboardSetUnicodeString(keyUp, chars.length, chars);
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
}
