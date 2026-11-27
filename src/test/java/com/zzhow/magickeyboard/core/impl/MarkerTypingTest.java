package com.zzhow.magickeyboard.core.impl;

import com.zzhow.magickeyboard.core.ControlCenter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 键入主循环的标记调度测试：用记录型假实现穿透 AbstractKeyboard 基类，
 * 验证字面片段与标记按顺序正确分发（全平台可运行，无需真实按键注入）。
 *
 * @author ZZHow
 * create 2026/11/27
 */
class MarkerTypingTest {

    /**
     * 记录所有分发动作用的假实现
     */
    private static final class RecordingKeyboard extends AbstractKeyboard {
        final List<String> calls = new ArrayList<>();

        @Override
        protected void sendCodePoint(int codePoint, ControlCenter.Mode mode) {
            calls.add("cp:" + codePoint);
        }

        @Override
        protected void sendNewLine() {
            calls.add("ENTER");
        }

        @Override
        protected void sendTab() {
            calls.add("TAB");
        }

        @Override
        protected void sendEscapeKey() {
            calls.add("ESC");
        }

        @Override
        protected void sendComboKey(Set<KeyMarkers.Modifier> modifiers, char key) {
            calls.add("COMBO:" + modifiers + ":" + key);
        }
    }

    private long savedInterval;

    @BeforeEach
    void speedUp() {
        savedInterval = ControlCenter.timeInterval;
        ControlCenter.timeInterval = 0;
    }

    @AfterEach
    void restoreInterval() {
        ControlCenter.timeInterval = savedInterval;
    }

    @Test
    void executesMarkersAndLiteralsInOrder() {
        RecordingKeyboard keyboard = new RecordingKeyboard();
        keyboard.sendText("a{ENTER}b{ESC}{CTRL+S}{DELAY=1}x{FOO}", ControlCenter.Mode.COMPATIBLE_MODE);

        assertEquals(List.of(
                "cp:97",                 // a
                "ENTER",
                "cp:98",                 // b
                "ESC",
                "COMBO:[CTRL]:S",
                "cp:120",                // x
                "cp:123", "cp:70", "cp:79", "cp:79", "cp:125" // {FOO} 按原文逐字符
        ), keyboard.calls);
    }

    @Test
    void typesLiteralNewlinesAndTabsAsSpecialKeys() {
        RecordingKeyboard keyboard = new RecordingKeyboard();
        keyboard.sendText("a\nb\tc", ControlCenter.Mode.COMPATIBLE_MODE);

        assertEquals(List.of("cp:97", "ENTER", "cp:98", "TAB", "cp:99"), keyboard.calls);
    }

    @Test
    void passesSupplementaryCodePointsThrough() {
        RecordingKeyboard keyboard = new RecordingKeyboard();
        keyboard.sendText("\uD83D\uDE00{ENTER}", ControlCenter.Mode.RAPID_MODE); // 😀

        assertEquals(List.of("cp:128512", "ENTER"), keyboard.calls);
    }

    @Test
    void handlesTabMarkerSameAsLiteralTab() {
        RecordingKeyboard keyboard = new RecordingKeyboard();
        keyboard.sendText("{TAB}", ControlCenter.Mode.COMPATIBLE_MODE);

        assertEquals(List.of("TAB"), keyboard.calls);
    }
}
