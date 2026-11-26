package com.zzhow.magickeyboard.core.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Linux 键盘布局查找表单元测试
 *
 * @author ZZHow
 * create 2026/11/26
 */
class LinuxKeyLayoutTest {

    /**
     * 构造每键码 2 列的测试布局，row[0] 对应键码 9
     */
    private static LinuxKeyLayout layout2(long[][] rows) {
        long[] flat = new long[rows.length * 2];
        for (int i = 0; i < rows.length; i++) {
            flat[i * 2] = rows[i][0];
            flat[i * 2 + 1] = rows[i][1];
        }
        return new LinuxKeyLayout(9, 9 + rows.length - 1, 2, flat);
    }

    @Test
    void findsUnshiftedAndShiftedKeysymsOnUsLayout() {
        LinuxKeyLayout layout = layout2(new long[][]{
                {0, 0},      // kc9 空闲
                {'1', '!'},  // kc10
                {'2', '@'},  // kc11
                {'q', 'Q'},  // kc12
                {'a', 'A'},  // kc13
        });

        assertEquals(new LinuxKeyLayout.KeyBinding(13, false), layout.find('a'));
        assertEquals(new LinuxKeyLayout.KeyBinding(13, true), layout.find('A'));
        assertEquals(new LinuxKeyLayout.KeyBinding(10, false), layout.find('1'));
        assertEquals(new LinuxKeyLayout.KeyBinding(10, true), layout.find('!'));
        assertEquals(new LinuxKeyLayout.KeyBinding(12, true), layout.find('Q'));
    }

    @Test
    void resolvesShiftRequirementFromActualLayoutOnAzerty() {
        // 法语 AZERTY 片段：数字行未按列是符号，数字在按列
        LinuxKeyLayout layout = layout2(new long[][]{
                {'&', '1'},  // kc10（QWERTY-1 位置）
                {'é', '2'},  // kc11
                {'a', 'A'},  // kc12（QWERTY-q 位置）
                {'q', 'Q'},  // kc13（QWERTY-a 位置）
        });

        // 美式布局表会断言数字不需要 Shift，实际布局下需要 —— 布局自适应的核心验证点
        assertEquals(new LinuxKeyLayout.KeyBinding(9, true), layout.find('1'));
        assertEquals(new LinuxKeyLayout.KeyBinding(9, false), layout.find('&'));
        assertEquals(new LinuxKeyLayout.KeyBinding(11, false), layout.find('a'));
        assertEquals(new LinuxKeyLayout.KeyBinding(12, false), layout.find('q'));
    }

    @Test
    void ignoresModeSwitchColumns() {
        // 每键码 3 列：第 3 列为 Mode_switch 层，不应参与查找
        long[] flat = {'3', '#', '~'};
        LinuxKeyLayout layout = new LinuxKeyLayout(9, 9, 3, flat);

        assertEquals(new LinuxKeyLayout.KeyBinding(9, true), layout.find('#'));
        assertNull(layout.find('~'));
    }

    @Test
    void returnsNullForMissingKeysymAndNoSymbol() {
        LinuxKeyLayout layout = layout2(new long[][]{
                {'1', '!'},
        });

        assertNull(layout.find('中'));
        assertNull(layout.find(0x01000000L + 0x4E2D));
        assertNull(layout.find(LinuxKeyLayout.NO_SYMBOL));
    }

    @Test
    void findsFreeKeycodesExcludingOccupiedOnes() {
        LinuxKeyLayout layout = layout2(new long[][]{
                {0, 0},      // kc9 空闲
                {'1', '!'},  // kc10
                {0, 0},      // kc11 空闲
        });

        assertEquals(9, layout.findFreeKeycode(Set.of()));
        assertEquals(11, layout.findFreeKeycode(Set.of(9)));
        assertEquals(0, layout.findFreeKeycode(Set.of(9, 11)));
    }

    @Test
    void returnsRowCopyForRestore() {
        LinuxKeyLayout layout = layout2(new long[][]{
                {'1', '!'},
                {0, 0},
        });

        assertArrayEquals(new long[]{'1', '!'}, layout.row(9));
        assertArrayEquals(new long[]{0, 0}, layout.row(10));

        // 修改返回的副本不得影响布局表
        long[] row = layout.row(9);
        row[0] = 0;
        assertArrayEquals(new long[]{'1', '!'}, layout.row(9));
    }
}
