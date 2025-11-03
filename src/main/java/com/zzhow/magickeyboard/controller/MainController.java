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
    private CheckBox checkboxIgnoreLeadingWhitespace;
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

        OverlayCountdown.setLanguage(language);

        language = switch (language) {
            case "zh_HANS" -> "简体中文";
            case "zh_HANT" -> "繁體中文";
            case "en_US" -> "English";
            default -> "简体中文";
        };
        choiceBoxLanguage.setValue(language);
        switchLanguage();
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
            ResourceBundle bundle = ControlCenter.bundle;
            this.buttonStart.setText(bundle.getString("main.buttonPause"));
            this.buttonClear.setText(bundle.getString("main.buttonStop"));
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
            ResourceBundle bundle = ControlCenter.bundle;
            String compatibleMode = bundle.getString("main.compatibleMode");
            String rapidMode = bundle.getString("main.rapidMode");

            ControlCenter.mode = switch (choiceBoxMode.getValue()) {
                case String s when s.equals(compatibleMode) -> ControlCenter.Mode.COMPATIBLE_MODE;
                case String s when s.equals(rapidMode) -> ControlCenter.Mode.RAPID_MODE;
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
            ResourceBundle bundle = ControlCenter.bundle;
            String topRight = bundle.getString("main.floatingWindowTopRight");
            String topLeft = bundle.getString("main.floatingWindowTopLeft");
            String bottomRight = bundle.getString("main.floatingWindowBottomRight");
            String bottomLeft = bundle.getString("main.floatingWindowBottomLeft");

            ControlCenter.floatingWindowPosition = switch (choiceBoxPosition.getValue()) {
                case String s when s.equals(topRight) -> OverlayCountdown.Corner.TOP_RIGHT;
                case String s when s.equals(topLeft) -> OverlayCountdown.Corner.TOP_LEFT;
                case String s when s.equals(bottomRight) -> OverlayCountdown.Corner.BOTTOM_RIGHT;
                case String s when s.equals(bottomLeft) -> OverlayCountdown.Corner.BOTTOM_LEFT;
                default -> OverlayCountdown.Corner.TOP_RIGHT;
            };
        }
    }

    @FXML
    private void onButtonAboutClicked() {
        AboutWindow.open();
    }

    @FXML
    private void onCheckBoxClicked() {
        ControlCenter.isIgnoreLeadingWhitespace = checkboxIgnoreLeadingWhitespace.isSelected();
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

        OverlayCountdown.setLanguage(selectorValue);

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

        // 更新复选框文本
        this.checkboxIgnoreLeadingWhitespace.setText(bundle.getString("main.checkboxIgnoreLeadingWhitespace"));

        // 更新选择框内容
        ControlCenter.Mode mode = ControlCenter.mode;
        choiceBoxMode.getItems().clear();
        choiceBoxMode.getItems().addAll(bundle.getString("main.compatibleMode"), bundle.getString("main.rapidMode"));
        choiceBoxMode.setValue(mode == ControlCenter.Mode.COMPATIBLE_MODE ?
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
        ResourceBundle bundle = ControlCenter.bundle;
        this.buttonStart.setText(bundle.getString("main.buttonStart"));
        this.buttonClear.setText(bundle.getString("main.buttonClear"));
        ControlCenter.isCountdown = false;
        ControlCenter.isStartInput = false;
        ControlCenter.isPaused = false;
    }

    // 暂停
    private void pause() {
        ResourceBundle bundle = ControlCenter.bundle;
        this.buttonStart.setText(bundle.getString("main.buttonResume"));
        this.buttonClear.setText(bundle.getString("main.buttonStop"));
    }

    // 继续
    private void resume() {
        ResourceBundle bundle = ControlCenter.bundle;
        this.buttonStart.setText(bundle.getString("main.buttonPause"));
    }
}