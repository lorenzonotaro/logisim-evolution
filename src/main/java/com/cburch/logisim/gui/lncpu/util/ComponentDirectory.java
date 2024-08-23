package com.cburch.logisim.gui.lncpu.util;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentDirectory extends HashMap<String, ComponentDirectory.Entry> {

    public static final String ROM_DIRECTORY = "ROM/STORAGE_ROM";
    public static final String RESET_BUTTON_DIRECTORY = "RESET_BTN";
    private static ComponentDirectory instance;

    private final Project project;

    private ComponentDirectory(Project project) {
        super();
        this.project = project;
        this.project.getSimulator().addSimulatorListener(new Simulator.StatusListener() {
            @Override
            public void simulatorReset(Simulator.Event e) {
                rebuildComponentDirectory();
            }

            @Override
            public void simulatorStateChanged(Simulator.Event e) {
            }
        });
        rebuildComponentDirectory();
    }

    public static void init(Project project){
        if (instance == null)
            instance = new ComponentDirectory(project);
        else instance.rebuildComponentDirectory();
    }

    private ComponentDirectory() {
        throw new UnsupportedOperationException();
    }

    private void rebuildComponentDirectory(){
        synchronized (this){
            var circuit = project.getCurrentCircuit();
            var circuitState = project.getSimulator().getCircuitState();

            this.clear();
            buildComponentDirectory(this, circuit, circuitState, "");
        }
    }

    private static void buildComponentDirectory(ComponentDirectory directory, Circuit circuit, CircuitState circuitState, String baseDir) {
        for(Component comp : circuit.getNonWires()){
            String label = comp.getAttributeSet().getValue(StdAttr.LABEL);
            String thisComp = (label == null || label.isEmpty()) ? comp.getFactory().getDisplayName() : label;
            String thisName = baseDir + thisComp;
            directory.put(thisName, new Entry(comp, circuitState));
            if (comp.getFactory() instanceof SubcircuitFactory subcircuitFactory){
                buildComponentDirectory(directory, subcircuitFactory.getSubcircuit(),subcircuitFactory.getSubstate(circuitState, comp), thisName + "/");
            }
        }
    }

    public static Entry getEntry(String name){
        if (instance == null)
            throw new IllegalStateException("ComponentDirectory not initialized");
        return instance.get(name);
    }

    @Override
    public Entry get(Object key) {
        synchronized (this){
            return super.get(key);
        }
    }
    public static class Entry {
        public Component component;

        public CircuitState state;

        Entry(Component component, CircuitState state) {
            this.component = component;
            this.state = state;
        }
    }
}
