package com.zzhow.magickeyboard.window;

import com.zzhow.magickeyboard.MagicKeyboardApplication;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * 主窗口类
 *
 * @author ZZHow
 * @date 2025/10/12
 */
public class MainWindow extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 780, 520);
        stage.setTitle("MagicKeyboard");
        stage.setScene(scene);
        stage.setMinHeight(400);
        stage.setMinWidth(600);
        Image icon = new Image(Objects.requireNonNull(MagicKeyboardApplication.class.getResourceAsStream("/image/MagicKeyboard.png")));
        stage.getIcons().add(icon);
        stage.show();
    }

    public static void show() {
        launch();
    }
}