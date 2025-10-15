package com.zzhow.magickeyboard.window;

import com.zzhow.magickeyboard.MagicKeyboardApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * @author ZZHow
 * create 2025/10/15
 * update 2025/10/15
 */
public class AboutWindow {
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
            e.printStackTrace();
        }
    }
}
