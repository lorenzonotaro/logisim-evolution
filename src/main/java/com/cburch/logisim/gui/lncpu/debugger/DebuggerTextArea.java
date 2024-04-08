package com.cburch.logisim.gui.lncpu.debugger;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;

public class DebuggerTextArea extends JTextArea {
    private int highlightedLineNumber = -1;

    public int getHighlightedLineNumber() {
        return highlightedLineNumber;
    }

    public Color getHitLineColor() {
        return hitLineColor;
    }

    public void setHitLineColor(Color hitLineColor) {
        this.hitLineColor = hitLineColor;
    }

    private Color hitLineColor = Color.RED;

    public DebuggerTextArea() {
        super();
    }

    public DebuggerTextArea(int rows, int columns) {
        super(rows, columns);
    }

    public void setHighlightedLineNumber(int lineNumber) {
        this.highlightedLineNumber = lineNumber;
        highlightLine();
    }

    private void highlightLine() {
        if (highlightedLineNumber < 0) {
            return;
        }
        try {
            int startOffset = getLineStartOffset(highlightedLineNumber - 1);
            int endOffset = getLineEndOffset(highlightedLineNumber - 1);

            if (startOffset >= 0 && endOffset >= 0) {
                    getHighlighter().removeAllHighlights();
                    Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(hitLineColor);
                    getHighlighter().addHighlight(startOffset, endOffset, painter);

            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        setHighlightedLineNumber(-1);
    }
}
