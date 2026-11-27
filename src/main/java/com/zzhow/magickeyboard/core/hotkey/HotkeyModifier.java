package com.zzhow.magickeyboard.core.hotkey;

/**
 * 热键修饰键
 * <p>
 * 位掩码定义与 Windows 的 MOD_* 常量保持一致，作为跨平台的统一编码，
 * 各平台实现负责换算为本机修饰键。
 *
 * @author ZZHow
 * create 2026/11/27
 */
public enum HotkeyModifier {
    ALT(0x1),
    CONTROL(0x2),
    SHIFT(0x4);

    private final int mask;

    HotkeyModifier(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
    }

    /**
     * 组合修饰键集合的位掩码
     */
    public static int maskOf(Iterable<HotkeyModifier> modifiers) {
        int mask = 0;
        for (HotkeyModifier modifier : modifiers) {
            mask |= modifier.mask;
        }
        return mask;
    }
}
