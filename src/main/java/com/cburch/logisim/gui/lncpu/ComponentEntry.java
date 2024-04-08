package com.cburch.logisim.gui.lncpu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;

public class ComponentEntry {
    public Component component;

    public CircuitState state;

    ComponentEntry(Component component, CircuitState state) {
        this.component = component;
        this.state = state;
    }
}
