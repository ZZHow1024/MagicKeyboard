package com.zzhow.magickeyboard.core.hotkey.impl;

import com.zzhow.magickeyboard.core.hotkey.Hotkey;
import com.zzhow.magickeyboard.core.hotkey.IHotkeyManager;

import java.util.logging.Logger;

/**
 * 全局热键管理器-未支持平台实现
 * <p>
 * macOS（Carbon RegisterEventHotKey）与 Linux（XGrabKey / Wayland portal）
 * 将在后续版本提供，当前注册请求直接返回失败并记录一次日志。
 *
 * @author ZZHow
 * create 2026/11/27
 */
public class UnsupportedHotkeyManager implements IHotkeyManager {

    private static final Logger LOGGER = Logger.getLogger(UnsupportedHotkeyManager.class.getName());

    private volatile boolean warned = false;

    @Override
    public boolean register(Hotkey hotkey, Runnable callback) {
        if (!warned) {
            warned = true;
            LOGGER.info("Global hotkeys are not supported on this platform yet.");
        }
        return false;
    }

    @Override
    public void shutdown() {
        // 无需处理
    }
}
