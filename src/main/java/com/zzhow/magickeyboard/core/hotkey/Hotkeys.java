package com.zzhow.magickeyboard.core.hotkey;

import com.zzhow.magickeyboard.core.hotkey.impl.UnsupportedHotkeyManager;
import com.zzhow.magickeyboard.core.hotkey.impl.WindowsHotkeyManager;

import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 全局热键入口类：按平台选择实现并注册默认热键
 * <p>
 * 默认热键：Ctrl+Alt+M 开始/暂停/继续，Ctrl+Alt+S 停止。
 *
 * @author ZZHow
 * create 2026/11/27
 */
public final class Hotkeys {

    private static final Logger LOGGER = Logger.getLogger(Hotkeys.class.getName());

    public static final int HOTKEY_ID_TOGGLE = 1;
    public static final int HOTKEY_ID_STOP = 2;

    private Hotkeys() {
    }

    /**
     * 按当前平台创建热键管理器
     */
    public static IHotkeyManager create() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new WindowsHotkeyManager();
        }
        return new UnsupportedHotkeyManager();
    }

    /**
     * 注册默认热键（Ctrl+Alt+M / Ctrl+Alt+S），结果记录日志
     *
     * @param manager      热键管理器
     * @param toggleAction 开始/暂停/继续 动作
     * @param stopAction   停止动作
     */
    public static void registerDefaults(IHotkeyManager manager, Runnable toggleAction, Runnable stopAction) {
        boolean toggleOk = manager.register(
                new Hotkey(HOTKEY_ID_TOGGLE, Set.of(HotkeyModifier.CONTROL, HotkeyModifier.ALT), 'M'),
                toggleAction);
        boolean stopOk = manager.register(
                new Hotkey(HOTKEY_ID_STOP, Set.of(HotkeyModifier.CONTROL, HotkeyModifier.ALT), 'S'),
                stopAction);

        if (toggleOk && stopOk) {
            LOGGER.info("Global hotkeys registered: Ctrl+Alt+M (toggle), Ctrl+Alt+S (stop)");
        }
    }
}
