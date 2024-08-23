package com.cburch.logisim.gui.lncpu.debugger;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.gui.lncpu.util.LncpuUtils;
import com.cburch.logisim.gui.lncpu.util.WatchedSignal;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.proj.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cburch.logisim.gui.lncpu.util.LncpuUtils.sleep;

public class LncpuDebugger {

    private final Project project;

    private Status status;

    private final List<DebuggerListener> debuggerListeners = new ArrayList<>();

    private Line[] lines;

    // Maps addresses to lines
    private Map<Long, Line> codeMap;

    private String immediateCode;

    private Line currentLine;
    private Line stepOverTarget;

    public LncpuDebugger(Project project) {

        this.project = project;

        setStatus(Status.UNCONFIGURED);

        this.codeMap = new HashMap<>();
    }

    public void init(String immediateCode){
        this.immediateCode = immediateCode;
        lines = parseLines(immediateCode);

        reset();
    }

    public void reset() {
        project.getSimulator().setAutoPropagation(true);
        project.getSimulator().setAutoTicking(false);
        project.getSimulator().setTickFrequency(2048);
        project.getSimulator().reset();

        sleep(200); // TODO: find a better way to wait for the simulator to reset

        LncpuUtils.setResetButton(true);

        sleep(50);

        LncpuUtils.setResetButton(false);

        setStatus(Status.READY);
    }


    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        debuggerListeners.forEach(listener -> listener.debbugerStatusChanged(status));
    }


    public void addDebuggerListener(DebuggerListener listener){
        debuggerListeners.add(listener);
    }

    public void removeDebuggerListener(DebuggerListener listener){
        debuggerListeners.remove(listener);
    }

    private Line[] parseLines(String immediateCode) {
        final var lines = immediateCode.split("\n");
        final var parsedLines = new Line[lines.length];
        for (int i = 0; i < lines.length; i++) {
            var line = lines[i];

            if (line.isBlank() || line.trim().startsWith("#")) {
                continue;
            }

            parsedLines[i] = new Line(i + 1, lines[i]);

            codeMap.put(parsedLines[i].getAddress().longValue(), parsedLines[i]);

        }
        return parsedLines;
    }

    public Line getLine(int lineNumber){
        if(lineNumber > lines.length)
            return null;
        return lines[lineNumber - 1];
    }

    public Line[] getLines() {
        return lines;
    }

    public void tick(Simulator.Event e) {
        var lastLine = currentLine;
        if (checkSyncronized()){
            if(status == Status.STEPPING_INTO && lastLine != currentLine){
                setStatus(Status.PAUSED);
                fireLineHit(currentLine);
            }else if(status == Status.STEPPING_OVER && lastLine != currentLine && currentLine == stepOverTarget){
                setStatus(Status.PAUSED);
                fireLineHit(currentLine);
            }else if(status == Status.RUNNING && currentLine.hasBreakpoint() && lastLine != currentLine){
                setStatus(Status.PAUSED);
                fireLineHit(currentLine);
            }else if(status == Status.PAUSED){
                project.getSimulator().setAutoTicking(false);
                fireLineHit(currentLine);
            }
        }
    }

    private void fireLineHit(Line currentLine) {
        debuggerListeners.forEach(listener -> listener.lineHit(currentLine));
    }

    // We are syncronized if the CSPC contains an address that's +1 from a valid instruction, and IR contains the instruction code (fetch complete)
    private boolean checkSyncronized() {
        final var cspc = WatchedSignal.CS_PC.getValue().toLongValue();
        final var ir = WatchedSignal.IR.getValue().toLongValue();

        final var line = codeMap.get(cspc - 1);

        if (line != null && line.getInstructionCode() == ir){
            this.currentLine = line;
            return true;
        }

        return false;
    }

    public void stepInto(){
        requireConfigured();
        setStatus(Status.STEPPING_INTO);
        project.getSimulator().setAutoTicking(true);
    }

    public void stepOver(){
        requireConfigured();

        if(currentLine == null)
            return;

        if(currentLine.getInstructionName().startsWith("lcall")){
            stepOverTarget = getNextLine();
            setStatus(Status.STEPPING_OVER);
            project.getSimulator().setAutoTicking(true);
        }else{
            stepInto();
        }

    }

    private Line getNextLine() {
        if(currentLine == null)
            return null;
        for (int i = currentLine.getLineNumber() + 1; i < lines.length + 1; i++) {
            Line line = lines[i - 1];
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    public void run(){
        requireConfigured();
        setStatus(Status.RUNNING);
        project.getSimulator().setAutoTicking(true);
    }

    public void pause(){
        requireConfigured();
        setStatus(Status.PAUSED);
    }

    public void requireConfigured(){
        if(status == Status.UNCONFIGURED){
            throw new IllegalStateException("Debugger is not configured");
        }
    }
}
