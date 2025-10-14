package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.KeyboardInput;
import com.zzhow.magickeyboard.util.OverlayCountdown;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    private ChoiceBox<String> choiceBoxPosition;

    @FXML
    private void initialize() {
        KeyboardInput.sendText("");
        ControlCenter.onPaused = () -> Platform.runLater(this::pause);
        ControlCenter.onResume = () -> Platform.runLater(this::resume);
        ControlCenter.onResetStatus = () -> Platform.runLater(this::resetStatus);
        choiceBoxPosition.getItems().addAll("悬浮窗右上", "悬浮窗左上", "悬浮窗右下", "悬浮窗左下");
        choiceBoxPosition.setValue("悬浮窗右上");
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

    @FXML
    private void onButtonStartClicked() {
        if (!ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            // 既没开始倒计时，也没在键入
            ControlCenter.isCountdown = true;
            this.buttonStart.setText("暂停");
            this.buttonClear.setText("停止");
            OverlayCountdown.show(3, 0.5, Color.BLACK, 80, ControlCenter.floatingWindowPosition, () -> {
                ControlCenter.isStartInput = true;
                KeyboardInput.sendText(textArea.getText());
                Platform.runLater(this::resetStatus);
            });
        } else {
            // 倒计时结束，开始键入
            if (ControlCenter.isPaused) {
                // 已暂停
                ControlCenter.resumeOrPause();
            } else {
                // 未暂停
                ControlCenter.resumeOrPause();
            }
        }
    }

    @FXML
    private void switchPosition() {
        ControlCenter.floatingWindowPosition = switch (choiceBoxPosition.getValue()) {
            case "悬浮窗右上" -> OverlayCountdown.Corner.TOP_RIGHT;
            case "悬浮窗左上" -> OverlayCountdown.Corner.TOP_LEFT;
            case "悬浮窗右下" -> OverlayCountdown.Corner.BOTTOM_RIGHT;
            case "悬浮窗左下" -> OverlayCountdown.Corner.BOTTOM_LEFT;
            default -> OverlayCountdown.Corner.TOP_RIGHT;
        };
    }

    // 重置状态
    private void resetStatus() {
        this.buttonStart.setText("开始键入");
        this.buttonClear.setText("清空");
        ControlCenter.isCountdown = false;
        ControlCenter.isStartInput = false;
        ControlCenter.isPaused = false;
    }

    // 暂停
    private void pause() {
        this.buttonStart.setText("继续");
        this.buttonClear.setText("停止");
    }

    // 继续
    private void resume() {
        this.buttonStart.setText("暂停");
    }
}