package com.cburch.logisim.gui.lncpu.debugger;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.lncpu.ComponentEntry;
import com.cburch.logisim.proj.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LncpuDebugger {

    private final Project project;
    private final Map<String, ComponentEntry> componentDirectory;
    private Status status;

    private final List<DebuggerListener> debuggerListeners = new ArrayList<>();

    private Line[] lines;

    private String immediateCode;

    public LncpuDebugger(Project project, Map<String, ComponentEntry> componentDirectory) {
        this.project = project;
        this.componentDirectory = componentDirectory;
        setStatus(Status.UNCONFIGURED);
    }

    public void init(String immediateCode){
        this.immediateCode = immediateCode;
        lines = parseLines(immediateCode);

        project.getSimulator().setAutoPropagation(false);
        project.getSimulator().reset();

        setStatus(Status.PAUSED);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        debuggerListeners.forEach(listener -> listener.debbugerStatusChanged(status));
    }


    public void addStatusChangeListener(DebuggerListener listener){
        debuggerListeners.add(listener);
    }

    public void removeStatusChangeListener(DebuggerListener listener){
        debuggerListeners.remove(listener);
    }

    private static Line[] parseLines(String immediateCode) {
        final var lines = immediateCode.split("\n");
        final var parsedLines = new Line[lines.length];
        for (int i = 0; i < lines.length; i++) {
            var line = lines[i];

            if (line.isBlank() || line.trim().startsWith("#")) {
                continue;
            }

            parsedLines[i] = new Line(i + 1, lines[i]);
        }
        return parsedLines;
    }

    public boolean setBreakpoint(int lineNumber, boolean value){
        if (lines[lineNumber - 1] == null) {
            return false;
        }
        lines[lineNumber - 1].setBreakpoint(value);
        return true;
    }

    public Line getLine(int lineNumber){
        return lines[lineNumber - 1];
    }

    public Line[] getLines() {
        return lines;
    }

    public void tick(Simulator.Event e) {
    }
}
