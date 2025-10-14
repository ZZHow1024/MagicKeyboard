package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.KeyboardInput;
import com.zzhow.magickeyboard.util.OverlayCountdown;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

/**
 * 主窗口控制类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/10/14
 */
public class MainController {
    @FXML
    private TextArea textArea;
    @FXML
    private Button buttonStart;
    @FXML
    private Button buttonClear;

    @FXML
    private void initialize() {
        KeyboardInput.sendText("");
    }

    @FXML
    public void onButtonClearClicked() {
        if (!ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            // 既没开始倒计时，也没在键入
            textArea.clear();
        } else {
            // 倒计时结束，开始键入
            ControlCenter.stop();
            resetStatus();
        }
    }

    // 重置状态
    private void resetStatus() {
        this.buttonStart.setText("开始键入");
        this.buttonClear.setText("清空");
        ControlCenter.isCountdown = false;
        ControlCenter.isStartInput = false;
        ControlCenter.isPaused = false;
    }

    @FXML
    private void onButtonStartClicked() {
        if (!ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            // 既没开始倒计时，也没在键入
            ControlCenter.isCountdown = true;
            this.buttonStart.setText("暂停");
            this.buttonClear.setText("停止");
            OverlayCountdown.show(3, 0.5, Color.BLACK, 80, OverlayCountdown.Corner.TOP_RIGHT, () -> {
                ControlCenter.isStartInput = true;
                KeyboardInput.sendText(textArea.getText());
                Platform.runLater(this::resetStatus);
            });
        } else {
            // 倒计时结束，开始键入
            if (ControlCenter.isPaused) {
                // 已暂停
                ControlCenter.resumeOrPause();
                this.buttonStart.setText("暂停");
            } else {
                // 未暂停
                ControlCenter.resumeOrPause();
                this.buttonStart.setText("继续");
            }
        }
    }
}