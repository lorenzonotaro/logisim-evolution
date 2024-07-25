package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.gui.lncpu.util.WatchedSignal;

public class RegisterValuePassCondition implements IPassCondition{

    private final WatchedSignal signal;

    private final long value;
    private long actualValue;

    public RegisterValuePassCondition(WatchedSignal signal, long value) {
        this.signal = signal;
        this.value = value;
    }

    @Override
    public boolean test() {
        return ((actualValue) = signal.getValue().toLongValue()) == value;
    }

    @Override
    public long getActualValue() {
        return actualValue;
    }

    @Override
    public String toString() {
        return "%s = 0x%02x".formatted(signal.displayName, value);
    }
}
