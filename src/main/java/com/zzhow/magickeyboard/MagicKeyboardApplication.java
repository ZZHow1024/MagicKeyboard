package com.zzhow.magickeyboard;

import com.zzhow.magickeyboard.window.MainWindow;

/**
 * MagicKeyboard 程序入口类
 *
 * @author ZZHow
 * create 2025/10/12
 * update 2026/11/23
 */
public class MagicKeyboardApplication {

    /**
     * 应用版本号（发布时需与 pom.xml 的 version 保持一致）
     */
    public static final String VERSION = "2.0.0";

    public static void main(String[] args) {
        MainWindow.show();
    }
}
