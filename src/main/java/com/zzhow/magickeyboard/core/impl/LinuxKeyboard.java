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
 * update 2025/1/1
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
            System.err.println("无法加载 X11 库,请确保系统安装了 X11");
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
                checkPause();
                if (isStopped) {
                    break;
                }

                char c = text.charAt(i);

                // 处理特殊按键
                if (c == '\n') { // 回车键
                    sendSpecialKeyWithDisplay(display, 36); // Enter keycode
                    sleep();
                    continue;
                } else if (c == '\t') { // Tab键
                    sendSpecialKeyWithDisplay(display, 23); // Tab keycode
                    sleep();
                    continue;
                }

                // 处理代理对（Surrogate Pairs）- 用于支持某些特殊字符和 Emoji
                if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                    char low = text.charAt(i + 1);
                    if (Character.isLowSurrogate(low)) {
                        int codePoint = Character.toCodePoint(c, low);
                        // 尝试发送,如果无法发送则跳过
                        if (sendChar(display, codePoint)) {
                            sleep();
                        }
                        i++; // 跳过低位代理
                        continue;
                    }
                }

                // 发送普通字符,如果无法发送则跳过
                if (sendChar(display, c)) {
                    sleep();
                }
            }
            isStopped = true;
        } finally {
            Xlib.INSTANCE.XFlush(display);
            Xlib.INSTANCE.XCloseDisplay(display);
        }
    }

    /**
     * 检查暂停状态
     */
    private void checkPause() {
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
    }

    /**
     * 延迟
     */
    private void sleep() {
        try {
            Thread.sleep(ControlCenter.timeInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 发送字符,返回是否成功
     */
    private boolean sendChar(Pointer display, int codePoint) {
        char c = (char) codePoint;

        // 检查是否需要 Shift 键
        boolean needsShift = needsShiftKey(c);

        // 将字符转换为基础按键的 keysym (对于大写字母,转换为小写)
        long keysym = charToKeysym(c, needsShift);

        if (keysym == 0) {
            // 静默跳过无法映射的字符(如中文)
            return false;
        }

        // 将 keysym 转换为 keycode
        long keycode = Xlib.INSTANCE.XKeysymToKeycode(display, keysym);

        if (keycode == 0) {
            // 静默跳过无法找到keycode的字符
            return false;
        }

        // 如果需要 Shift，先按下 Shift 键
        if (needsShift) {
            X11.INSTANCE.XTestFakeKeyEvent(display, 50, true, 0); // Left Shift keycode = 50
            try {
                Thread.sleep(10); // 短暂延迟确保Shift被识别
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 发送按键按下和释放事件
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, false, 0);

        // 如果按下了 Shift，释放 Shift 键
        if (needsShift) {
            try {
                Thread.sleep(10); // 短暂延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            X11.INSTANCE.XTestFakeKeyEvent(display, 50, false, 0); // Release Shift
        }

        X11.INSTANCE.XFlush(display);
        return true;
    }

    /**
     * 判断字符是否需要按 Shift 键
     */
    private boolean needsShiftKey(char c) {
        // 大写字母需要 Shift
        if (c >= 'A' && c <= 'Z') {
            return true;
        }

        // 小写字母不需要 Shift
        if (c >= 'a' && c <= 'z') {
            return false;
        }

        // 数字不需要 Shift
        if (c >= '0' && c <= '9') {
            return false;
        }

        // 需要 Shift 的符号（美式键盘布局）
        String shiftSymbols = "!@#$%^&*()_+{}|:\"<>?~";
        if (shiftSymbols.indexOf(c) != -1) {
            return true;
        }

        // 不需要 Shift 的符号
        String noShiftSymbols = "`-=[]\\;',./";
        if (noShiftSymbols.indexOf(c) != -1) {
            return false;
        }

        // 空格不需要 Shift
        if (c == ' ') {
            return false;
        }

        // 其他字符默认不需要 Shift
        return false;
    }

    /**
     * 将字符转换为 X11 keysym
     * 注意: 对于需要Shift的字符(如大写字母),返回的是基础按键的keysym(小写字母)
     *
     * @param c          字符
     * @param needsShift 是否需要Shift键
     * @return X11 keysym
     */
    private long charToKeysym(char c, boolean needsShift) {
        // 大写字母: 返回对应小写字母的keysym
        if (c >= 'A' && c <= 'Z') {
            return Character.toLowerCase(c);
        }

        // 小写字母: 直接返回
        if (c >= 'a' && c <= 'z') {
            return c;
        }

        // 数字键: 直接返回 (0-9对应的keysym就是ASCII码)
        if (c >= '0' && c <= '9') {
            return c;
        }

        // 需要Shift的符号: 返回基础按键的keysym
        switch (c) {
            case '!':
                return '1';
            case '@':
                return '2';
            case '#':
                return '3';
            case '$':
                return '4';
            case '%':
                return '5';
            case '^':
                return '6';
            case '&':
                return '7';
            case '*':
                return '8';
            case '(':
                return '9';
            case ')':
                return '0';
            case '_':
                return '-';
            case '+':
                return '=';
            case '{':
                return '[';
            case '}':
                return ']';
            case '|':
                return '\\';
            case ':':
                return ';';
            case '"':
                return '\'';
            case '<':
                return ',';
            case '>':
                return '.';
            case '?':
                return '/';
            case '~':
                return '`';
        }

        // 不需要Shift的符号: 直接返回
        if ((c >= 0x20 && c <= 0x7E)) {
            return c;
        }

        // Latin-1 补充字符
        if (c >= 0x00A0 && c <= 0x00FF) {
            return c;
        }

        // Unicode 字符使用 0x01000000 + 码点
        if (c >= 0x0100 && c <= 0xFFFF) {
            return 0x01000000L + c;
        }

        // 无法映射的字符,返回0表示跳过
        return 0;
    }

    /**
     * 使用已有display发送特殊按键(内部方法)
     */
    private void sendSpecialKeyWithDisplay(Pointer display, int keycode) {
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, false, 0);
        X11.INSTANCE.XFlush(display);
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