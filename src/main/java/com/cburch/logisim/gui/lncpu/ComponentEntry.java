package com.cburch.logisim.gui.lncpu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;

class ComponentEntry {
    Component component;

    CircuitState state;

    ComponentEntry(Component component, CircuitState state) {
        this.component = component;
        this.state = state;
    }
}
