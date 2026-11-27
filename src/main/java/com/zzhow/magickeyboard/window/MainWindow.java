package com.zzhow.magickeyboard.window;

import com.zzhow.magickeyboard.MagicKeyboardApplication;
import com.zzhow.magickeyboard.config.PreferencesManager;
import com.zzhow.magickeyboard.core.KeyboardInput;
import com.zzhow.magickeyboard.core.hotkey.Hotkeys;
import com.zzhow.magickeyboard.core.hotkey.IHotkeyManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * 主窗口类
 *
 * @author ZZHow
 * create 2025/10/12
 * update 2026/11/27
 */
public class MainWindow extends Application {

    private Stage stage;
    private IHotkeyManager hotkeyManager;

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
        applyPersistedBounds(stage);
        stage.show();
        this.stage = stage;

        // 注册全局热键（Ctrl+Alt+M 开始/暂停/继续，Ctrl+Alt+S 停止），回调切回 JavaFX 线程
        hotkeyManager = Hotkeys.create();
        Hotkeys.registerDefaults(hotkeyManager,
                () -> Platform.runLater(() -> {
                    Runnable action = com.zzhow.magickeyboard.core.ControlCenter.onHotkeyToggle;
                    if (action != null) {
                        action.run();
                    }
                }),
                () -> Platform.runLater(() -> {
                    Runnable action = com.zzhow.magickeyboard.core.ControlCenter.onHotkeyStop;
                    if (action != null) {
                        action.run();
                    }
                }));
    }

    /**
     * 恢复上次关闭时的窗口尺寸与位置（位置不在任何屏幕内时使用默认值）
     */
    private static void applyPersistedBounds(Stage stage) {
        double[] bounds = PreferencesManager.loadWindowBounds();
        if (bounds == null) {
            return;
        }
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D area = screen.getVisualBounds();
            if (area.contains(bounds[0], bounds[1])) {
                stage.setX(bounds[0]);
                stage.setY(bounds[1]);
                stage.setWidth(bounds[2]);
                stage.setHeight(bounds[3]);
                return;
            }
        }
    }

    @Override
    public void stop() {
        // 关闭主窗口/应用退出时注销热键、保存窗口状态并停止模拟键入
        if (hotkeyManager != null) {
            hotkeyManager.shutdown();
            hotkeyManager = null;
        }
        if (stage != null) {
            PreferencesManager.saveWindowBounds(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        }
        KeyboardInput.stop();
    }

    public static void show() {
        launch();
    }
}
