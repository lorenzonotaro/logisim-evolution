package com.cburch.logisim.gui.lncpu;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Mem;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.TextLineNumber;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DebugLncpuWindow implements Simulator.StatusListener {
    private final Project project;
    private final LFrame window;
    private final JTextArea codeArea;
    private File tempDir;

    private File lastProgramOpened;

    public DebugLncpuWindow(Project project) {
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
        debugControlPanel.add(new JButton("Step"));
        debugControlPanel.add(new JButton("Run"));
        north.add(debugControlPanel, BorderLayout.EAST);

        final var openFile = new JButton("Load program...");
        openFile.addActionListener(this::loadProgramPressed);
        north.add(openFile, BorderLayout.WEST);

        //Main panel

        codeArea = new JTextArea();
        codeArea.setFont(new Font("monospaced", Font.PLAIN, AppPreferences.getScaled(10)));
        final var scroller = new JScrollPane(codeArea);
        scroller.setRowHeaderView(new TextLineNumber(codeArea));
        main.add(scroller, BorderLayout.CENTER);

        project.getSimulator().addSimulatorListener(this);
    }

    private void loadProgramPressed(ActionEvent actionEvent) {
        var fc = JFileChoosers.createSelected(getRecent());

        fc.setFileFilter(new FileNameExtensionFilter("lnasm file", "lnasm"));
        fc.setDialogTitle("Load program");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);

        var ret = fc.showOpenDialog(this.window);

        if (ret == JFileChooser.APPROVE_OPTION) {
            var file = fc.getSelectedFile();
            lastProgramOpened = file;
            loadProgram(file);
        }

        if(this.tempDir == null) {
            try {
                tempDir = Files.createTempDirectory("lncpu").toFile();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.window, "An error occurred while creating temporary directory: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            // set to delete temporary directory and its contents
            tempDir.deleteOnExit();
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

            var program = Files.readAllBytes(new File("a.out").toPath());

            var immediateCode = Files.readString(new File("a.immediate.txt").toPath());

            codeArea.setText(immediateCode);
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
}
