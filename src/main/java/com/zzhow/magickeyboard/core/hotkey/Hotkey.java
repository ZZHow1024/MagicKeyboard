package com.zzhow.magickeyboard.core.hotkey;

import java.util.Set;

/**
 * 全局热键定义
 * <p>
 * {@code keyCode} 采用与 Windows 虚拟键码一致的跨平台编码
 * （字母 A-Z 对应 0x41-0x5A，数字 0-9 对应 0x30-0x39），
 * 各平台实现负责换算为本机键码。
 *
 * @param id        热键标识（同一 id 重复注册会替换旧的）
 * @param modifiers 修饰键集合
 * @param keyCode   主键码
 * @author ZZHow
 * create 2026/11/27
 */
public record Hotkey(int id, Set<HotkeyModifier> modifiers, int keyCode) {

    public Hotkey {
        modifiers = Set.copyOf(modifiers);
    }

    /**
     * 修饰键位掩码
     */
    public int modifierMask() {
        return HotkeyModifier.maskOf(modifiers);
    }
}
