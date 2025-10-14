package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;
import com.zzhow.magickeyboard.core.IKeyboard;

import java.util.Arrays;
import java.util.List;

/**
 * 模拟键盘输入-Windows 实现类
 *
 * @author ZZHow
 * @date 2025/10/13
 */
public class WindowsKeyboard implements IKeyboard {
    
    private volatile boolean isStopped = false;
    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        int INPUT_KEYBOARD = 1;

        // 按键事件标志
        int KEY_EVENT_KEYUP = 0x0002;        // 按键释放
        int KEY_EVENT_UNICODE = 0x0004;      // Unicode 字符

        class INPUT extends Structure {
            public int type;
            public INPUT_UNION inputUnion;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("type", "inputUnion");
            }

            public static class INPUT_UNION extends Union {
                public KEYBDINPUT ki;
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
                Thread.sleep(10); // 添加延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
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
        input.inputUnion = new User32.INPUT.INPUT_UNION();
        input.inputUnion.ki = new User32.KEYBDINPUT();

        // 使用 Unicode 模式
        input.inputUnion.ki.wVk = new WinDef.WORD(0);  // 虚拟键码设为 0
        input.inputUnion.ki.wScan = new WinDef.WORD(c); // Unicode 字符放在 wScan 中
        input.inputUnion.ki.time = new WinDef.DWORD(0);
        input.inputUnion.ki.dwExtraInfo = null;

        // 设置标志：KEY_EVENT_UNICODE + KEY_EVENT_KEYUP
        int flags = User32.KEY_EVENT_UNICODE;
        if (keyUp) {
            flags |= User32.KEY_EVENT_KEYUP;
        }
        input.inputUnion.ki.dwFlags = new WinDef.DWORD(flags);

        input.inputUnion.setType(User32.KEYBDINPUT.class);
        input.inputUnion.write();
        input.write();

        User32.INSTANCE.SendInput(1, new User32.INPUT[]{input}, input.size());
    }

    // 可选：如果需要发送特殊按键（如 Enter、Tab 等），可以使用虚拟键码
    public void sendSpecialKey(int virtualKeyCode) {
        sendVirtualKey(virtualKeyCode, false); // 按下
        sendVirtualKey(virtualKeyCode, true);  // 释放
    }

    private void sendVirtualKey(int vk, boolean keyUp) {
        User32.INPUT input = new User32.INPUT();
        input.type = User32.INPUT_KEYBOARD;
        input.inputUnion = new User32.INPUT.INPUT_UNION();
        input.inputUnion.ki = new User32.KEYBDINPUT();

        input.inputUnion.ki.wVk = new WinDef.WORD(vk);
        input.inputUnion.ki.wScan = new WinDef.WORD(0);
        input.inputUnion.ki.time = new WinDef.DWORD(0);
        input.inputUnion.ki.dwExtraInfo = null;
        input.inputUnion.ki.dwFlags = new WinDef.DWORD(keyUp ? User32.KEY_EVENT_KEYUP : 0);

        input.inputUnion.setType(User32.KEYBDINPUT.class);
        input.inputUnion.write();
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
    }
}
