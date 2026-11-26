package com.zzhow.magickeyboard.core.impl;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;
import java.util.Set;

/**
 * Linux 键盘布局查找表
 * <p>
 * 通过 XGetKeyboardMapping 读取当前键盘布局的 keysym 表，
 * 提供「字符 → (键码, 是否 Shift)」查找与空闲键码定位，
 * 使非美式布局下的兼容模式与任意 Unicode 字符注入成为可能。
 *
 * @author ZZHow
 * create 2026/11/26
 */
final class LinuxKeyLayout {

    /**
     * X11 的 NoSymbol
     */
    static final long NO_SYMBOL = 0L;

    private final int minKeycode;
    private final int maxKeycode;
    private final int keysymsPerKeycode;
    private final long[] keysyms; // 扁平数组：(键码 - minKeycode) * keysymsPerKeycode + 列号

    LinuxKeyLayout(int minKeycode, int maxKeycode, int keysymsPerKeycode, long[] keysyms) {
        this.minKeycode = minKeycode;
        this.maxKeycode = maxKeycode;
        this.keysymsPerKeycode = keysymsPerKeycode;
        this.keysyms = keysyms;
    }

    /**
     * 键码绑定关系
     *
     * @param keycode 键码
     * @param shift   是否需要 Shift
     */
    record KeyBinding(int keycode, boolean shift) {
    }

    /**
     * 从 X11 服务器读取当前键盘布局，失败时返回 null（调用方回退内置美式布局表）
     */
    static LinuxKeyLayout load(Pointer display, LinuxKeyboard.Xlib xlib) {
        try {
            IntByReference minKeycode = new IntByReference();
            IntByReference maxKeycode = new IntByReference();
            xlib.XDisplayKeycodes(display, minKeycode, maxKeycode);

            int min = minKeycode.getValue();
            int max = maxKeycode.getValue();
            int count = max - min + 1;
            if (min <= 0 || count <= 0 || count > 256) {
                return null;
            }

            IntByReference perKeycode = new IntByReference();
            Pointer mapping = xlib.XGetKeyboardMapping(display, min, count, perKeycode);
            if (mapping == null) {
                return null;
            }
            try {
                int per = perKeycode.getValue();
                if (per <= 0) {
                    return null;
                }
                long[] flat = mapping.getLongArray(0, count * per);
                return new LinuxKeyLayout(min, max, per, flat);
            } finally {
                xlib.XFree(mapping);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 在布局中查找 keysym（仅搜索未按下列与按下列，忽略 Mode_switch 列）
     *
     * @param keysym 目标 keysym（NoSymbol 直接返回 null）
     * @return 键码与是否 Shift，布局中不存在时返回 null
     */
    KeyBinding find(long keysym) {
        if (keysym == NO_SYMBOL) {
            return null;
        }
        for (int keycode = minKeycode; keycode <= maxKeycode; keycode++) {
            int base = (keycode - minKeycode) * keysymsPerKeycode;
            if (keysyms[base] == keysym) {
                return new KeyBinding(keycode, false);
            }
            if (keysymsPerKeycode > 1 && keysyms[base + 1] == keysym) {
                return new KeyBinding(keycode, true);
            }
        }
        return null;
    }

    /**
     * 查找所有列均为 NoSymbol 的空闲键码（排除已被动态占用的键码）
     *
     * @param exclude 需排除的键码集合
     * @return 空闲键码，不存在时返回 0
     */
    int findFreeKeycode(Set<Integer> exclude) {
        for (int keycode = minKeycode; keycode <= maxKeycode; keycode++) {
            if (exclude.contains(keycode)) {
                continue;
            }
            if (isFreeRow(keycode)) {
                return keycode;
            }
        }
        return 0;
    }

    private boolean isFreeRow(int keycode) {
        int base = (keycode - minKeycode) * keysymsPerKeycode;
        for (int column = 0; column < keysymsPerKeycode; column++) {
            if (keysyms[base + column] != NO_SYMBOL) {
                return false;
            }
        }
        return true;
    }

    /**
     * 返回键码当前 keysym 行的副本（用于会话结束后恢复）
     */
    long[] row(int keycode) {
        int base = (keycode - minKeycode) * keysymsPerKeycode;
        return Arrays.copyOfRange(keysyms, base, base + keysymsPerKeycode);
    }

    int keysymsPerKeycode() {
        return keysymsPerKeycode;
    }

    int maxKeycode() {
        return maxKeycode;
    }
}
