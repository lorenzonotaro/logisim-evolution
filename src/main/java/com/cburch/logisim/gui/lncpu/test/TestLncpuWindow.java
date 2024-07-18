package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.gui.lncpu.util.WatchedSignal;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.MemState;
import com.cburch.logisim.util.JFileChoosers;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestLncpuWindow{

    static final String ROM_DIRECTORY = "ROM/STORAGE_ROM";

    static final String RESET_BUTTON_DIRECTORY = "RESET_BTN";

    private final Project project;
    private final JButton runButton, stopButton;

    private final ComponentDirectory componentDirectory;

    private JTextArea outputArea;

    private LFrame window;

    private File recent;

    private File tempDir;

    private volatile boolean stopRequested = false, simulationRunning = false;

    private final ComponentDirectory.Entry notHltEntry, romEntry;

    private Object lock;

    public TestLncpuWindow(Project project) {
        this.project = project;
        this.componentDirectory = ComponentDirectory.makeComponentDirectory(project);
        this.notHltEntry = componentDirectory.get(WatchedSignal.NOT_HLT.directory);
        this.romEntry = componentDirectory.get(ROM_DIRECTORY);
        window = new LFrame.SubWindow(null);
        window.setTitle("Test Lncpu");
        window.setMinimumSize(new Dimension(400, 300));

        JPanel contentPane = new JPanel(new BorderLayout());

        JPanel northPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.runButton = new JButton("Run tests...");
        this.runButton.addActionListener(this::run);
        this.stopButton = new JButton("Stop");
        this.stopButton.addActionListener(this::stop);
        this.stopButton.setEnabled(false);

        northPane.add(runButton);
        northPane.add(stopButton);
        contentPane.add(northPane, BorderLayout.NORTH);

        this.outputArea = new JTextArea();
        this.outputArea.setEditable(false);
        contentPane.add(new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        window = new LFrame.SubWindow(null);
        window.setTitle("Test Lncpu");
        window.setMinimumSize(new Dimension(400, 300));


        window.setContentPane(contentPane);
    }

    private void stop(ActionEvent __) {
        this.stopRequested = true;
    }

    private void run(ActionEvent __) {
        outputArea.setText("");

        outputArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        outputArea.setForeground(Color.WHITE);
        outputArea.setBackground(Color.BLACK);
        final var caret = (DefaultCaret) outputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        this.stopRequested = false;

        // Select folder containing tests
        var fc = JFileChoosers.createAt(recent == null ? project.getLogisimFile().getLoader().getMainFile().getParentFile() : recent);

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select folder containing tests");
        fc.setApproveButtonText("Select");

        if (fc.showOpenDialog(window) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        runButton.setEnabled(false);
        stopButton.setEnabled(true);

        recent = fc.getSelectedFile();

        runTests(recent, project);

        runButton.setEnabled(true);
        stopButton.setEnabled(false);

        this.stopRequested = false;
    }

    private void runTests(File directory, Project project) {
        printf("Parsing tests...\n");

        var tests = new ArrayList<>();

        var linkerFile = new File(directory, "linker.cfg");

        if(!linkerFile.exists()) {
            printf("linker.cfg not found.\n");
            return;
        }

        var testsInDir = directory.listFiles(File::isDirectory);

        if (testsInDir == null) {
            printf("No tests found.\n");
            return;
        }

        // for each subdirectory
        for (var subDir : testsInDir) {
            try{
                var test = new Test(subDir, linkerFile);
                tests.add(test);
            }catch (Test.TestParseException e){
                printf("'%s' not loaded: ", subDir.getName(), e.getMessage());
            }
        }
    }

    private void setResetButton(boolean b) {
        var value = b ? Value.TRUE : Value.FALSE;
        var resetButton = componentDirectory.get(RESET_BUTTON_DIRECTORY);
        var state = resetButton.state.getInstanceState(resetButton.component);

        final var data = (InstanceDataSingleton) state.getData();
        if (data == null) {
            state.setData(new InstanceDataSingleton(value));
        } else {
            data.setValue(value);
        }
        state.getInstance().fireInvalidated();
    }

    private void printf(String running, Object... args) {
        outputArea.append(String.format(running, args));
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

}
