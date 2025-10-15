package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;
import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.IKeyboard;

import java.util.Arrays;
import java.util.List;

/**
 * 模拟键盘输入-Windows 实现类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/10/15
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
        int KEYEVENTF_KEYUP = 0x0002;        // 按键释放
        int KEYEVENTF_UNICODE = 0x0004;      // Unicode 字符
        int KEYEVENTF_SCANCODE = 0x0008;     // 扫描码

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
            public WinDef.WORD wVk;           // 虚拟键码
            public WinDef.WORD wScan;         // 扫描码或 Unicode 字符
            public WinDef.DWORD dwFlags;      // 标志
            public WinDef.DWORD time;         // 时间戳
            public Pointer dwExtraInfo;       // 额外信息

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
                sendSpecialKey(0x0D); // VK_RETURN
                continue;
            } else if (c == '\t') { // Tab键
                sendSpecialKey(0x09); // VK_TAB
                continue;
            }

            // 处理代理对（Surrogate Pairs）- 用于支持某些特殊字符和 Emoji
            if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                char low = text.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    // 发送高代理字符
                    sendUnicodeChar(c);
                    // 发送低代理字符
                    sendUnicodeChar(low);
                    i++; // 跳过下一个字符
                    continue;
                }
            }

            sendUnicodeChar(c);
            try {
                Thread.sleep(ControlCenter.timeInterval); // 添加延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        isStopped = true;
    }

    private void sendUnicodeChar(char c) {
        // 按键按下
        sendUnicodeKey(c, false);
        // 按键释放
        sendUnicodeKey(c, true);
    }

    private void sendUnicodeKey(char c, boolean keyUp) {
        User32.INPUT input = new User32.INPUT();
        input.type = User32.INPUT_KEYBOARD;
        input.input = new User32.INPUT.INPUT_UNION();
        input.input.ki = new User32.KEYBDINPUT();

        // 使用 Unicode 模式
        input.input.ki.wVk = new WinDef.WORD(0);  // 虚拟键码设为 0
        input.input.ki.wScan = new WinDef.WORD(c); // Unicode 字符放在 wScan 中
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = null;

        // 设置标志：KEYEVENTF_UNICODE + KEYEVENTF_KEYUP
        int flags = User32.KEYEVENTF_UNICODE;
        if (keyUp) {
            flags |= User32.KEYEVENTF_KEYUP;
        }
        input.input.ki.dwFlags = new WinDef.DWORD(flags);

        // 关键：必须先设置 Union 类型，再写入
        input.input.setType(User32.KEYBDINPUT.class);
        input.write();

        User32.INSTANCE.SendInput(1, new User32.INPUT[]{input}, input.size());
    }

    // 发送特殊按键（如 Enter、Tab 等）
    public void sendSpecialKey(int virtualKeyCode) {
        sendVirtualKey(virtualKeyCode, false); // 按下
        sendVirtualKey(virtualKeyCode, true);  // 释放
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

        // 关键：必须先设置 Union 类型，再写入
        input.input.setType(User32.KEYBDINPUT.class);
        input.write();

        User32.INSTANCE.SendInput(1, new User32.INPUT[]{input}, input.size());
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