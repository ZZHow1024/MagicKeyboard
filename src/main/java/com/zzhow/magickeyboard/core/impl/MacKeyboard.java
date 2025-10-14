package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.zzhow.magickeyboard.core.IKeyboard;

/**
 * 模拟键盘输入-macOS 实现类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/10/14
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

        void CGEventKeyboardSetUnicodeString(Pointer event, long stringLength, char[] unicodeString);
    }

    private static final int kCGSessionEventTap = 1;

    @Override
    public void sendText(String text) {
        // 重置状态
        isStopped = false;
        isPaused = false;

        for (int i = 0; i < text.length(); i++) {
            // 检查是否停止
            if (isStopped) {
                break;
            }

            // 检查是否暂停
            while (isPaused) {
                synchronized (pauseLock) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                // 再次检查是否停止
                if (isStopped) {
                    break;
                }
            }

            // 如果停止则退出循环
            if (isStopped) {
                break;
            }

            char c = text.charAt(i);

            // 处理特殊按键
            if (c == '\n') { // 回车键
                sendSpecialKey((short)36); // kVK_Return
                continue;
            } else if (c == '\t') { // Tab键
                sendSpecialKey((short)48); // kVK_Tab
                continue;
            }

            sendChar(c);
            try {
                Thread.sleep(10); // 添加延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        isStopped = true;
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

    // 发送特殊按键
    private void sendSpecialKey(short virtualKey) {
        // 创建按键按下事件
        Pointer keyDown = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, virtualKey, true);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyDown);

        // 创建按键释放事件
        Pointer keyUp = CoreGraphics.INSTANCE.CGEventCreateKeyboardEvent(null, virtualKey, false);
        CoreGraphics.INSTANCE.CGEventPost(kCGSessionEventTap, keyUp);

        // 释放资源
        CoreGraphics.INSTANCE.CFRelease(keyDown);
        CoreGraphics.INSTANCE.CFRelease(keyUp);
    }

    @Override
    public void stop() {
        isStopped = true;
        isPaused = false;
        // 唤醒可能处于暂停状态的线程
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
        // 唤醒暂停的线程
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        // 等待键盘键入完毕
        while (!isStopped) {
            synchronized (pauseLock) {
                try {
                    // 等待直到键盘输入完成或停止
                    pauseLock.wait(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
