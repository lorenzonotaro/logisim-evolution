package com.cburch.logisim.gui.lncpu;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.lncpu.debugger.*;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.MemState;
import com.cburch.logisim.util.JFileChoosers;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class DebugLncpuWindow implements Simulator.Listener, DebuggerListener {

    private final JButton stepOverBtn, stepIntoBtn, pauseResumeBtn;

    private final Project project;
    private final LFrame window;
    private final DebuggerTextArea codeArea;
    private final Inspector inspector;
    private File tempDir;

    private final Map<String, ComponentEntry> componentDirectory;

    private File lastProgramOpened;

    static final String ROM_DIRECTORY = "ROM/STORAGE_ROM";

    static final String RAM_DIRECTORY = "RAM/RAM";

    static final String TEXT_STEP_OVER = "Step over (F8)";
    static final String TEXT_STEP_INTO = "Step into (F7)";
    static final String TEXT_PAUSE = "Pause (F5)";
    static final String TEXT_RESUME = "Resume (F5)";


    private LncpuDebugger debugger;

    public DebugLncpuWindow(Project project) {

        componentDirectory = buildComponentDirectory(new HashMap<>(), project.getCurrentCircuit(), project.getCircuitState(),"");

        project.getSimulator().addSimulatorListener(DebugLncpuWindow.this);

        this.debugger = new LncpuDebugger(project, componentDirectory);
        this.debugger.addDebuggerListener(this);

        this.project = project;
        this.window = new LFrame.SubWindow(null);

        this.window.setTitle("Debug lncpu");
        this.window.setMinimumSize(new Dimension(1024, 768));

        final var main = new JPanel(new BorderLayout());
        final var north = new JPanel(new BorderLayout());

        this.window.add(main, BorderLayout.CENTER);
        this.window.add(north, BorderLayout.NORTH);

        // North panel
        final var debugControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        debugControlPanel.add(stepOverBtn = new JButton(TEXT_STEP_OVER));
        debugControlPanel.add(stepIntoBtn = new JButton(TEXT_STEP_INTO));
        debugControlPanel.add(pauseResumeBtn = new JButton(TEXT_PAUSE));
        north.add(debugControlPanel, BorderLayout.EAST);
        final var openFile = new JButton("Load program...");
        openFile.addActionListener(this::loadProgramPressed);
        north.add(openFile, BorderLayout.WEST);

        //Main panel

        codeArea = new DebuggerTextArea();
        codeArea.setFont(new Font("monospaced", Font.PLAIN, AppPreferences.getScaled(8)));
        codeArea.setEditable(false);

        final var scroller = new JScrollPane(codeArea);
        scroller.setRowHeaderView(new DebuggerLineNumber(codeArea, debugger));
        main.add(scroller, BorderLayout.CENTER);

        inspector = new Inspector(project, componentDirectory);
        main.add(inspector, BorderLayout.EAST);

        // set buttons to an unconfigured state
        this.debbugerStatusChanged(Status.UNCONFIGURED);

        stepOverBtn.addActionListener(this::stepOverPressed);

        stepIntoBtn.addActionListener(this::stepIntoPressed);

        pauseResumeBtn.addActionListener(this::pauseResumePressed);


        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "stepOver");
        window.getRootPane().getActionMap().put("stepOver", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stepOverPressed(e);
            }
        });

        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "stepInto");
        window.getRootPane().getActionMap().put("stepInto", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stepIntoPressed(e);
            }
        });

        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "pauseResume");
        window.getRootPane().getActionMap().put("pauseResume", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseResumePressed(e);
            }
        });
    }

    private void pauseResumePressed(ActionEvent e) {
        if(debugger.getStatus() == Status.PAUSED){
            debugger.run();
            codeArea.setHighlightedLineNumber(-1);
        } else {
            debugger.pause();
        }
    }

    private void stepIntoPressed(ActionEvent e) {
        if(debugger.getStatus() == Status.PAUSED){
            // TODO
        }
    }

    private void stepOverPressed(ActionEvent e) {
        if(debugger.getStatus() == Status.PAUSED){
            debugger.step();
            codeArea.setHighlightedLineNumber(-1);
        }
    }

    private Map<String, ComponentEntry> buildComponentDirectory(Map<String, ComponentEntry> directory, Circuit circuit, CircuitState circuitState, String baseDir) {
        for(Component comp : circuit.getNonWires()){
            String label = comp.getAttributeSet().getValue(StdAttr.LABEL);
            String thisComp = (label == null || label.length() == 0) ? comp.getFactory().getDisplayName() : label;
            String thisName = baseDir + thisComp;
            directory.put(thisName, new ComponentEntry(comp, circuitState));
            if (comp.getFactory() instanceof SubcircuitFactory subcircuitFactory){
                buildComponentDirectory(directory, subcircuitFactory.getSubcircuit(),subcircuitFactory.getSubstate(circuitState, comp), thisName + "/");
            }
        }
        return directory;
    }

    private void loadProgramPressed(ActionEvent actionEvent) {
        var fc = JFileChoosers.createSelected(getRecent());

        fc.setFileFilter(new FileNameExtensionFilter("lnasm file", "lnasm"));
        fc.setDialogTitle("Load program");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);

        var ret = fc.showOpenDialog(this.window);

        if(this.tempDir == null) {
            try {
                tempDir = Files.createTempDirectory("lncpu").toFile();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.window, "An error occurred while creating temporary directory: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            // set to delete temporary directory and its contents
            tempDir.deleteOnExit();
        }

        if (ret == JFileChooser.APPROVE_OPTION) {
            var file = fc.getSelectedFile();
            lastProgramOpened = file;
            loadProgram(file);
        }
    }

    private File getRecent() {
        if (lastProgramOpened == null) {
            final var lf = (project == null ? null : project.getLogisimFile());
            final var ld = (lf == null ? null : lf.getLoader());
            return (ld == null ? new File(System.getProperty("user.home")) : ld.getCurrentDirectory());
        }
        return lastProgramOpened;
    }


    private void loadProgram(File file) {
        try {
            // exec cmd lnasm on the given file to generate binary and immediate code
            var cmd = new ProcessBuilder("cmd", "/C", "lnasm", file.getAbsolutePath(), "-o", "a.out", "-f", "binary");
            cmd.directory(tempDir);
            var process = cmd.start();
            process.waitFor();

            if(process.exitValue() != 0) {
                JOptionPane.showMessageDialog(this.window, String.format("Compilation failed for file '%s': \n%s", file.getName(), process.getErrorStream()), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            cmd = new ProcessBuilder("cmd", "/C", "lnasm", file.getAbsolutePath(), "-o", "a.immediate.txt", "-f", "immediate");
            cmd.directory(tempDir);
            process = cmd.start();
            process.waitFor();

            if(process.exitValue() != 0) {
                JOptionPane.showMessageDialog(this.window, String.format("Immediate code generation failed for file '%s': \n%s", file.getName(), process.getErrorStream()), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // load the program into the ROM
            var rom = componentDirectory.get(ROM_DIRECTORY);
            if(rom == null) {
                JOptionPane.showMessageDialog(this.window, "ROM not found in the circuit", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            var memState = (MemState) rom.state.getData(rom.component);
            HexFile.open(memState.getContents(), new File(tempDir, "a.out"), "Binary big-endian");

            var immediateCode = Files.readString(new File(tempDir,"a.immediate.txt").toPath());

            codeArea.setText(immediateCode);

            try{
                debugger.init(immediateCode);
            } catch(IllegalArgumentException e){
                JOptionPane.showMessageDialog(this.window, "An error occurred while parsing the immediate code: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | InterruptedException e) {
            JOptionPane.showMessageDialog(this.window,  "An error occurred while loading the program: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }



    public boolean isVisible() {
        if (window != null) {
            return window.isVisible();
        } else {
            return false;
        }
    }

    public void setVisible(boolean bool) {
        window.setVisible(bool);
    }

    public void toFront() {
        if(window != null)
            window.toFront();
    }

    @Override
    public void simulatorReset(Simulator.Event e) {

    }

    @Override
    public void simulatorStateChanged(Simulator.Event e) {

    }


    @Override
    public void propagationCompleted(Simulator.Event e) {
        if(e.didTick()){
            inspector.update();
            window.repaint();
            debugger.tick(e);
        }
    }


    public void debbugerStatusChanged(Status status) {
        if(status == Status.UNCONFIGURED){
            stepIntoBtn.setEnabled(false);
            stepOverBtn.setEnabled(false);
            pauseResumeBtn.setEnabled(false);
        } else {
            stepIntoBtn.setEnabled(true);
            stepOverBtn.setEnabled(true);
            pauseResumeBtn.setEnabled(true);
        }

        switch (status) {
            case RUNNING:
            case STEPPING:
                pauseResumeBtn.setText(TEXT_PAUSE);
                break;
            case PAUSED:
                pauseResumeBtn.setText(TEXT_RESUME);
                break;
        }
    }

    @Override
    public void lineHit(Line line) {
        codeArea.setHighlightedLineNumber(line.getLineNumber());
    }
}
