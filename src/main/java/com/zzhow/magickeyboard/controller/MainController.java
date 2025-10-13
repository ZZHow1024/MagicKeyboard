package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.core.KeyboardInput;
import com.zzhow.magickeyboard.util.OverlayCountdown;
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
    @FXML
    private TextArea textArea;
    @FXML
    private Button buttonStart;

    @FXML
    private void initialize() {
        KeyboardInput.sendText("");
    }

    @FXML
    public void onButtonClearClicked() {
        textArea.clear();
    }

    @FXML
    private void onButtonStartClicked() {
        buttonStart.setDisable(true);
        OverlayCountdown.show(3, 0.5, Color.BLACK, 80, OverlayCountdown.Corner.TOP_RIGHT, () -> {
            KeyboardInput.sendText(textArea.getText());
            buttonStart.setDisable(false);
        });
    }
}