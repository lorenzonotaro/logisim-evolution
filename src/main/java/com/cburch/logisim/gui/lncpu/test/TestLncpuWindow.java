package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.JFileChoosers;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class TestLncpuWindow implements ILogger, ITestSuiteListener {

    private final Project project;

    private final JButton runButton, stopButton, exportTestData;

    private final ComponentDirectory componentDirectory;

    private JTextArea outputArea;

    private LFrame window;

    private File recent;



    private Object lock;

    private TestSuite suite;

    public TestLncpuWindow(Project project) {
        this.project = project;
        this.componentDirectory = ComponentDirectory.makeComponentDirectory(project);

        window = new LFrame.SubWindow(null);
        window.setTitle("Test Lncpu");
        window.setMinimumSize(new Dimension(800, 600));

        JPanel contentPane = new JPanel(new BorderLayout());

        JPanel northLeftPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.runButton = new JButton("Run tests...");
        this.runButton.addActionListener(this::run);
        this.stopButton = new JButton("Stop");
        this.stopButton.addActionListener(this::stop);
        this.stopButton.setEnabled(false);

        northLeftPane.add(runButton);
        northLeftPane.add(stopButton);


        JPanel northPane = new JPanel(new BorderLayout());


        JPanel northRightPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        this.exportTestData = new JButton("Export test data...");
        this.exportTestData.setEnabled(false);
        this.exportTestData.addActionListener(e -> {
            if (suite != null) {
                var fc = JFileChoosers.createAt(recent);

                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setDialogTitle("Select file to save test data");
                fc.setApproveButtonText("Save");
                fc.setFileFilter(new FileNameExtensionFilter("TXT File", "txt"));

                if (fc.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    suite.exportTestTXT(fc.getSelectedFile());
                }
            }
        });

        northRightPane.add(exportTestData);

        northPane.add(northLeftPane, BorderLayout.WEST);
        northPane.add(northRightPane, BorderLayout.EAST);
        contentPane.add(northPane, BorderLayout.NORTH);

        this.outputArea = new JTextArea();
        this.outputArea.setEditable(false);
        outputArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        outputArea.setForeground(Color.WHITE);
        outputArea.setBackground(Color.BLACK);

        final var caret = (DefaultCaret) outputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        contentPane.add(new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        window.setContentPane(contentPane);
    }

    private void stop(ActionEvent actionEvent) {
        if (suite != null) {
            suite.stop();
        }
    }

    private void run(ActionEvent __) {
        outputArea.setText("");

        // Select folder containing tests
        var fc = JFileChoosers.createAt(recent == null ? project.getLogisimFile().getLoader().getMainFile().getParentFile() : recent);

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select folder containing tests");
        fc.setApproveButtonText("Select");

        if (fc.showOpenDialog(window) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        recent = fc.getSelectedFile();

        if (recent == null || recent.isFile()) {
            return;
        }

        var suiteThread = new Thread(() -> {
            TestSuite suite = new TestSuite(recent, componentDirectory, project, this, this);

            if(suite.status == TestSuite.Status.NOT_CONFIGURED) {
                return;
            }

            this.suite = suite;

            suite.run();
        });

        suiteThread.setName("Test Suite Thread");

        suiteThread.start();
    }

    void printf(String running, Object... args) {
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

    @Override
    public void log(String format, Object... args) {
        SwingUtilities.invokeLater(() -> printf(format, args));
    }

    @Override
    public void onTestSuiteStatusChange(TestSuite.Status status) {
        if(status == TestSuite.Status.EXECUTING) {
            runButton.setEnabled(false);
            stopButton.setEnabled(true);
            exportTestData.setEnabled(false);
        }else if (status == TestSuite.Status.DONE) {
            runButton.setEnabled(true);
            stopButton.setEnabled(false);
            exportTestData.setEnabled(true);

            suite.printSummary();
        }
    }
}
