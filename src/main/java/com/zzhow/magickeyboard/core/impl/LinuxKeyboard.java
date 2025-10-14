package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.IKeyboard;

/**
 * 模拟键盘输入-Linux 实现类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/10/14
 */
public class LinuxKeyboard implements IKeyboard {

    private volatile boolean isStopped = false;
    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();

    // X11 接口定义
    public interface X11 extends Library {
        X11 INSTANCE = loadX11();

        static X11 loadX11() {
            try {
                return Native.load("Xtst", X11.class);
            } catch (UnsatisfiedLinkError e) {
                return null;
            }
        }

        Pointer XOpenDisplay(String display);

        void XCloseDisplay(Pointer display);

        void XTestFakeKeyEvent(Pointer display, int keycode, boolean is_press, long delay);

        void XFlush(Pointer display);
    }

    // Xlib 接口（用于 Unicode 支持）
    public interface Xlib extends Library {
        Xlib INSTANCE = loadXlib();

        static Xlib loadXlib() {
            try {
                return Native.load("X11", Xlib.class);
            } catch (UnsatisfiedLinkError e) {
                return null;
            }
        }

        Pointer XOpenDisplay(String display);

        void XCloseDisplay(Pointer display);

        void XFlush(Pointer display);

        long XKeysymToKeycode(Pointer display, long keysym);
    }

    @Override
    public void sendText(String text) {
        // 重置状态
        isStopped = false;
        isPaused = false;

        if (X11.INSTANCE == null || Xlib.INSTANCE == null) {
            System.err.println("无法加载 X11 库，请确保系统安装了 X11");
            return;
        }

        Pointer display = Xlib.INSTANCE.XOpenDisplay(null);
        if (display == null) {
            System.err.println("无法打开 X11 显示");
            return;
        }

        try {
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
                    sendSpecialKey(36); // Enter keycode
                    i++; // 跳过下一个字符（如果有的话）
                    continue;
                } else if (c == '\t') { // Tab键
                    sendSpecialKey(23); // Tab keycode
                    i++; // 跳过下一个字符（如果有的话）
                    continue;
                }

                // 处理代理对（Surrogate Pairs）- 用于支持某些特殊字符和 Emoji
                if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                    char low = text.charAt(i + 1);
                    if (Character.isLowSurrogate(low)) {
                        int codePoint = Character.toCodePoint(c, low);
                        sendChar(display, codePoint);
                        i++; // 跳过下一个字符
                        continue;
                    }
                }

                sendChar(display, c);

                try {
                    Thread.sleep(ControlCenter.timeInterval); // 添加延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            isStopped = true;
        } finally {
            Xlib.INSTANCE.XFlush(display);
            Xlib.INSTANCE.XCloseDisplay(display);
        }
    }

    private void sendChar(Pointer display, int codePoint) {
        // 将字符/码点转换为 X11 keysym
        long keysym = charToKeysym(codePoint);

        if (keysym == 0) {
            System.err.println("无法映射字符: U+" + Integer.toHexString(codePoint).toUpperCase());
            return;
        }

        // 将 keysym 转换为 keycode
        long keycode = Xlib.INSTANCE.XKeysymToKeycode(display, keysym);

        if (keycode == 0) {
            System.err.println("无法找到 keycode: " + (char) codePoint);
            return;
        }

        // 发送按键按下和释放事件
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, false, 0);
    }

    /**
     * 将字符/码点转换为 X11 keysym
     * <p>
     * X11 keysym 映射规则：
     * - ASCII (0x20-0x7E): 直接映射
     * - Latin-1 (0xA0-0xFF): 直接映射
     * - Unicode (0x100-0x10FFFF): 0x01000000 + 码点
     */
    private long charToKeysym(int codePoint) {
        // ASCII 可打印字符 (空格到波浪号)
        if (codePoint >= 0x20 && codePoint <= 0x7E) {
            return codePoint;
        }

        // Latin-1 补充字符
        if (codePoint >= 0x00A0 && codePoint <= 0x00FF) {
            return codePoint;
        }

        // Unicode 字符使用 0x01000000 + 码点
        // 这是 X11 标准的 Unicode keysym 表示方法
        if (codePoint >= 0x0100 && codePoint <= 0x10FFFF) {
            return 0x01000000L + codePoint;
        }

        // 无法映射的字符
        return 0;
    }

    /**
     * 发送特殊按键（如 Enter、Tab 等）
     * <p>
     * 常用 keycode：
     * - Enter: 36
     * - Tab: 23
     * - Escape: 9
     * - Backspace: 22
     * - Space: 65
     */
    public void sendSpecialKey(int keycode) {
        if (X11.INSTANCE == null) {
            System.err.println("X11 库未加载");
            return;
        }

        Pointer display = X11.INSTANCE.XOpenDisplay(null);
        if (display == null) {
            System.err.println("无法打开 X11 显示");
            return;
        }

        try {
            X11.INSTANCE.XTestFakeKeyEvent(display, keycode, true, 0);
            X11.INSTANCE.XTestFakeKeyEvent(display, keycode, false, 0);
            X11.INSTANCE.XFlush(display);
        } finally {
            X11.INSTANCE.XCloseDisplay(display);
        }
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