package com.zzhow.magickeyboard.config;

/**
 * 偏好存储抽象（便于单元测试注入内存实现）
 *
 * @author ZZHow
 * create 2026/11/27
 */
public interface PreferenceStore {

    /**
     * 读取字符串值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 存储的值，不存在时返回默认值
     */
    String get(String key, String defaultValue);

    /**
     * 写入字符串值
     *
     * @param key   键
     * @param value 值
     */
    void put(String key, String value);
}
