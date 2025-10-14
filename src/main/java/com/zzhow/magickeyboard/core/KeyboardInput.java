package com.zzhow.magickeyboard.core;

import com.zzhow.magickeyboard.core.impl.LinuxKeyboard;
import com.zzhow.magickeyboard.core.impl.MacKeyboard;
import com.zzhow.magickeyboard.core.impl.WindowsKeyboard;

/**
 * 模拟键盘输入核心入口类
 *
 * @author ZZHow
 * @date 2025/10/13
 */

public class KeyboardInput {

    private static final IKeyboard keyboardImpl;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("OS = " + os);
        if (os.contains("win")) {
            keyboardImpl = new WindowsKeyboard();
        } else if (os.contains("mac")) {
            keyboardImpl = new MacKeyboard();
        } else {
            keyboardImpl = new LinuxKeyboard();
        }
        System.out.println("keyboardImpl = " + keyboardImpl.getClass().getName());
    }

    /**
     * 根据传入的字符串开始键盘输入
     */
    public static void sendText(String text) {
        keyboardImpl.sendText(text);
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
     * 继续键盘输入
     */
    public static void resume() {
        keyboardImpl.resume();
    }
}
