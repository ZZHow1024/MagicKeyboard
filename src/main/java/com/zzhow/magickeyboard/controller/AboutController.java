package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.core.ControlCenter;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.ResourceBundle;

/**
 * 关于窗口控制类
 *
 * @author ZZHow
 * create 2025/11/4
 * update 2025/11/4
 */
public class AboutController {
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

        if (ControlCenter.getLanguage().contains("zh"))
            labelTitle.setText(bundle.getString("magicKeyboard") + " 2.0.0");
        else
            labelTitle.setVisible(false);
        labelFeatures.setText(bundle.getString("about.features"));
        labelContent.setText(bundle.getString("about.content"));
    }
}
