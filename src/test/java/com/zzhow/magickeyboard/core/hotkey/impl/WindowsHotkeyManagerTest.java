package com.zzhow.magickeyboard.core.hotkey.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;
import com.zzhow.magickeyboard.core.hotkey.Hotkey;
import com.zzhow.magickeyboard.core.hotkey.HotkeyModifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Windows 全局热键管理器集成测试（仅在 Windows 上运行）
 * <p>
 * 通过 SendInput 合成按键触发热键，与应用自身的模拟键入通道一致。
 *
 * @author ZZHow
 * create 2026/11/27
 */
class WindowsHotkeyManagerTest {

    private interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        int SendInput(int nInputs, WinUser.INPUT[] pInputs, int cbSize);
    }

    /**
     * 以虚拟键码发送一次按下/释放事件
     */
    private static void sendVirtualKey(int vk, boolean keyUp) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input = new WinUser.INPUT.INPUT_UNION();
        input.input.ki = new WinUser.KEYBDINPUT();
        input.input.ki.wVk = new WinDef.WORD(vk);
        input.input.ki.dwFlags = new WinDef.DWORD(keyUp ? WinUser.KEYBDINPUT.KEYEVENTF_KEYUP : 0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = null;
        input.input.setType(WinUser.KEYBDINPUT.class);
        input.write();
        User32.INSTANCE.SendInput(1, new WinUser.INPUT[]{input}, input.size());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void registersAndFiresGlobalHotkeyOnWindows() throws Exception {
        // 避免与默认热键（Ctrl+Alt+M/S）冲突，测试使用 Ctrl+Alt+T
        WindowsHotkeyManager manager = new WindowsHotkeyManager();
        CountDownLatch fired = new CountDownLatch(1);
        try {
            assumeTrue(manager.register(
                            new Hotkey(99, Set.of(HotkeyModifier.CONTROL, HotkeyModifier.ALT), 'T'),
                            fired::countDown),
                    "hotkey registration failed (possibly occupied by another application)");

            sendVirtualKey(0x11, false); // VK_CONTROL down
            sendVirtualKey(0x12, false); // VK_MENU(Alt) down
            sendVirtualKey('T', false);  // 'T' down
            sendVirtualKey('T', true);   // 'T' up
            sendVirtualKey(0x12, true);  // Alt up
            sendVirtualKey(0x11, true);  // Ctrl up

            assertTrue(fired.await(5, TimeUnit.SECONDS), "hotkey callback was not fired");
        } finally {
            manager.shutdown();
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void registerAfterShutdownFails() {
        WindowsHotkeyManager manager = new WindowsHotkeyManager();
        manager.shutdown();
        assertFalse(manager.register(
                new Hotkey(98, Set.of(HotkeyModifier.CONTROL), 'K'),
                () -> {
                }));
    }
}
