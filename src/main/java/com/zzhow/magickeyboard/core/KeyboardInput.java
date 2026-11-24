package com.zzhow.magickeyboard.core;

import com.zzhow.magickeyboard.core.impl.LinuxKeyboard;
import com.zzhow.magickeyboard.core.impl.MacKeyboard;
import com.zzhow.magickeyboard.core.impl.WindowsKeyboard;
import com.zzhow.magickeyboard.util.TextUtil;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * 模拟键盘输入核心入口类
 *
 * @author ZZHow
 * create 2025/10/13
 * update 2026/11/23
 */
public class KeyboardInput {

    private static final Logger LOGGER = Logger.getLogger(KeyboardInput.class.getName());

    private static final IKeyboard keyboardImpl;

    static {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        LOGGER.info("OS = " + os);
        if (os.contains("win")) {
            keyboardImpl = new WindowsKeyboard();
        } else if (os.contains("mac")) {
            keyboardImpl = new MacKeyboard();
        } else {
            keyboardImpl = new LinuxKeyboard();
        }
        LOGGER.info("keyboardImpl = " + keyboardImpl.getClass().getName());
    }

    /**
     * 根据传入的字符串开始键盘输入
     */
    public static void sendText(String text, ControlCenter.Mode mode) {
        if (ControlCenter.isIgnoreLeadingWhitespace) {
            // 忽略每行行首的空字符
            text = TextUtil.stripLeadingWhitespace(text);
        }
        keyboardImpl.sendText(text, mode);
    }

    /**
     * 停止键盘输入
     */
    public static void stop() {
        keyboardImpl.stop();
    }

    /**
     * 暂停键盘输入
     */
    public static void pause() {
        keyboardImpl.pause();
    }

    /**
     * 阻塞式继续键盘输入，等待键盘键入完毕后才执行结束
     */
    public static void resume() {
        keyboardImpl.resume();
    }
}
