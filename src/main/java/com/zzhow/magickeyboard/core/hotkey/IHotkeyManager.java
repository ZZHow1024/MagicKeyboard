package com.zzhow.magickeyboard.core.hotkey;

/**
 * 全局热键管理器接口
 * <p>
 * 回调在热键监听线程上触发，不保证处于 JavaFX 线程，
 * 需要操作 UI 的调用方自行用 {@code Platform.runLater} 包装。
 *
 * @author ZZHow
 * create 2026/11/27
 */
public interface IHotkeyManager {

    /**
     * 注册全局热键
     *
     * @param hotkey   热键定义
     * @param callback 热键触发回调（在监听线程上执行）
     * @return 注册是否成功（被系统占用等失败时返回 false）
     */
    boolean register(Hotkey hotkey, Runnable callback);

    /**
     * 注销全部热键并停止监听（幂等）
     */
    void shutdown();
}
