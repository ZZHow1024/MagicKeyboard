package com.zzhow.magickeyboard.config;

import com.zzhow.magickeyboard.MagicKeyboardApplication;

import java.util.prefs.Preferences;

/**
 * 基于 java.util.prefs 的偏好存储实现
 * （Windows 注册表 / macOS plist / Linux dconf）
 *
 * @author ZZHow
 * create 2026/11/27
 */
final class JavaPreferenceStore implements PreferenceStore {

    private final Preferences prefs = Preferences.userNodeForPackage(MagicKeyboardApplication.class);

    @Override
    public String get(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    @Override
    public void put(String key, String value) {
        prefs.put(key, value);
    }
}
