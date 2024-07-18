package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.gui.lncpu.util.WatchedSignal;

public class RegisterValuePassCondition implements IPassCondition{

    private final WatchedSignal signal;

    private final long value;

    public RegisterValuePassCondition(WatchedSignal signal, long value) {
        this.signal = signal;
        this.value = value;
    }

    @Override
    public boolean test(ComponentDirectory directory, CircuitState state) {
        return signal.getValue(directory).toLongValue() == value;
    }
}
