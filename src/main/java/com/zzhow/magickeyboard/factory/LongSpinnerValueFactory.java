package com.zzhow.magickeyboard.factory;

import javafx.scene.control.SpinnerValueFactory;

/**
 * 长整型微调值工厂类
 *
 * @author ZZHow
 * create 2025/10/14
 * update 2025/10/14
 */
public class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {
    private final long min;
    private final long max;
    private final long step;

    public LongSpinnerValueFactory(long min, long max, long initialValue, long step) {
        this.min = min;
        this.max = max;
        this.step = step;
        setValue(initialValue);
    }

    @Override
    public void decrement(int steps) {
        long newValue = getValue() - steps * step;
        if (newValue < min) newValue = min;
        setValue(newValue);
    }

    @Override
    public void increment(int steps) {
        long newValue = getValue() + steps * step;
        if (newValue > max) newValue = max;
        setValue(newValue);
    }
}
