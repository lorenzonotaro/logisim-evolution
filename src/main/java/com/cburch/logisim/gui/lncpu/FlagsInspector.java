package com.cburch.logisim.gui.lncpu;

import javax.swing.*;
import java.awt.*;

class FlagsInspector extends JPanel {
    private final FlagIndicator[] flags = new FlagIndicator[4];

    public FlagsInspector() {
        this.setLayout(new GridLayout(flags.length, 1));
        this.setBorder(BorderFactory.createTitledBorder("Flags"));

        String[] FLAGS_LABELS = {"I (interrupt)", "Z (zero)", "N (negative)", "C (carry)"};

        for (int i = 0; i < flags.length; i++) {
            flags[i] = new FlagIndicator();
            flags[i].setStatus(false);
            this.add(wrapperPanel(flags[i], FLAGS_LABELS[i]));
        }
    }

    // Create a panel so that the label takes up the whole width
    private static JPanel wrapperPanel(FlagIndicator flag, String flagsLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(flagsLabel), BorderLayout.WEST);
        panel.add(flag, BorderLayout.EAST);
        return panel;
    }

    void update(long newValues) {
        for (int i = flags.length - 1; i >= 0; i--) {
            flags[i].setStatus(((newValues >> (flags.length - i - 1) & 0x1) == 1));
        }
    }


    // A simple circle with a color, indicating the flag status
    private static class FlagIndicator extends JComponent {
        private boolean status;

        private static final Color ON_COLOR = Color.BLUE;
        private static final Color OFF_COLOR = Color.GRAY;
        private static final Color BORDER_COLOR = Color.BLACK;

        private static final int SIZE = 20;

        public FlagIndicator() {
            this.status = false;
            this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(status ? ON_COLOR : OFF_COLOR);
            g.fillOval(0, 0, SIZE, SIZE);
            g.setColor(BORDER_COLOR);
            g.drawOval(0, 0, SIZE, SIZE);
        }

        public Dimension getPreferredSize() {
            return new Dimension(SIZE, SIZE);
        }

        public void setStatus(boolean status) {
            this.status = status;
            repaint();
        }
    }
}
