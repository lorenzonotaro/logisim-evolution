package com.cburch.logisim.gui.lncpu.debugger;

public interface DebuggerListener {
    void debbugerStatusChanged(Status status);

    void breakpointHit(Line breakpoint);
}
