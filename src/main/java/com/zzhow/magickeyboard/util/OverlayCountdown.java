package com.zzhow.magickeyboard.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

/**
 * 透明倒计时蒙层工具类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/11/3
 */
public class OverlayCountdown {
    private static Timeline currentTimeline;
    private static Stage currentStage;
    private static volatile boolean isPaused = false;
    private static volatile boolean isStopped = false;
    private static double remainingSeconds = 0;
    private static FadeTransition currentFadeOut;
    private static Label currentCountdownLabel;
    private static ResourceBundle bundle;
    private static String currentLanguage = "zh_HANS";

    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    /**
     * 显示透明倒计时蒙层
     *
     * @param seconds         倒计时秒数
     * @param opacity         背景透明度 (0~1)
     * @param backgroundColor 背景颜色
     * @param fontSize        倒计时字体大小
     * @param corner          显示位置
     * @param onFinish        倒计时结束回调（可为 null）
     */
    public static void show(int seconds, double opacity, Color backgroundColor,
                            int fontSize, Corner corner, Runnable onFinish) {

        Platform.runLater(() -> {
            // 重置状态
            isStopped = false;
            isPaused = false;
            remainingSeconds = seconds;

            // 加载语言资源
            bundle = ResourceBundle.getBundle("MessagesBundle_" + currentLanguage);

            Stage overlayStage = new Stage();
            overlayStage.initStyle(StageStyle.TRANSPARENT);
            overlayStage.setAlwaysOnTop(true);

            // 倒计时文字
            Label countdownLabel = new Label(String.valueOf(seconds));
            countdownLabel.setTextFill(Color.WHITE);
            countdownLabel.setStyle("-fx-font-weight: bold;");
            countdownLabel.setFont(javafx.scene.text.Font.font(fontSize));

            // 保存当前倒计时标签引用
            currentCountdownLabel = countdownLabel;

            // 标题文字
            Label titleLabel = null;
            VBox vbox = new VBox(5); // 倒计时与标题间距 5px
            vbox.setAlignment(Pos.CENTER);

            String titleText = "MagicKeyboard";
            int titleFontSize = 20;
            titleLabel = new Label(titleText);
            titleLabel.setTextFill(Color.WHITE);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-wrap-text: true; -fx-text-alignment: center;");
            titleLabel.setFont(javafx.scene.text.Font.font(20));
            titleLabel.setMaxWidth(Double.MAX_VALUE); // 允许宽度自适应
            titleLabel.setAlignment(Pos.CENTER); // 文本居中对齐

            // 计算标题所需的最小宽度
            titleLabel.setMinWidth(titleText.length() * titleFontSize * 0.6);

            // 按钮区域 - 水平排列
            Button pauseResumeButton = new Button("⏯");
            Button stopButton = new Button("⏹");

            // 设置按钮样式
            pauseResumeButton.setStyle("-fx-font-size: 16px; -fx-padding: 8px 12px; -fx-min-width: 40px;");
            stopButton.setStyle("-fx-font-size: 16px; -fx-padding: 8px 12px; -fx-min-width: 40px;");

            // 设置按钮点击事件
            pauseResumeButton.setOnAction(e -> {
                com.zzhow.magickeyboard.core.ControlCenter.resumeOrPause();
            });

            stopButton.setOnAction(e -> {
                com.zzhow.magickeyboard.core.ControlCenter.stop();
            });

            // 创建水平按钮容器
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.getChildren().addAll(pauseResumeButton, stopButton);

            vbox.getChildren().addAll(titleLabel, countdownLabel, buttonBox);

            StackPane root = new StackPane(vbox);

            double padding = fontSize * 1.5; // 比数字大
            root.setStyle(String.format(
                    "-fx-background-color: rgba(%d,%d,%d,%.2f); -fx-padding: %.1f;",
                    (int) (backgroundColor.getRed() * 255),
                    (int) (backgroundColor.getGreen() * 255),
                    (int) (backgroundColor.getBlue() * 255),
                    opacity, padding
            ));

            Scene scene = new Scene(root, Color.TRANSPARENT);
            overlayStage.setScene(scene);
            scene.setFill(Color.TRANSPARENT);

            // 自动计算大小
            vbox.applyCss();
            vbox.layout();

            // 确保窗口宽度足够显示标题
            double stageWidth = vbox.getWidth() + padding * 2;
            double stageHeight = vbox.getHeight() + padding * 2;

            // 确保窗口宽度至少能显示完整标题
            stageWidth = Math.max(stageWidth, titleText.length() * titleFontSize * 0.8 + padding * 2);

            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            double x = 0, y = 0;
            switch (corner) {
                case TOP_LEFT:
                    x = screenBounds.getMinX();
                    y = screenBounds.getMinY();
                    break;
                case TOP_RIGHT:
                    x = screenBounds.getMaxX() - stageWidth;
                    y = screenBounds.getMinY();
                    break;
                case BOTTOM_LEFT:
                    x = screenBounds.getMinX();
                    y = screenBounds.getMaxY() - stageHeight;
                    break;
                case BOTTOM_RIGHT:
                    x = screenBounds.getMaxX() - stageWidth;
                    y = screenBounds.getMaxY() - stageHeight;
                    break;
            }

            overlayStage.setX(x);
            overlayStage.setY(y);
            overlayStage.setWidth(stageWidth);
            overlayStage.setHeight(stageHeight);

            overlayStage.show();

            // 保存当前实例引用
            currentStage = overlayStage;

            // 淡出动画（提前定义，供回调函数使用）
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.8), root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                overlayStage.close();
                currentStage = null;
                currentTimeline = null;
                currentFadeOut = null;
                currentCountdownLabel = null;
            });

            // 倒计时逻辑
            Timeline timeline = new Timeline();
            for (int i = 0; i <= seconds; i++) {
                int remaining = seconds - i;
                timeline.getKeyFrames().add(new KeyFrame(
                        Duration.seconds(i),
                        e -> {
                            // 检查是否停止
                            if (isStopped) {
                                timeline.stop();
                                overlayStage.close();
                                return;
                            }

                            // 检查是否暂停
                            if (isPaused) {
                                timeline.pause();
                                return;
                            }

                            if (remaining == 0) {
                                // 倒计时结束时显示"正在键入"并调整字体大小
                                countdownLabel.setStyle("-fx-font-size: 50px;");
                                // 透明度渐变
                                Timeline fade = new Timeline(
                                        new KeyFrame(Duration.seconds(0), new KeyValue(countdownLabel.opacityProperty(), 1.0, Interpolator.EASE_BOTH)),
                                        new KeyFrame(Duration.seconds(0.5), new KeyValue(countdownLabel.opacityProperty(), 0.0, Interpolator.EASE_BOTH))
                                );
                                fade.setCycleCount(Timeline.INDEFINITE);
                                fade.setAutoReverse(true);
                                fade.play();
                                countdownLabel.setText(bundle.getString("overlay.typing"));
                            } else {
                                countdownLabel.setText(String.valueOf(remaining));
                            }
                            remainingSeconds = remaining;

                            // 倒计时结束时执行回调
                            if (remaining == 0 && onFinish != null) {
                                // 在新线程中执行回调
                                new Thread(() -> {
                                    try {
                                        onFinish.run();
                                    } finally {
                                        // 回调完成后在JavaFX线程中执行淡出动画
                                        Platform.runLater(() -> {
                                            if (!isStopped) {
                                                currentFadeOut = fadeOut;
                                                fadeOut.play();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        }
                ));
            }

            currentTimeline = timeline;
            currentFadeOut = fadeOut;
            timeline.play();
        });
    }

    /**
     * 停止倒计时
     */
    public static void stop() {
        isStopped = true;
        isPaused = false;

        Platform.runLater(() -> {
            if (currentTimeline != null) {
                currentTimeline.stop();
            }
            if (currentFadeOut != null) {
                currentFadeOut.stop();
            }
            if (currentStage != null) {
                currentStage.close();
                currentStage = null;
                currentTimeline = null;
                currentFadeOut = null;
            }
        });
    }

    /**
     * 设置当前语言
     *
     * @param language 语言代码，如 en_US, zh_HANS, zh_HANT
     */
    public static void setLanguage(String language) {
        currentLanguage = language;

        // 如果当前有显示的悬浮窗，更新其文本
        if (currentStage != null && currentStage.isShowing()) {
            try {
                bundle = ResourceBundle.getBundle("MessagesBundle_" + language);

                if (currentCountdownLabel != null && remainingSeconds == 0) {
                    if (isPaused) {
                        currentCountdownLabel.setText(bundle.getString("overlay.paused"));
                    } else {
                        currentCountdownLabel.setText(bundle.getString("overlay.typing"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 暂停倒计时
     */
    public static void pause() {
        isPaused = true;

        Platform.runLater(() -> {
            if (currentTimeline != null && currentTimeline.getStatus() == Animation.Status.RUNNING) {
                currentTimeline.pause();
            }

            // 如果倒计时已经结束（剩余秒数为0），更新显示文字为"已暂停"
            if (remainingSeconds == 0 && currentCountdownLabel != null) {
                currentCountdownLabel.setText(bundle.getString("overlay.paused"));
            }
        });
    }

    /**
     * 继续倒计时
     */
    public static void resume() {
        isPaused = false;

        Platform.runLater(() -> {
            if (currentTimeline != null && currentTimeline.getStatus() == Animation.Status.PAUSED) {
                currentTimeline.play();
            }

            // 如果倒计时已经结束（剩余秒数为0），恢复显示"正在键入"
            if (remainingSeconds == 0 && currentCountdownLabel != null) {
                currentCountdownLabel.setText(bundle.getString("overlay.typing"));
            }
        });
    }

    /**
     * 获取剩余秒数
     */
    public static double getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * 检查倒计时是否正在运行
     */
    public static boolean isRunning() {
        return currentTimeline != null && currentTimeline.getStatus() == Animation.Status.RUNNING;
    }

    /**
     * 检查倒计时是否已暂停
     */
    public static boolean isPaused() {
        return isPaused;
    }

    /**
     * 检查倒计时是否已停止
     */
    public static boolean isStopped() {
        return isStopped;
    }
}