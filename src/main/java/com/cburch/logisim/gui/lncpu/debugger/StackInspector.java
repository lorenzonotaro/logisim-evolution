package com.cburch.logisim.gui.lncpu.debugger;

import com.cburch.logisim.std.memory.MemContents;
import com.cburch.logisim.util.TextLineNumber;

import javax.swing.*;
import java.awt.*;

public class StackInspector extends JPanel {

    private final JTextArea stack;

    public StackInspector() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createTitledBorder("Stack"));

        stack = new JTextArea();
        stack.setEditable(false);
        stack.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        stack.setLineWrap(true);
        stack.setMinimumSize(new Dimension(200, 400));

        JScrollPane scrollPane = new JScrollPane(stack);
        this.add(scrollPane);
    }

    public void update(MemContents memory, long ss, long sp) {

        long address = ((ss << 8) + sp) & 0xFFFF;

        if (address < 0x2000 || address >= 0x4000) {
            stack.setText("Stack outside of RAM bounds\n");
            return;
        }

        // Create a string with lines in the format: 0000: 00 from ss:00 to ss:(sp - 1)
        StringBuilder stackString = new StringBuilder();
        for (int i = 0; i < sp; i++) {
            address = (ss << 8) + i;
            stackString.append(String.format("%04X:\t%02X\n", address, memory.get(address & 0x1fff)));
        }
        stack.setText(stackString.toString());

        // scroll to the end of the stack
        stack.setCaretPosition(stack.getDocument().getLength());
    }
}
