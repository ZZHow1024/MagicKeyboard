package com.zzhow.magickeyboard.core;

/**
 * 模拟键盘输入接口
 *
 * @author ZZHow
 * @date 2025/10/13
 */
public interface IKeyboard {
    /**
     * 根据传入的字符串开始键盘输入
     *
     * @param text 待键入的字符串
     */
    void sendText(String text);

    /**
     * 停止键盘输入
     */
    void stop();

    /**
     * 暂停键盘输入
     */
    void pause();

    /**
     * 继续键盘输入
     */
    void resume();
}
