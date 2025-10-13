package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.core.KeyboardInput;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

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
    private void initialize() {
        KeyboardInput.sendText("");
    }

    @FXML
    public void onButtonClearClicked() {
        textArea.clear();
    }

    @FXML
    private void onButtonStartClicked() {
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> {
            KeyboardInput.sendText(textArea.getText());
        });
        delay.play();
    }
}