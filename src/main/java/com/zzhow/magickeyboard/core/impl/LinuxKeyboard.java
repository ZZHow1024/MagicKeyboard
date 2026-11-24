package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.zzhow.magickeyboard.core.ControlCenter;

import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * 模拟键盘输入-Linux 实现类
 * <p>
 * 兼容模式：keysym 转键码 + Shift 组合键；极速模式：Unicode keysym。
 * 仅支持 X11 显示服务器。
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2026/11/23
 */
public class LinuxKeyboard extends AbstractKeyboard {

    private static final Logger LOGGER = Logger.getLogger(LinuxKeyboard.class.getName());

    private static final int KEYCODE_LEFT_SHIFT = 50; // Left Shift keycode
    private static final int KEYCODE_ENTER = 36;      // Enter keycode
    private static final int KEYCODE_TAB = 23;        // Tab keycode

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

        void XTestFakeKeyEvent(Pointer display, int keycode, boolean is_press, long delay);
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

    // 键入期间持有的 X11 显示连接
    private Pointer display;

    @Override
    public void sendText(String text, ControlCenter.Mode mode) {
        if (X11.INSTANCE == null || Xlib.INSTANCE == null) {
            LOGGER.severe(x11UnavailableMessage());
            return;
        }

        Pointer display = Xlib.INSTANCE.XOpenDisplay(null);
        if (display == null) {
            LOGGER.severe(x11DisplayUnavailableMessage());
            return;
        }
        this.display = display;

        try {
            super.sendText(text, mode);
        } finally {
            Xlib.INSTANCE.XFlush(display);
            Xlib.INSTANCE.XCloseDisplay(display);
            this.display = null;
        }
    }

    private static String x11UnavailableMessage() {
        try {
            return ControlCenter.bundle.getString("error.x11Unavailable");
        } catch (Exception e) {
            return "Unable to load the X11 libraries (X11/libXtst).";
        }
    }

    private static String x11DisplayUnavailableMessage() {
        try {
            return ControlCenter.bundle.getString("error.x11DisplayUnavailable");
        } catch (Exception e) {
            return "Unable to open the X11 display.";
        }
    }

    @Override
    protected void sendNewLine() {
        sendSpecialKeyWithDisplay(KEYCODE_ENTER);
    }

    @Override
    protected void sendTab() {
        sendSpecialKeyWithDisplay(KEYCODE_TAB);
    }

    /**
     * 根据模式发送字符
     */
    private void sendCharByMode(int codePoint, ControlCenter.Mode mode) {
        if (mode == ControlCenter.Mode.RAPID_MODE) {
            // 极速模式：使用 Unicode 方式
            sendCharUnicode(codePoint);
        } else {
            // 兼容模式：使用虚拟键码方式
            sendChar(codePoint);
        }
    }

    @Override
    protected void sendCodePoint(int codePoint, ControlCenter.Mode mode) {
        sendCharByMode(codePoint, mode);
    }

    /**
     * 兼容模式：发送字符（使用虚拟键码 + Shift）
     */
    private void sendChar(int codePoint) {
        // 检查是否需要 Shift 键
        boolean needsShift = needsShiftKey(codePoint);

        // 将字符转换为基础按键的 keysym (对于大写字母,转换为小写)
        long keysym = charToKeysym(codePoint, needsShift);

        if (keysym == 0) {
            // 静默跳过无法映射的字符(如中文)
            return;
        }

        // 将 keysym 转换为 keycode
        long keycode = Xlib.INSTANCE.XKeysymToKeycode(display, keysym);

        if (keycode == 0) {
            // 静默跳过无法找到keycode的字符
            return;
        }

        // 如果需要 Shift，先按下 Shift 键
        if (needsShift) {
            X11.INSTANCE.XTestFakeKeyEvent(display, KEYCODE_LEFT_SHIFT, true, 0);
            sleepInterval();
        }

        // 发送按键按下和释放事件
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, false, 0);

        // 如果按下了 Shift，释放 Shift 键
        if (needsShift) {
            sleepInterval();
            X11.INSTANCE.XTestFakeKeyEvent(display, KEYCODE_LEFT_SHIFT, false, 0); // Release Shift
        }

        Xlib.INSTANCE.XFlush(display);
    }

    /**
     * 极速模式：使用 Unicode 方式发送字符
     */
    private void sendCharUnicode(int codePoint) {
        // 直接使用 Unicode keysym (0x01000000 + 码点)
        long keysym;
        if (codePoint <= 0x7E) {
            // ASCII 字符直接使用
            keysym = codePoint;
        } else if (codePoint >= 0x00A0 && codePoint <= 0x00FF) {
            // Latin-1 补充字符
            keysym = codePoint;
        } else {
            // Unicode 字符使用 0x01000000 + 码点
            keysym = 0x01000000L + codePoint;
        }

        // 将 keysym 转换为 keycode
        long keycode = Xlib.INSTANCE.XKeysymToKeycode(display, keysym);

        if (keycode == 0) {
            // 静默跳过无法找到keycode的字符
            return;
        }

        // 发送按键按下和释放事件（不使用 Shift）
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, (int) keycode, false, 0);

        Xlib.INSTANCE.XFlush(display);
    }

    /**
     * 判断字符是否需要按 Shift 键（美式键盘布局）
     */
    static boolean needsShiftKey(int codePoint) {
        char c = (char) codePoint;

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

        // 其他字符默认不需要 Shift（含空格）
        return false;
    }

    /**
     * 将字符转换为 X11 keysym
     * 注意: 对于需要Shift的字符(如大写字母),返回的是基础按键的keysym(小写字母)
     *
     * @param codePoint  Unicode 码点
     * @param needsShift 是否需要Shift键
     * @return X11 keysym，无法映射时返回 0
     */
    static long charToKeysym(int codePoint, boolean needsShift) {
        // 大写字母: 返回对应小写字母的keysym
        if (codePoint >= 'A' && codePoint <= 'Z') {
            return Character.toLowerCase((char) codePoint);
        }

        // 小写字母: 直接返回
        if (codePoint >= 'a' && codePoint <= 'z') {
            return codePoint;
        }

        // 数字键: 直接返回 (0-9对应的keysym就是ASCII码)
        if (codePoint >= '0' && codePoint <= '9') {
            return codePoint;
        }

        // 需要Shift的符号: 返回基础按键的keysym
        if (needsShift) {
            switch (codePoint) {
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
        }

        // 不需要Shift的符号: 直接返回
        if (codePoint >= 0x20 && codePoint <= 0x7E) {
            return codePoint;
        }

        // Latin-1 补充字符
        if (codePoint >= 0x00A0 && codePoint <= 0x00FF) {
            return codePoint;
        }

        // Unicode 字符使用 0x01000000 + 码点（含补充字符，原实现对高位截断导致无法输入）
        if (codePoint >= 0x0100 && codePoint <= 0x10FFFF) {
            return 0x01000000L + codePoint;
        }

        // 无法映射的字符,返回0表示跳过
        return 0;
    }

    /**
     * 使用已有display发送特殊按键(内部方法)
     */
    private void sendSpecialKeyWithDisplay(int keycode) {
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, false, 0);
        Xlib.INSTANCE.XFlush(display);
    }
}
