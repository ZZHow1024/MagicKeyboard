package com.zzhow.magickeyboard.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

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
    private Button buttonClear;

    @FXML
    public void onButtonClearClicked() {
        textArea.clear();
    }
}