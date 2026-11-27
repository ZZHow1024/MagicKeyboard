package com.zzhow.magickeyboard.config;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.util.OverlayCountdown;

import java.util.Objects;

/**
 * 配置持久化管理器
 * <p>
 * 以字符串形式在 {@link PreferenceStore} 中读写各配置项，
 * 损坏或非法的存储值一律回退默认值，保证应用可正常启动。
 *
 * @author ZZHow
 * create 2026/11/27
 */
public final class PreferencesManager {

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_MODE = "mode";
    private static final String KEY_INTERVAL = "interval";
    private static final String KEY_POSITION = "position";
    private static final String KEY_IGNORE_LEADING_WHITESPACE = "ignoreLeadingWhitespace";
    private static final String KEY_WINDOW_BOUNDS = "windowBounds";

    private static final long DEFAULT_INTERVAL = 35L;
    private static final long INTERVAL_MAX = 1000L;
    private static final double MIN_WINDOW_SIZE = 1.0;

    private static PreferenceStore store = new JavaPreferenceStore();

    private PreferencesManager() {
    }

    /**
     * 注入偏好存储（仅供单元测试使用）
     */
    static void setStore(PreferenceStore replacement) {
        store = Objects.requireNonNull(replacement);
    }

    /**
     * 语言代码（zh_HANS / zh_HANT / en_US），未保存过时返回空串（由系统语言兜底）
     */
    public static String loadLanguage() {
        return store.get(KEY_LANGUAGE, "");
    }

    public static void saveLanguage(String languageCode) {
        store.put(KEY_LANGUAGE, languageCode);
    }

    public static ControlCenter.Mode loadMode() {
        try {
            return ControlCenter.Mode.valueOf(store.get(KEY_MODE, ""));
        } catch (IllegalArgumentException e) {
            return ControlCenter.Mode.COMPATIBLE_MODE;
        }
    }

    public static void saveMode(ControlCenter.Mode mode) {
        store.put(KEY_MODE, mode.name());
    }

    /**
     * 键入间隔（ms），非法或超界时回退默认值
     */
    public static long loadInterval() {
        try {
            long interval = Long.parseLong(store.get(KEY_INTERVAL, "").trim());
            if (interval < 0 || interval > INTERVAL_MAX) {
                return DEFAULT_INTERVAL;
            }
            return interval;
        } catch (NumberFormatException e) {
            return DEFAULT_INTERVAL;
        }
    }

    public static void saveInterval(long interval) {
        store.put(KEY_INTERVAL, Long.toString(interval));
    }

    public static OverlayCountdown.Corner loadPosition() {
        try {
            return OverlayCountdown.Corner.valueOf(store.get(KEY_POSITION, ""));
        } catch (IllegalArgumentException e) {
            return OverlayCountdown.Corner.BOTTOM_RIGHT;
        }
    }

    public static void savePosition(OverlayCountdown.Corner corner) {
        store.put(KEY_POSITION, corner.name());
    }

    public static boolean loadIgnoreLeadingWhitespace() {
        return Boolean.parseBoolean(store.get(KEY_IGNORE_LEADING_WHITESPACE, "false"));
    }

    public static void saveIgnoreLeadingWhitespace(boolean ignoreLeadingWhitespace) {
        store.put(KEY_IGNORE_LEADING_WHITESPACE, Boolean.toString(ignoreLeadingWhitespace));
    }

    /**
     * 保存主窗口尺寸与位置（x,y,w,h）
     */
    public static void saveWindowBounds(double x, double y, double width, double height) {
        store.put(KEY_WINDOW_BOUNDS, x + "," + y + "," + width + "," + height);
    }

    /**
     * 读取主窗口尺寸与位置
     *
     * @return [x, y, width, height]，未保存过或数据非法时返回 null
     */
    public static double[] loadWindowBounds() {
        try {
            String[] parts = store.get(KEY_WINDOW_BOUNDS, "").split(",");
            if (parts.length != 4) {
                return null;
            }
            double[] bounds = new double[4];
            for (int i = 0; i < 4; i++) {
                bounds[i] = Double.parseDouble(parts[i].trim());
            }
            if (!Double.isFinite(bounds[0]) || !Double.isFinite(bounds[1])
                    || bounds[2] < MIN_WINDOW_SIZE || bounds[3] < MIN_WINDOW_SIZE) {
                return null;
            }
            return bounds;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
