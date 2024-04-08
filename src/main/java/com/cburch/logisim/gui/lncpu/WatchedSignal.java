package com.cburch.logisim.gui.lncpu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.std.memory.MemContents;
import com.cburch.logisim.std.memory.RegisterData;
import com.cburch.logisim.std.wiring.Pin;

import java.util.function.BiFunction;

public enum WatchedSignal {
    RA("RA/ASYNC_OUT", "RA", 8, false, WatchedSignal::pinValueGetter),
    RB("RB/ASYNC_OUT", "RB", 8, false, WatchedSignal::pinValueGetter),
    RC("RC/ASYNC_OUT", "RC", 8, false, WatchedSignal::pinValueGetter),
    RD("RD/ASYNC_OUT", "RD", 8, false, WatchedSignal::pinValueGetter),

    CS_PC("CSPC/CS_PC", "CS:PC", 16, false, WatchedSignal::pinValueGetter),

    SS("SS", "SS", 8, false, WatchedSignal::registerValueGetter),
    SP("SP/SP_OUT", "SP", 8, false, WatchedSignal::pinValueGetter),

    IR("ControlUnit/IR", "IR", 8, false, WatchedSignal::registerValueGetter),

    FLAGS("FLAGS/FLAGS_OUT", "FLAGS", 4, true, WatchedSignal::pinValueGetter);
    private static Value registerValueGetter(Component component, CircuitState circuitState) {
        return ((RegisterData) circuitState.getData(component)).getValue();
    }

    public final String directory;
    public final String displayName;

    public final int bits;

    public final boolean displayBinary;
    public final BiFunction<Component, CircuitState, Value> valueGetter;

    WatchedSignal(String directory, String displayName, int bits, boolean displayBinary, BiFunction<Component, CircuitState, Value> valueGetter) {
        this.directory = directory;
        this.displayName = displayName;
        this.bits = bits;
        this.displayBinary = displayBinary;
        this.valueGetter = valueGetter;
    }

    public String getDirectory() {
        return directory;
    }

    private static Value pinValueGetter(Component component, CircuitState state){
        return ((Pin) component.getFactory()).getValue(state.getInstanceState(component));
    }
}
