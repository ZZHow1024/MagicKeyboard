package com.zzhow.magickeyboard.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * TextUtil 单元测试
 *
 * @author ZZHow
 * create 2026/11/23
 */
class TextUtilTest {

    @Test
    void stripsLeadingSpacesAndTabsOnEveryLine() {
        assertEquals("int main() {\nreturn 0;\n}", TextUtil.stripLeadingWhitespace("    int main() {\n        return 0;\n    }"));
        assertEquals("hello\tworld", TextUtil.stripLeadingWhitespace("  hello\tworld"));
        assertEquals("hello", TextUtil.stripLeadingWhitespace("\t\thello"));
    }

    @Test
    void preservesInnerWhitespace() {
        assertEquals("a  b\tc", TextUtil.stripLeadingWhitespace("a  b\tc"));
        assertEquals("foo  bar", TextUtil.stripLeadingWhitespace("    foo  bar"));
    }

    @Test
    void preservesEmptyLinesAndTrailingNewlines() {
        assertEquals("a\n\nb\n", TextUtil.stripLeadingWhitespace("  a\n\n  b\n"));
        assertEquals("a\n\n\n", TextUtil.stripLeadingWhitespace("\ta\n\n\n"));
    }

    @Test
    void handlesCrLfLineEndings() {
        assertEquals("a\r\nb", TextUtil.stripLeadingWhitespace("a\r\n    b"));
    }

    @Test
    void handlesChineseText() {
        assertEquals("你好\n世界", TextUtil.stripLeadingWhitespace("    你好\n\t世界"));
    }

    @Test
    void returnsEmptyStringForEmptyInput() {
        assertEquals("", TextUtil.stripLeadingWhitespace(""));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(TextUtil.stripLeadingWhitespace(null));
    }

    @Test
    void returnsSameInstanceForNullInput() {
        String value = null;
        assertSame(value, TextUtil.stripLeadingWhitespace(value));
    }
}
