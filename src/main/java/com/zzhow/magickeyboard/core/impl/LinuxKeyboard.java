package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.zzhow.magickeyboard.core.ControlCenter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 模拟键盘输入-Linux 实现类
 * <p>
 * 兼容模式：按实际键盘布局将字符解析为 (键码, Shift) 后发送；
 * 极速模式：按 keysym 查键码后直接发送。
 * 布局中不存在的字符（如中文、Emoji）通过 XChangeKeyboardMapping
 * 临时绑定空闲键码注入，会话结束后恢复原布局。
 * 布局读取失败时回退内置美式布局表，保持原有行为。
 * 仅支持 X11 显示服务器。
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2026/11/26
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

    // Xlib 接口（用于布局读取与 Unicode 支持）
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

        void XSync(Pointer display, boolean discard);

        int XFree(Pointer data);

        long XKeysymToKeycode(Pointer display, long keysym);

        int XDisplayKeycodes(Pointer display, IntByReference minKeycodesReturn, IntByReference maxKeycodesReturn);

        Pointer XGetKeyboardMapping(Pointer display, int firstKeycode, int keycodeCount, IntByReference keysymsPerKeycodeReturn);

        int XChangeKeyboardMapping(Pointer display, int firstKeycode, int keysymsPerKeycode, long[] keysyms, int numKeysyms);
    }

    // 键入期间持有的 X11 显示连接与布局状态（同一时刻仅一个键入会话）
    private Pointer display;
    private LinuxKeyLayout layout;
    private final Map<Long, Integer> dynamicBindings = new HashMap<>(); // keysym → 动态绑定的键码
    private final Map<Integer, long[]> originalRows = new HashMap<>();  // 被动态改写的键码 → 原始 keysym 行

    @Override
    public void sendText(String text, ControlCenter.Mode mode) {
        if (X11.INSTANCE == null || Xlib.INSTANCE == null) {
            LOGGER.severe(x11UnavailableMessage());
            return;
        }

        boolean wayland = isWaylandSession();
        if (wayland) {
            LOGGER.info(waylandSessionHintMessage());
        }

        Pointer display = Xlib.INSTANCE.XOpenDisplay(null);
        if (display == null) {
            LOGGER.severe(wayland
                    ? x11DisplayUnavailableUnderWaylandMessage()
                    : x11DisplayUnavailableMessage());
            return;
        }
        this.display = display;
        this.layout = LinuxKeyLayout.load(display, Xlib.INSTANCE);
        this.dynamicBindings.clear();
        this.originalRows.clear();

        try {
            super.sendText(text, mode);
        } finally {
            restoreKeyboardMapping();
            Xlib.INSTANCE.XFlush(display);
            Xlib.INSTANCE.XCloseDisplay(display);
            this.display = null;
            this.layout = null;
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

    /**
     * 检测 Wayland 会话（WAYLAND_DISPLAY 或 XDG_SESSION_TYPE）
     */
    static boolean isWaylandSession() {
        if (System.getenv("WAYLAND_DISPLAY") != null) {
            return true;
        }
        return "wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"));
    }

    private static String waylandSessionHintMessage() {
        try {
            return ControlCenter.bundle.getString("error.waylandSessionHint");
        } catch (Exception e) {
            return "Wayland session detected: input goes through XWayland in this version.";
        }
    }

    private static String x11DisplayUnavailableUnderWaylandMessage() {
        try {
            return ControlCenter.bundle.getString("error.x11DisplayUnavailableWayland");
        } catch (Exception e) {
            return "Unable to open the X11 display. Under Wayland, make sure XWayland is installed and running.";
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
     * 兼容模式：发送字符（优先按实际布局解析，其次动态键码绑定，最后回退内置美式布局表）
     */
    private void sendChar(int codePoint) {
        if (!isTypeable(codePoint)) {
            return;
        }

        if (layout != null) {
            LinuxKeyLayout.KeyBinding binding = layout.find(codePointToKeysym(codePoint));
            if (binding != null) {
                pressKey(binding.keycode(), binding.shift());
                return;
            }

            int keycode = resolveByServerOrBinding(codePointToKeysym(codePoint));
            if (keycode != 0) {
                // 布局中不存在的字符（中文、Emoji 等）经动态键码绑定注入，无需 Shift
                pressKey(keycode, false);
                return;
            }
            // 布局与动态绑定均不可用：回退内置美式布局表路径
        }

        legacySendChar(codePoint);
    }

    /**
     * 极速模式：使用 Unicode keysym 方式发送字符
     */
    private void sendCharUnicode(int codePoint) {
        if (!isTypeable(codePoint)) {
            return;
        }

        long keysym = codePointToKeysym(codePoint);
        int keycode;
        if (layout != null) {
            LinuxKeyLayout.KeyBinding binding = layout.find(keysym);
            keycode = binding != null ? binding.keycode() : resolveByServerOrBinding(keysym);
        } else {
            long kc = Xlib.INSTANCE.XKeysymToKeycode(display, keysym);
            keycode = kc != 0 ? (int) kc : 0;
        }

        if (keycode == 0) {
            // 静默跳过无法找到键码的字符
            return;
        }

        // 发送按键按下和释放事件（不使用 Shift）
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, false, 0);
        Xlib.INSTANCE.XFlush(display);
    }

    /**
     * 判断码点是否可直接注入（排除控制字符与孤立代理项；补充字符以完整码点传入）
     */
    private static boolean isTypeable(int codePoint) {
        return codePoint >= 0x20 && (codePoint < 0xD800 || codePoint > 0xDFFF);
    }

    /**
     * 码点转目标 keysym：ASCII 与 Latin-1 补充直接对应，其余使用 Unicode 区（0x01000000 + 码点）
     */
    private static long codePointToKeysym(int codePoint) {
        if (codePoint >= 0x20 && codePoint <= 0xFF) {
            return codePoint;
        }
        return 0x01000000L + codePoint;
    }

    /**
     * 服务器端查找 keysym 的键码，查不到时动态绑定
     */
    private int resolveByServerOrBinding(long keysym) {
        long keycode = Xlib.INSTANCE.XKeysymToKeycode(display, keysym);
        if (keycode != 0) {
            return (int) keycode;
        }
        return bindKeysym(keysym);
    }

    /**
     * 通过 XChangeKeyboardMapping 将 keysym 临时绑定到空闲键码（会话内缓存，会话结束时恢复）
     *
     * @return 绑定的键码，失败时返回 0
     */
    private int bindKeysym(long keysym) {
        if (layout == null) {
            return 0;
        }

        Integer cached = dynamicBindings.get(keysym);
        if (cached != null) {
            return cached;
        }

        Set<Integer> occupied = new HashSet<>(dynamicBindings.values());
        int target = layout.findFreeKeycode(occupied);
        if (target == 0) {
            // 无空闲键码：临时占用最大键码（会话结束后恢复）
            target = layout.maxKeycode();
            if (occupied.contains(target)) {
                return 0;
            }
        }

        int per = layout.keysymsPerKeycode();
        long[] newRow = new long[per];
        newRow[0] = keysym;
        long[] originalRow = layout.row(target);

        Xlib.INSTANCE.XChangeKeyboardMapping(display, target, per, newRow, 1);
        Xlib.INSTANCE.XSync(display, false);

        // 以服务器端查找结果验证绑定是否生效
        long keycode = Xlib.INSTANCE.XKeysymToKeycode(display, keysym);
        if (keycode != target) {
            return 0;
        }

        dynamicBindings.put(keysym, target);
        originalRows.put(target, originalRow);
        return target;
    }

    /**
     * 会话结束时恢复所有被动态改写的键码行
     */
    private void restoreKeyboardMapping() {
        if (originalRows.isEmpty() || display == null) {
            originalRows.clear();
            dynamicBindings.clear();
            return;
        }
        int per = layout != null ? layout.keysymsPerKeycode() : 0;
        if (per <= 0) {
            originalRows.clear();
            dynamicBindings.clear();
            return;
        }
        for (Map.Entry<Integer, long[]> entry : originalRows.entrySet()) {
            Xlib.INSTANCE.XChangeKeyboardMapping(display, entry.getKey(), per, entry.getValue(), 1);
        }
        Xlib.INSTANCE.XSync(display, false);
        originalRows.clear();
        dynamicBindings.clear();
    }

    /**
     * 按下并释放一个键码（可选 Shift），保持与原实现一致的节奏
     */
    private void pressKey(int keycode, boolean shift) {
        if (shift) {
            // 按下 Shift
            X11.INSTANCE.XTestFakeKeyEvent(display, KEYCODE_LEFT_SHIFT, true, 0);
            sleepInterval();
        }

        // 发送按键按下和释放事件
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, true, 0);
        X11.INSTANCE.XTestFakeKeyEvent(display, keycode, false, 0);

        if (shift) {
            sleepInterval();
            // 释放 Shift
            X11.INSTANCE.XTestFakeKeyEvent(display, KEYCODE_LEFT_SHIFT, false, 0);
        }

        Xlib.INSTANCE.XFlush(display);
    }

    /**
     * 旧路径：内置美式布局表 + XKeysymToKeycode（布局不可用或动态绑定失败时的回退）
     */
    private void legacySendChar(int codePoint) {
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

        pressKey((int) keycode, needsShift);
    }

    /**
     * 判断字符是否需要按 Shift 键（美式键盘布局，仅作回退路径）
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
     * 将字符转换为 X11 keysym（美式布局推导，仅作回退路径）
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
