package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.gui.lncpu.debugger.DebugLncpuWindow;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.std.memory.RamState;

public class RAMValuePassCondition implements IPassCondition{

    private final long address;

    private final long value;

    private long actualValue;

    public RAMValuePassCondition(long address, long value) {
        this.address = address;
        this.value = value;

    }

    @Override
    public boolean test(ComponentDirectory directory) {
        var ramEntry = directory.get(DebugLncpuWindow.RAM_DIRECTORY);
        var ramContents = ((RamState)ramEntry.state.getData(ramEntry.component)).getContents();

        return ((actualValue) = ramContents.get(address)) == value;

    }

    @Override
    public long getActualValue() {
        return actualValue;
    }

    @Override
    public String toString() {
        return "[0x%04x] = 0x%2x".formatted(address, value);
    }
}
