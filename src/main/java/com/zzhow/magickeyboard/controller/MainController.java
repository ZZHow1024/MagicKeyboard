package com.zzhow.magickeyboard.controller;

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
 * @date 2025/10/13
 */
public class MainController {
    private boolean isCountdown; // 是否开始倒计时
    private boolean isStartInput; // 是否开始键入
    private boolean isPaused; // 是否暂停
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
        if (!this.isCountdown && !this.isStartInput) {
            // 既没开始倒计时，也没在键入
            textArea.clear();
        } else if (this.isCountdown && !this.isStartInput) {
            // 正在倒计时，还未开始键入
            OverlayCountdown.stop();
            this.buttonStart.setText("开始键入");
            this.buttonClear.setText("清空");
            this.isCountdown = false;
            this.isStartInput = false;
            this.isPaused = false;
        }
    }

    @FXML
    private void onButtonStartClicked() {
        if (!this.isCountdown && !this.isStartInput) {
            // 既没开始倒计时，也没在键入
            this.isCountdown = true;
            this.buttonStart.setText("暂停");
            this.buttonClear.setText("停止");
            OverlayCountdown.show(3, 0.5, Color.BLACK, 80, OverlayCountdown.Corner.TOP_RIGHT, () -> {
                this.isStartInput = true;
                KeyboardInput.sendText(textArea.getText());
                Platform.runLater(() -> {
                    this.buttonStart.setText("开始键入");
                    this.buttonClear.setText("清空");
                });
                this.isCountdown = false;
                this.isStartInput = false;
                this.isPaused = false;
            });
        } else if (this.isCountdown && !this.isStartInput) {
            //正在倒计时，还未开始键入
            if (isPaused) {
                // 已暂停
                OverlayCountdown.resume();
                this.buttonStart.setText("暂停");
                this.isPaused = false;
            } else {
                // 未暂停
                OverlayCountdown.pause();
                this.buttonStart.setText("继续");
                this.isPaused = true;
            }
        }
    }
}