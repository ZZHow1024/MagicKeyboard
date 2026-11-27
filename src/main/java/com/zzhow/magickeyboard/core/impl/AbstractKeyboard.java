package com.zzhow.magickeyboard.core.impl;

import com.zzhow.magickeyboard.core.ControlCenter;
import com.zzhow.magickeyboard.core.IKeyboard;

import java.util.List;

/**
 * 模拟键盘输入抽象基类
 * <p>
 * 以模板方法的形式统一了三个平台实现的公共逻辑：
 * 暂停/停止/恢复状态管理、键入主循环（停止检查、暂停检查、换行/制表符特殊键、
 * 代理对处理、键入间隔），子类只需实现单个字符的发送方式。
 *
 * @author ZZHow
 * create 2026/11/23
 */
public abstract class AbstractKeyboard implements IKeyboard {

    private volatile boolean stopped = false;
    private volatile boolean paused = false;
    private volatile boolean finished = true; // 初始为 true，保证未开始键入时 resume() 直接返回
    private final Object pauseLock = new Object();

    /**
     * 发送一个 Unicode 码点（BMP 字符或补充字符）
     *
     * @param codePoint Unicode 码点
     * @param mode      键入模式
     */
    protected abstract void sendCodePoint(int codePoint, ControlCenter.Mode mode);

    /**
     * 发送换行键
     */
    protected abstract void sendNewLine();

    /**
     * 发送制表键
     */
    protected abstract void sendTab();

    /**
     * 发送 Esc 键
     */
    protected abstract void sendEscapeKey();

    /**
     * 发送修饰键组合（如 Ctrl+S），主键由解析器保证为 A-Z 或 0-9
     *
     * @param modifiers 修饰键集合（至少一个）
     * @param key       主键（A-Z 或 0-9）
     */
    protected abstract void sendComboKey(java.util.Set<KeyMarkers.Modifier> modifiers, char key);

    @Override
    public void sendText(String text, ControlCenter.Mode mode) {
        resetState();
        try {
            List<Object> segments = KeyMarkers.parse(text);
            for (Object segment : segments) {
                // 检查是否停止
                if (stopped) {
                    break;
                }

                // 检查是否暂停
                awaitIfPaused();
                if (stopped) {
                    break;
                }

                if (segment instanceof KeyMarkers.Marker marker) {
                    executeMarker(marker);
                    continue;
                }

                String literal = (String) segment;
                for (int i = 0; i < literal.length(); i++) {
                    // 检查是否停止
                    if (stopped) {
                        break;
                    }

                    // 检查是否暂停
                    awaitIfPaused();
                    if (stopped) {
                        break;
                    }

                    char c = literal.charAt(i);

                    // 处理特殊按键（两种模式都需要）
                    if (c == '\n') {
                        sendNewLine();
                        sleepInterval();
                        continue;
                    } else if (c == '\t') {
                        sendTab();
                        sleepInterval();
                        continue;
                    }

                    // 处理代理对（Surrogate Pairs）：高、低位代理组成完整码点后一次发送
                    if (Character.isHighSurrogate(c) && i + 1 < literal.length()
                            && Character.isLowSurrogate(literal.charAt(i + 1))) {
                        sendCodePoint(Character.toCodePoint(c, literal.charAt(i + 1)), mode);
                        i++; // 跳过低位代理
                    } else {
                        sendCodePoint(c, mode);
                    }

                    sleepInterval();
                }
            }
        } finally {
            // 键入结束，唤醒等待键入完毕的线程（如 resume() 的调用方）
            finished = true;
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }

    /**
     * 执行标记对应的按键动作
     */
    private void executeMarker(KeyMarkers.Marker marker) {
        switch (marker) {
            case KeyMarkers.EnterMarker e -> sendNewLine();
            case KeyMarkers.TabMarker t -> sendTab();
            case KeyMarkers.EscapeMarker e -> sendEscapeKey();
            case KeyMarkers.DelayMarker d -> {
                sleepMillis(d.millis());
                return;
            }
            case KeyMarkers.ComboMarker c -> sendComboKey(c.modifiers(), c.key());
        }
        sleepInterval();
    }

    @Override
    public final void stop() {
        stopped = true;
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    @Override
    public final void pause() {
        paused = true;
    }

    @Override
    public final void resume() {
        paused = false;
        // 唤醒暂停中的键入线程
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        // 阻塞式等待键入完毕
        synchronized (pauseLock) {
            while (!finished) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 重置键入状态（子类在开始新一轮键入前通过 super.sendText() 间接调用）
     */
    private void resetState() {
        stopped = false;
        paused = false;
        finished = false;
    }

    /**
     * 暂停期间阻塞等待，直到恢复或停止
     */
    private void awaitIfPaused() {
        synchronized (pauseLock) {
            while (paused && !stopped) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stopped = true;
                }
            }
        }
    }

    /**
     * 按全局键入间隔延迟，被中断时终止键入
     */
    protected final void sleepInterval() {
        try {
            Thread.sleep(ControlCenter.timeInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopped = true;
        }
    }

    /**
     * 按指定时长延迟（DELAY 标记），分段休眠以便及时响应停止请求
     */
    protected final void sleepMillis(long millis) {
        long remaining = Math.min(Math.max(millis, 0L), KeyMarkers.MAX_DELAY_MS);
        long slept = 0;
        while (!stopped && slept < remaining) {
            long step = Math.min(200L, remaining - slept);
            try {
                Thread.sleep(step);
                slept += step;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopped = true;
            }
        }
    }
}
