package com.cburch.logisim.gui.lncpu.util;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

import java.util.HashMap;
import java.util.Map;

public class ComponentDirectory extends HashMap<String, ComponentDirectory.Entry> {
    public static class Entry {
        public Component component;

        public CircuitState state;

        Entry(Component component, CircuitState state) {
            this.component = component;
            this.state = state;
        }
    }

    private ComponentDirectory() {
    }

    public static ComponentDirectory makeComponentDirectory(Project project){
        var circuit = project.getCurrentCircuit();
        var circuitState = project.getCircuitState();
        return buildComponentDirectory(new ComponentDirectory(), circuit, circuitState, "");
    }

    private static ComponentDirectory buildComponentDirectory(ComponentDirectory directory, Circuit circuit, CircuitState circuitState, String baseDir) {
        for(Component comp : circuit.getNonWires()){
            String label = comp.getAttributeSet().getValue(StdAttr.LABEL);
            String thisComp = (label == null || label.length() == 0) ? comp.getFactory().getDisplayName() : label;
            String thisName = baseDir + thisComp;
            directory.put(thisName, new Entry(comp, circuitState));
            if (comp.getFactory() instanceof SubcircuitFactory subcircuitFactory){
                buildComponentDirectory(directory, subcircuitFactory.getSubcircuit(),subcircuitFactory.getSubstate(circuitState, comp), thisName + "/");
            }
        }
        return directory;
    }
}
