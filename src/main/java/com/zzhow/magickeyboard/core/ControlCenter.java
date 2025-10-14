package com.zzhow.magickeyboard.core;

import com.zzhow.magickeyboard.util.OverlayCountdown;
import javafx.scene.paint.Color;

/**
 * 控制中心类
 *
 * @author ZZHow
 * create 2025/10/14
 * update 2025/10/14
 */
public class ControlCenter {
    public static boolean isCountdown = false; // 是否开始倒计时
    public static boolean isStartInput = false; // 是否开始键入
    public static boolean isPaused = false; // 是否暂停
    public static Runnable onPaused;
    public static Runnable onResume;

    public static void stop() {
        if (ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            // 正在倒计时，还未开始键入
            OverlayCountdown.stop();
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
            } else {
                // 未暂停
                OverlayCountdown.pause();
                ControlCenter.isPaused = true;
            }
        } else if (ControlCenter.isStartInput) {
            // 倒计时结束，开始键入
            if (isPaused) {
                // 已暂停
                OverlayCountdown.stop();
                OverlayCountdown.show(3, 0.5, Color.BLACK, 80, OverlayCountdown.Corner.TOP_RIGHT, KeyboardInput::resume);
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
}
