package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.KeyboardInput;
import com.zzhow.magickeyboard.factory.LongSpinnerValueFactory;
import com.zzhow.magickeyboard.util.OverlayCountdown;
import com.zzhow.magickeyboard.window.AboutWindow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 主窗口控制类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2025/11/3
 */
public class MainController {
    @FXML
    private TextArea textArea;
    @FXML
    private Button buttonStart;
    @FXML
    private Button buttonClear;
    @FXML
    private Button buttonAbout;
    @FXML
    private Spinner<Long> spinner;
    @FXML
    private Label labelTypeInterval;
    @FXML
    private Label labelMillisecond;
    @FXML
    private ChoiceBox<String> choiceBoxMode;
    @FXML
    private ChoiceBox<String> choiceBoxPosition;
    @FXML
    private ChoiceBox<String> choiceBoxLanguage;

    @FXML
    private void initialize() {
        KeyboardInput.sendText("");
        ControlCenter.onPaused = () -> Platform.runLater(this::pause);
        ControlCenter.onResume = () -> Platform.runLater(this::resume);
        ControlCenter.onResetStatus = () -> Platform.runLater(this::resetStatus);
        spinner.setValueFactory(new LongSpinnerValueFactory(10L, 1000L, 35L, 1L));
        // 限制只能输入整数
        spinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d*")) {
                spinner.getEditor().setText(oldValue);
            } else {
                try {
                    if (!newValue.isEmpty() && !newValue.equals("-")) {
                        long value = Long.parseLong(newValue);
                        spinner.getValueFactory().setValue(value);
                        ControlCenter.timeInterval = value;
                    }
                } catch (NumberFormatException e) {
                    spinner.getEditor().setText(oldValue);
                }
            }
        });

        choiceBoxLanguage.getItems().addAll("简体中文", "繁體中文", "English");
        String language = Locale.getDefault().toLanguageTag();
        if (language.contains("zh")) {
            if (language.contains("CN") || language.contains("cn"))
                language = "zh_HANS";
            else if (language.contains("HANS") || language.contains("Hans"))
                language = "zh_HANS";
            else
                language = "zh_HANT";
        } else {
            language = "en_US";
        }

        language = switch (language) {
            case "zh_HANS" -> "简体中文";
            case "zh_HANT" -> "繁體中文";
            case "en_US" -> "English";
            default -> "简体中文";
        };
        choiceBoxLanguage.setValue(language);
        switchLanguage();

        choiceBoxMode.getItems().addAll("兼容模式", "极速模式");
        choiceBoxMode.setValue("兼容模式");
        choiceBoxPosition.getItems().addAll("悬浮窗右上", "悬浮窗左上", "悬浮窗右下", "悬浮窗左下");
        choiceBoxPosition.setValue("悬浮窗右上");
    }

    @FXML
    public void onButtonClearClicked() {
        if (!ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            // 既没开始倒计时，也没在键入
            textArea.clear();
        } else {
            // 倒计时结束，开始键入
            ControlCenter.stop();
            resetStatus();
        }
    }

    @FXML
    private void onButtonStartClicked() {
        if (!ControlCenter.isCountdown && !ControlCenter.isStartInput) {
            // 既没开始倒计时，也没在键入
            ControlCenter.isCountdown = true;
            this.buttonStart.setText("暂停");
            this.buttonClear.setText("停止");
            OverlayCountdown.show(3, 0.5, Color.BLACK, 80, ControlCenter.floatingWindowPosition, () -> {
                ControlCenter.isStartInput = true;
                KeyboardInput.sendText(textArea.getText(), ControlCenter.mode);
                Platform.runLater(this::resetStatus);
            });
        } else {
            // 倒计时结束，开始键入
            if (ControlCenter.isPaused) {
                // 已暂停
                ControlCenter.resumeOrPause();
            } else {
                // 未暂停
                ControlCenter.resumeOrPause();
            }
        }
    }

    @FXML
    private void switchMode() {
        if (choiceBoxMode.getValue() == null) {
            ControlCenter.mode = ControlCenter.Mode.COMPATIBLE_MODE;
        } else {
            ControlCenter.mode = switch (choiceBoxMode.getValue()) {
                case "兼容模式" -> ControlCenter.Mode.COMPATIBLE_MODE;
                case "极速模式" -> ControlCenter.Mode.RAPID_MODE;
                default -> ControlCenter.Mode.COMPATIBLE_MODE;
            };
        }

        if (ControlCenter.mode == ControlCenter.Mode.COMPATIBLE_MODE) {
            spinner.setValueFactory(new LongSpinnerValueFactory(10L, 1000L, 35L, 1L));
        } else {
            spinner.setValueFactory(new LongSpinnerValueFactory(0L, 1000L, 10L, 1L));
        }
    }

    @FXML
    private void switchPosition() {
        if (choiceBoxPosition.getValue() == null) {
            ControlCenter.floatingWindowPosition = OverlayCountdown.Corner.TOP_RIGHT;
        } else {
            ControlCenter.floatingWindowPosition = switch (choiceBoxPosition.getValue()) {
                case "悬浮窗右上" -> OverlayCountdown.Corner.TOP_RIGHT;
                case "悬浮窗左上" -> OverlayCountdown.Corner.TOP_LEFT;
                case "悬浮窗右下" -> OverlayCountdown.Corner.BOTTOM_RIGHT;
                case "悬浮窗左下" -> OverlayCountdown.Corner.BOTTOM_LEFT;
                default -> OverlayCountdown.Corner.TOP_RIGHT;
            };
        }
    }

    @FXML
    private void onButtonAboutClicked() {
        AboutWindow.open();
    }

    @FXML
    private void switchLanguage() {
        String selectorValue = choiceBoxLanguage.getValue();
        selectorValue = switch (selectorValue) {
            case "简体中文" -> "zh_HANS";
            case "繁體中文" -> "zh_HANT";
            case "English" -> "en_US";
            default -> "zh_HANS";
        };

        ControlCenter.setLanguage(selectorValue);
        ResourceBundle bundle = ControlCenter.bundle;

        // 更新按钮文本
        if (ControlCenter.isStartInput) {
            if (ControlCenter.isPaused) {
                this.buttonStart.setText(bundle.getString("main.buttonResume"));
            } else {
                this.buttonStart.setText(bundle.getString("main.buttonPause"));
            }
            this.buttonClear.setText(bundle.getString("main.buttonStop"));
        } else {
            this.buttonStart.setText(bundle.getString("main.buttonStart"));
            this.buttonClear.setText(bundle.getString("main.buttonClear"));
        }
        
        // 更新关于按钮文本
        this.buttonAbout.setText(bundle.getString("main.buttonAbout"));
        
        // 更新标签文本
        this.labelTypeInterval.setText(bundle.getString("main.labelTypeInterval"));
        this.labelMillisecond.setText(bundle.getString("main.labelMillisecond"));

        // 更新选择框内容
        choiceBoxMode.getItems().clear();
        choiceBoxMode.getItems().addAll(bundle.getString("main.compatibleMode"), bundle.getString("main.rapidMode"));
        choiceBoxMode.setValue(ControlCenter.mode == ControlCenter.Mode.COMPATIBLE_MODE ? 
                bundle.getString("main.compatibleMode") : bundle.getString("main.rapidMode"));
        
        choiceBoxPosition.getItems().clear();
        choiceBoxPosition.getItems().addAll(
                bundle.getString("main.floatingWindowTopRight"),
                bundle.getString("main.floatingWindowTopLeft"),
                bundle.getString("main.floatingWindowBottomRight"),
                bundle.getString("main.floatingWindowBottomLeft")
        );
        
        // 根据当前位置设置选择框的值
        String positionValue = switch (ControlCenter.floatingWindowPosition) {
            case TOP_RIGHT -> bundle.getString("main.floatingWindowTopRight");
            case TOP_LEFT -> bundle.getString("main.floatingWindowTopLeft");
            case BOTTOM_RIGHT -> bundle.getString("main.floatingWindowBottomRight");
            case BOTTOM_LEFT -> bundle.getString("main.floatingWindowBottomLeft");
        };
        choiceBoxPosition.setValue(positionValue);
    }

    // 重置状态
    private void resetStatus() {
        this.buttonStart.setText("开始键入");
        this.buttonClear.setText("清空");
        ControlCenter.isCountdown = false;
        ControlCenter.isStartInput = false;
        ControlCenter.isPaused = false;
    }

    // 暂停
    private void pause() {
        this.buttonStart.setText("继续");
        this.buttonClear.setText("停止");
    }

    // 继续
    private void resume() {
        this.buttonStart.setText("暂停");
    }
}