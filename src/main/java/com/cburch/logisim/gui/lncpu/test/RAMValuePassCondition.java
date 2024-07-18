package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.lncpu.debugger.DebugLncpuWindow;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.gui.lncpu.util.WatchedSignal;
import com.cburch.logisim.std.memory.RamState;

public class RAMValuePassCondition implements IPassCondition{

    private final long address;

    private final long value;

    public RAMValuePassCondition(long address, long value) {
        this.address = address;
        this.value = value;

    }

    @Override
    public boolean test(ComponentDirectory directory, CircuitState state) {
        var signal = directory.get("RAM/RAM");

        var ramEntry = directory.get(DebugLncpuWindow.RAM_DIRECTORY);
        var ramContents = ((RamState)ramEntry.state.getData(ramEntry.component)).getContents();

        return ramContents.get(address) == value;

    }
}
