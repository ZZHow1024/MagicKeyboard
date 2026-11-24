package com.zzhow.magickeyboard.util;

/**
 * 文本预处理工具类
 *
 * @author ZZHow
 * create 2026/11/23
 */
public final class TextUtil {

    private TextUtil() {
    }

    /**
     * 去除每一行行首的空格与制表符（代码模式）
     * <p>
     * 保留行内空白、空行与末尾换行结构不变。
     *
     * @param text 待处理的文本
     * @return 处理后的文本，入参为 null 时返回 null
     */
    public static String stripLeadingWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // MULTILINE 模式下 ^ 匹配每一行的行首
        return text.replaceAll("(?m)^[ \t]+", "");
    }
}
