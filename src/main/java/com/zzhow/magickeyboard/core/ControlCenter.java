package com.zzhow.magickeyboard.core;

import com.zzhow.magickeyboard.util.OverlayCountdown;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 控制中心类
 *
 * @author ZZHow
 * create 2025/10/14
 * update 2025/11/3
 */
public class ControlCenter {
    public static OverlayCountdown.Corner floatingWindowPosition = OverlayCountdown.Corner.BOTTOM_RIGHT; // 悬浮窗位置
    public static long timeInterval = 35L; // 键入间隔时间(ms)
    public static boolean isCountdown = false; // 是否开始倒计时
    public static boolean isStartInput = false; // 是否开始键入
    public static boolean isPaused = false; // 是否暂停

    public static Runnable onPaused;
    public static Runnable onResume;
    public static Runnable onResetStatus;

    public static Mode mode = Mode.COMPATIBLE_MODE; // 键入模式
    public static boolean isIgnoreLeadingWhitespace = false; // 是否忽略每行行首的空字符

    // 多语言
    public static ResourceBundle bundle = ResourceBundle.getBundle("MessagesBundle", Locale.of("zh", "HANS"));
    private static String language = "zh_HANS";

    public enum Mode {
        COMPATIBLE_MODE, // 兼容模式
        RAPID_MODE       // 极速模式
    }

    public static void stop() {
        if (ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            // 正在倒计时，还未开始键入
            OverlayCountdown.stop();
            ControlCenter.onResetStatus.run();
        } else if (ControlCenter.isCountdown) {
            // 倒计时结束，开始键入
            KeyboardInput.stop();
        }
    }

    public static void resumeOrPause() {
        if (ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            //正在倒计时，还未开始键入
            if (isPaused) {
                // 已暂停
                OverlayCountdown.resume();
                ControlCenter.isPaused = false;
                onResume.run();
            } else {
                // 未暂停
                OverlayCountdown.pause();
                ControlCenter.isPaused = true;
                onPaused.run();
            }
        } else if (ControlCenter.isStartInput) {
            // 倒计时结束，开始键入
            if (isPaused) {
                // 已暂停
                OverlayCountdown.stop();
                OverlayCountdown.show(3, 0.5, Color.BLACK, 80, ControlCenter.floatingWindowPosition, KeyboardInput::resume);
                ControlCenter.isPaused = false;
                onResume.run();
            } else {
                // 未暂停
                OverlayCountdown.pause();
                KeyboardInput.pause();
                ControlCenter.isPaused = true;
                onPaused.run();
            }
        }
    }

    public static String getLanguage() {
        return language;
    }

    public static void setLanguage(String language) {
        ControlCenter.language = language;
        bundle = ResourceBundle.getBundle("MessagesBundle", Locale.of(language.split("_")[0], language.split("_")[1]));
    }
}
