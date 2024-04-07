package com.cburch.logisim.gui.lncpu;

import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Mem;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.RamState;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

public class Inspector extends JPanel {
    private final Project project;
    private final Map<String, ComponentEntry> componentsDirectory;

    private final JTextField ra, rb, rc, rd;
    private final JTextField ss, sp, cspc, ir;
    private final FlagsInspector flags;
    private final StackInspector stack;
    public Inspector(Project project, Map<String, ComponentEntry> componentDirectory){
        this.project = project;
        this.componentsDirectory = componentDirectory;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        var registersPanel = new JPanel(new GridLayout(2,2));
        registersPanel.setBorder(new TitledBorder("Registers"));
        registersPanel.add(labelPanel(ra = createTextField(), "RA"));
        registersPanel.add(labelPanel(rb = createTextField(), "RB"));
        registersPanel.add(labelPanel(rc = createTextField(), "RC"));
        registersPanel.add(labelPanel(rd = createTextField(), "RD"));

        var specialRegistersPanel = new JPanel(new GridLayout(2,2));
        specialRegistersPanel.setBorder(new TitledBorder("Special registers"));
        specialRegistersPanel.add(labelPanel(ss = createTextField(), "SS"));
        specialRegistersPanel.add(labelPanel(sp = createTextField(), "SP"));
        specialRegistersPanel.add(labelPanel(cspc = createTextField(), "CS:PC"));
        specialRegistersPanel.add(labelPanel(ir = createTextField(), "IR"));

        this.add(wrap(registersPanel));
        this.add(wrap(specialRegistersPanel));

        this.add(wrap(flags = new FlagsInspector()));

        this.add(wrapBorderLayout(stack = new StackInspector()));

        JPanel editMemoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton inspectRAM = new JButton("Inspect RAM...");
        inspectRAM.addActionListener(e -> {
            var ramEntry = componentsDirectory.get(DebugLncpuWindow.RAM_DIRECTORY);
            var ramFactory = (Mem) componentsDirectory.get(DebugLncpuWindow.RAM_DIRECTORY).component.getFactory();
            HexFrame frame = ramFactory.getHexFrame(project, ramEntry.state.getInstanceState(ramEntry.component).getInstance(), ramEntry.state);
            frame.setVisible(true);
            frame.toFront();
        });
        editMemoryPanel.add(inspectRAM);

        JButton inspectROM = new JButton("Inspect ROM...");
        inspectROM.addActionListener(e -> {
            var romEntry = componentsDirectory.get(DebugLncpuWindow.ROM_DIRECTORY);
            var romFactory = (Mem) componentsDirectory.get(DebugLncpuWindow.ROM_DIRECTORY).component.getFactory();
            HexFrame frame = romFactory.getHexFrame(project, romEntry.state.getInstanceState(romEntry.component).getInstance(), romEntry.state);
            frame.setVisible(true);
            frame.toFront();
        });
        editMemoryPanel.add(inspectROM);

        this.add(editMemoryPanel);
    }

    private Component wrapBorderLayout(JComponent element) {
        var wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(element, BorderLayout.CENTER);
        var prefSize = wrapperPanel.getPreferredSize();
        wrapperPanel.setMaximumSize(prefSize);
        return wrapperPanel;
    }

    private static JPanel wrap(JPanel specialRegistersPanel) {
        var wrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapperPanel.add(specialRegistersPanel);
        var prefSize = wrapperPanel.getPreferredSize();
        wrapperPanel.setMaximumSize(prefSize);
        return wrapperPanel;
    }

    private static GridBagConstraints gbc(int x, int y, int spanw, int spanh){
        var gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = spanw;
        gbc.gridheight = spanh;
        return gbc;
    }

    static JPanel labelPanel(JComponent textField, String label) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(textField, BorderLayout.CENTER);
        return panel;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("monospaced", Font.PLAIN, AppPreferences.getScaled(8)));
        textField.setEditable(false);
        return textField;
    }


    void update(){
        setValue(ra, componentsDirectory, WatchedSignal.RA);
        setValue(rb, componentsDirectory, WatchedSignal.RB);
        setValue(rc, componentsDirectory, WatchedSignal.RC);
        setValue(rd, componentsDirectory, WatchedSignal.RD);

        setValue(ss, componentsDirectory, WatchedSignal.SS);
        setValue(sp, componentsDirectory, WatchedSignal.SP);
        setValue(ir, componentsDirectory, WatchedSignal.IR);
        setValue(cspc, componentsDirectory, WatchedSignal.CS_PC);

        var flagsEntry = componentsDirectory.get(WatchedSignal.FLAGS.directory);
        flags.update(WatchedSignal.FLAGS.valueGetter.apply(flagsEntry.component, flagsEntry.state).toLongValue());

        var ssEntry = componentsDirectory.get(WatchedSignal.SS.directory);
        var spEntry = componentsDirectory.get(WatchedSignal.SP.directory);
        var ramEntry = componentsDirectory.get(DebugLncpuWindow.RAM_DIRECTORY);

        var ramContents = ((RamState)ramEntry.state.getData(ramEntry.component)).getContents();
        stack.update(ramContents, WatchedSignal.SS.valueGetter.apply(ssEntry.component, ssEntry.state).toLongValue(),
                WatchedSignal.SP.valueGetter.apply(spEntry.component, spEntry.state).toLongValue());
    }

    private void setValue(JTextField textField, Map<String, ComponentEntry> componentsDirectory, WatchedSignal signal) {
        var entry = componentsDirectory.get(signal.directory);
        long value = signal.valueGetter.apply(entry.component, entry.state).toLongValue();
        String formatted = null;
        if(signal.displayBinary){
            formatted = String.format("%" + signal.bits + "s", Integer.toBinaryString((int) value)).replace(' ', '0');
        }else{
            formatted = String.format("%0" + (signal.bits / 4) + "x", value);
        }
        textField.setText(formatted);
    }
}
