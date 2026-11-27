package com.zzhow.magickeyboard.core.hotkey.impl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;
import com.zzhow.magickeyboard.core.hotkey.Hotkey;
import com.zzhow.magickeyboard.core.hotkey.IHotkeyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 全局热键管理器-Windows 实现类
 * <p>
 * 基于 RegisterHotKey + 独立消息泵线程：注册任务投递到泵线程执行
 * （RegisterHotKey 绑定调用线程），WM_HOTKEY 消息分发回调。
 *
 * @author ZZHow
 * create 2026/11/27
 */
public class WindowsHotkeyManager implements IHotkeyManager {

    private static final Logger LOGGER = Logger.getLogger(WindowsHotkeyManager.class.getName());

    private static final int WM_HOTKEY = 0x0312;
    private static final int WM_QUIT = 0x0012;
    private static final int WM_APP_WAKE = 0x8000; // WM_APP：唤醒消息泵处理注册任务

    private interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        int GetMessage(WinUser.MSG msg, HWND hWnd, int min, int max);

        boolean PostThreadMessage(int threadId, int msg, WPARAM wParam, LPARAM lParam);
    }

    private interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        int GetCurrentThreadId();
    }

    private static final class Registration {
        final Hotkey hotkey;
        final Runnable callback;

        Registration(Hotkey hotkey, Runnable callback) {
            this.hotkey = hotkey;
            this.callback = callback;
        }
    }

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final Map<Integer, Runnable> callbacks = new HashMap<>();
    private volatile boolean running = true;
    private volatile int pumpThreadId = 0;

    public WindowsHotkeyManager() {
        Thread pump = new Thread(this::pumpLoop, "magic-keyboard-hotkey");
        pump.setDaemon(true);
        pump.start();
    }

    @Override
    public boolean register(Hotkey hotkey, Runnable callback) {
        if (!running) {
            return false;
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        tasks.add(() -> {
            boolean ok = com.sun.jna.platform.win32.User32.INSTANCE.RegisterHotKey(null, hotkey.id(), hotkey.modifierMask(), hotkey.keyCode());
            if (ok) {
                callbacks.put(hotkey.id(), callback);
            } else {
                LOGGER.warning(() -> "Failed to register global hotkey id=" + hotkey.id()
                        + " (possibly occupied by another application)");
            }
            result.complete(ok);
        });
        wakePump();

        try {
            return result.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (TimeoutException | java.util.concurrent.ExecutionException e) {
            LOGGER.log(Level.WARNING, "Timed out waiting for hotkey registration", e);
            return false;
        }
    }

    @Override
    public void shutdown() {
        running = false;
        wakePump();
        // 发送 WM_QUIT 结束消息泵；泵线程退出前注销全部热键
        int threadId = pumpThreadId;
        if (threadId != 0) {
            User32.INSTANCE.PostThreadMessage(threadId, WM_QUIT, new WPARAM(0), new LPARAM(0));
        }
    }

    private void wakePump() {
        int threadId = pumpThreadId;
        if (threadId != 0) {
            User32.INSTANCE.PostThreadMessage(threadId, WM_APP_WAKE, new WPARAM(0), new LPARAM(0));
        }
    }

    private void pumpLoop() {
        pumpThreadId = Kernel32.INSTANCE.GetCurrentThreadId();
        // 执行启动期间排队的注册任务
        drainTasks();

        WinUser.MSG msg = new WinUser.MSG();
        while (running) {
            int result = User32.INSTANCE.GetMessage(msg, null, 0, 0);
            if (result <= 0) {
                break; // WM_QUIT 或错误
            }

            if (msg.message == WM_HOTKEY) {
                dispatch((int) msg.wParam.longValue());
            } else if (msg.message == WM_APP_WAKE) {
                drainTasks();
            }
        }

        unregisterAll();
    }

    private void drainTasks() {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
        }
    }

    private void dispatch(int id) {
        Runnable callback = callbacks.get(id);
        if (callback != null) {
            callback.run();
        }
    }

    private void unregisterAll() {
        for (Integer id : callbacks.keySet().toArray(new Integer[0])) {
            com.sun.jna.platform.win32.User32.INSTANCE.UnregisterHotKey(null, id);
        }
        callbacks.clear();
    }
}
