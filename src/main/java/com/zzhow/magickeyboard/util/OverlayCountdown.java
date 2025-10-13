package com.zzhow.magickeyboard.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

/**
 * 透明倒计时蒙层工具类
 *
 * @author ZZHow
 * @date 2025/10/13
 */
public class OverlayCountdown {

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
            Stage overlayStage = new Stage();
            overlayStage.initStyle(StageStyle.TRANSPARENT);
            overlayStage.setAlwaysOnTop(true);

            // 倒计时文字
            Label countdownLabel = new Label(String.valueOf(seconds));
            countdownLabel.setTextFill(Color.WHITE);
            countdownLabel.setStyle("-fx-font-weight: bold;");
            countdownLabel.setFont(javafx.scene.text.Font.font(fontSize));

            // 标题文字
            Label titleLabel = null;
            VBox vbox = new VBox(5); // 倒计时与标题间距 5px
            vbox.setAlignment(Pos.CENTER);

            String titleText = "MagicKeyboard";
            int titleFontSize = 20;
            titleLabel = new Label("MagicKeyboard");
            titleLabel.setTextFill(Color.WHITE);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-wrap-text: true; -fx-text-alignment: center;");
            titleLabel.setFont(javafx.scene.text.Font.font(20));
            titleLabel.setMaxWidth(Double.MAX_VALUE); // 允许宽度自适应
            titleLabel.setAlignment(Pos.CENTER); // 文本居中对齐

            // 计算标题所需的最小宽度
            titleLabel.setMinWidth(titleText.length() * titleFontSize * 0.6);

            vbox.getChildren().addAll(titleLabel, countdownLabel);

            StackPane root = new StackPane(vbox);
            root.setMouseTransparent(true);

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

            // 倒计时逻辑
            Timeline timeline = new Timeline();
            for (int i = 0; i <= seconds; i++) {
                int remaining = seconds - i;
                timeline.getKeyFrames().add(new KeyFrame(
                        Duration.seconds(i),
                        e -> {
                            countdownLabel.setText(remaining > 0 ? String.valueOf(remaining) : "");
                            // 倒计时结束时立即触发回调
                            if (remaining == 0 && onFinish != null) {
                                new Thread(onFinish).start();
                            }
                        }
                ));
            }

            // 淡出动画
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.8), root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> overlayStage.close());

            timeline.setOnFinished(e -> fadeOut.play());
            timeline.play();
        });
    }
}