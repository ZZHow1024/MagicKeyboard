package com.zzhow.magickeyboard.controller;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.KeyboardInput;
import com.zzhow.magickeyboard.config.PreferencesManager;
import com.zzhow.magickeyboard.util.OverlayCountdown;
import com.zzhow.magickeyboard.window.AboutWindow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 主窗口控制类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2026/11/23
 */
public class MainController {

    private static final long INTERVAL_MAX = 1000L;
    private static final long INTERVAL_MIN_COMPATIBLE = 10L;
    private static final long INTERVAL_MIN_RAPID = 0L;

    @FXML
    private TextArea textArea;
    @FXML
    private Button buttonStart;
    @FXML
    private Button buttonClear;
    @FXML
    private Button buttonAbout;
    @FXML
    private Spinner<Integer> spinner;
    @FXML
    private Label labelTypeInterval;
    @FXML
    private Label labelMillisecond;
    @FXML
    private CheckBox checkboxIgnoreLeadingWhitespace;
    @FXML
    private ChoiceBox<ControlCenter.Mode> choiceBoxMode;
    @FXML
    private ChoiceBox<OverlayCountdown.Corner> choiceBoxPosition;
    @FXML
    private ChoiceBox<String> choiceBoxLanguage;

    @FXML
    private void initialize() {
        // 恢复持久化配置
        ControlCenter.mode = PreferencesManager.loadMode();
        ControlCenter.timeInterval = PreferencesManager.loadInterval();
        ControlCenter.floatingWindowPosition = PreferencesManager.loadPosition();
        ControlCenter.isIgnoreLeadingWhitespace = PreferencesManager.loadIgnoreLeadingWhitespace();

        ControlCenter.onPaused = () -> Platform.runLater(this::pause);
        ControlCenter.onResume = () -> Platform.runLater(this::resume);
        ControlCenter.onResetStatus = () -> Platform.runLater(this::resetStatus);

        // 键入模式：选项直接存储枚举值，界面文案由转换器根据当前语言生成
        choiceBoxMode.setConverter(new StringConverter<>() {
            @Override
            public String toString(ControlCenter.Mode mode) {
                return modeLabel(mode);
            }

            @Override
            public ControlCenter.Mode fromString(String string) {
                return null;
            }
        });
        choiceBoxMode.getItems().setAll(ControlCenter.Mode.COMPATIBLE_MODE, ControlCenter.Mode.RAPID_MODE);
        choiceBoxMode.setValue(ControlCenter.mode);

        // 悬浮窗位置：选项直接存储枚举值
        choiceBoxPosition.setConverter(new StringConverter<>() {
            @Override
            public String toString(OverlayCountdown.Corner corner) {
                return cornerLabel(corner);
            }

            @Override
            public OverlayCountdown.Corner fromString(String string) {
                return null;
            }
        });
        choiceBoxPosition.getItems().setAll(
                OverlayCountdown.Corner.TOP_RIGHT,
                OverlayCountdown.Corner.TOP_LEFT,
                OverlayCountdown.Corner.BOTTOM_RIGHT,
                OverlayCountdown.Corner.BOTTOM_LEFT
        );
        choiceBoxPosition.setValue(ControlCenter.floatingWindowPosition);

        // 键入间隔：只能输入整数，并按当前模式的上下限收敛
        applyIntervalBounds();
        spinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d*")) {
                spinner.getEditor().setText(oldValue);
                return;
            }
            if (newValue.isEmpty() || newValue.equals("-")) {
                return;
            }
            try {
                long value = Long.parseLong(newValue);
                long clamped = Math.max(intervalMin(), Math.min(INTERVAL_MAX, value));
                if (clamped != value) {
                    spinner.getEditor().setText(String.valueOf(clamped));
                }
                spinner.getValueFactory().setValue((int) clamped);
                ControlCenter.timeInterval = clamped;
                PreferencesManager.saveInterval(clamped);
            } catch (NumberFormatException e) {
                spinner.getEditor().setText(oldValue);
            }
        });

        // 复选框显示状态与全局状态同步
        checkboxIgnoreLeadingWhitespace.setSelected(ControlCenter.isIgnoreLeadingWhitespace);

        // 语言：优先使用上次保存的选择，未保存过时按系统语言初始化
        choiceBoxLanguage.getItems().addAll("简体中文", "繁體中文", "English");
        String savedLanguage = PreferencesManager.loadLanguage();
        choiceBoxLanguage.setValue(displayNameOf(savedLanguage.isEmpty()
                ? detectSystemLanguage()
                : savedLanguage));
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
            ControlCenter.resumeOrPause();
        }
    }

    @FXML
    private void switchMode() {
        ControlCenter.Mode selected = choiceBoxMode.getValue();
        if (selected == null) {
            return;
        }
        ControlCenter.mode = selected;
        PreferencesManager.saveMode(selected);
        // 切换模式后键入间隔的上下限不同（极速模式允许 0ms），收敛当前值而不是重置为默认值
        applyIntervalBounds();
    }

    @FXML
    private void switchPosition() {
        OverlayCountdown.Corner selected = choiceBoxPosition.getValue();
        if (selected == null) {
            return;
        }
        ControlCenter.floatingWindowPosition = selected;
        PreferencesManager.savePosition(selected);
    }

    @FXML
    private void onButtonAboutClicked() {
        AboutWindow.open();
    }

    @FXML
    private void onCheckBoxClicked() {
        ControlCenter.isIgnoreLeadingWhitespace = checkboxIgnoreLeadingWhitespace.isSelected();
        PreferencesManager.saveIgnoreLeadingWhitespace(ControlCenter.isIgnoreLeadingWhitespace);
    }

    @FXML
    private void switchLanguage() {
        String languageCode = languageCodeOf(choiceBoxLanguage.getValue());

        // 先更新全局语言与 bundle，再刷新依赖文案的组件
        ControlCenter.setLanguage(languageCode);
        PreferencesManager.saveLanguage(languageCode);
        OverlayCountdown.setLanguage(languageCode);
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

        // 刷新模式/位置选择框的显示文案（选项仍是枚举值，仅触发重新渲染）
        ControlCenter.Mode mode = choiceBoxMode.getValue();
        choiceBoxMode.setValue(null);
        choiceBoxMode.setValue(mode == null ? ControlCenter.mode : mode);

        OverlayCountdown.Corner corner = choiceBoxPosition.getValue();
        choiceBoxPosition.setValue(null);
        choiceBoxPosition.setValue(corner == null ? ControlCenter.floatingWindowPosition : corner);
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

    // 按当前键入模式设置间隔的上下限，并保留用户已设置的值
    private void applyIntervalBounds() {
        long clamped = Math.max(intervalMin(), Math.min(INTERVAL_MAX, ControlCenter.timeInterval));
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory((int) intervalMin(), (int) INTERVAL_MAX, (int) clamped, 1));
        ControlCenter.timeInterval = clamped;
    }

    private long intervalMin() {
        return ControlCenter.mode == ControlCenter.Mode.RAPID_MODE
                ? INTERVAL_MIN_RAPID
                : INTERVAL_MIN_COMPATIBLE;
    }

    private static String modeLabel(ControlCenter.Mode mode) {
        ResourceBundle bundle = ControlCenter.bundle;
        return mode == ControlCenter.Mode.RAPID_MODE
                ? bundle.getString("main.rapidMode")
                : bundle.getString("main.compatibleMode");
    }

    private static String cornerLabel(OverlayCountdown.Corner corner) {
        ResourceBundle bundle = ControlCenter.bundle;
        return switch (corner) {
            case TOP_RIGHT -> bundle.getString("main.floatingWindowTopRight");
            case TOP_LEFT -> bundle.getString("main.floatingWindowTopLeft");
            case BOTTOM_RIGHT -> bundle.getString("main.floatingWindowBottomRight");
            case BOTTOM_LEFT -> bundle.getString("main.floatingWindowBottomLeft");
        };
    }

    /**
     * 检测系统语言：zh 按文字系统（Hans/Hant）与地区判断简繁，其余默认英文
     */
    private static String detectSystemLanguage() {
        Locale locale = Locale.getDefault();
        if (!"zh".equals(locale.getLanguage())) {
            return "en_US";
        }
        if ("Hant".equalsIgnoreCase(locale.getScript())) {
            return "zh_HANT";
        }
        if ("Hans".equalsIgnoreCase(locale.getScript())) {
            return "zh_HANS";
        }
        // 无文字系统标记时按地区判断
        return switch (locale.getCountry()) {
            case "TW", "HK", "MO" -> "zh_HANT";
            default -> "zh_HANS";
        };
    }

    private static String displayNameOf(String languageCode) {
        return switch (languageCode) {
            case "zh_HANT" -> "繁體中文";
            case "en_US" -> "English";
            default -> "简体中文";
        };
    }

    private static String languageCodeOf(String displayName) {
        return switch (displayName) {
            case "繁體中文" -> "zh_HANT";
            case "English" -> "en_US";
            default -> "zh_HANS";
        };
    }
}
