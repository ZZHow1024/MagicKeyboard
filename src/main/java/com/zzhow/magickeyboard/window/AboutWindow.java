package com.zzhow.magickeyboard.window;

import com.zzhow.magickeyboard.MagicKeyboardApplication;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 关于窗口类
 *
 * @author ZZHow
 * create 2025/10/15
 * update 2026/11/23
 */
public class AboutWindow {

    private static final Logger LOGGER = Logger.getLogger(AboutWindow.class.getName());

    public static void open() {
        Stage stage = new Stage();
        stage.setTitle("MagicKeyboard - About");
        Image icon = new Image(Objects.requireNonNull(MagicKeyboardApplication.class.getResourceAsStream("/image/MagicKeyboard.png")));
        stage.getIcons().add(icon);
        stage.setResizable(false);
        try {
            Pane load = FXMLLoader.load(Objects.requireNonNull(AboutWindow.class.getResource("about-view.fxml")));
            Scene scene = new Scene(load);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open the About window.", e);
        }
    }
}
