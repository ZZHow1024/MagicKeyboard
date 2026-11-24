package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.MagicKeyboardApplication;
import com.zzhow.magickeyboard.core.ControlCenter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.ResourceBundle;

/**
 * 关于窗口控制类
 *
 * @author ZZHow
 * create 2025/11/4
 * update 2026/11/23
 */
public class AboutController {
    @FXML
    private Label labelTitleEn;
    @FXML
    private Label labelTitle;
    @FXML
    private Label labelFeatures;
    @FXML
    private Label labelContent;

    @FXML
    private void initialize() {
        switchLanguage();
    }

    private void switchLanguage() {
        ResourceBundle bundle = ControlCenter.bundle;
        String version = MagicKeyboardApplication.VERSION;

        labelTitleEn.setText("MagicKeyboard " + version);
        if (ControlCenter.getLanguage().contains("zh")) {
            labelTitle.setText(bundle.getString("magicKeyboard") + " " + version);
            labelTitle.setVisible(true);
        } else {
            labelTitle.setVisible(false);
        }
        labelFeatures.setText(bundle.getString("about.features"));
        labelContent.setText(bundle.getString("about.content"));
    }
}
