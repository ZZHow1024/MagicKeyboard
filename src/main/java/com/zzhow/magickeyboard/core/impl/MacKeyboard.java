package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.zzhow.magickeyboard.core.IKeyboard;

/**
 * 模拟键盘输入-macOS 实现类
 *
 * @author ZZHow
 * @date 2025/10/13
 */
public class MacKeyboard implements IKeyboard {

    public interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

        Pointer CGEventCreateKeyboardEvent(Pointer source, short virtualKey, boolean keyDown);

        void CGEventPost(int tap, Pointer event);

        void CFRelease(Pointer cf);

        void CGEventKeyboardSetUnicodeString(Pointer event, long stringLength, char[] unicodeString);
    }

    private static final int kCGSessionEventTap = 1;

    @Override
    public void sendText(String text) {
        for (char c : text.toCharArray()) {
            sendChar(c);
            try {
                Thread.sleep(10); // 添加延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // 使用 Unicode 字符串
    private void sendChar(char c) {
        // 创建按键按下事件
        Pointer keyDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 0, true);
        char[] chars = {c};
        CoreGraphics.INSTANCE.CGEventKeyboardSetUnicodeString(keyDown, 1, chars);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyDown);

        // 创建按键释放事件
        Pointer keyUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, (short) 0, false);
        CoreGraphics.INSTANCE.CGEventKeyboardSetUnicodeString(keyUp, 1, chars);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyUp);

        // 释放资源
        CoreGraphics.INSTANCE.CFRelease(keyDown);
        CoreGraphics.INSTANCE.CFRelease(keyUp);
    }
}
