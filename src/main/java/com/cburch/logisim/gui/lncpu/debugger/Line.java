package com.cburch.logisim.gui.lncpu.debugger;

import java.util.Arrays;
import java.util.regex.*;

public class Line {

    private static final Pattern REGEX_MATCHER = Pattern.compile("^\\s*(?<labels>[a-zA-Z0-9_$\\s:]*:)?\\s*(?<address>[0-9a-zA-Z]{6}):\\s*(?<instructionCode>[0-9a-fA-F]{2})\\s*\\((?<instructionName>[a-z0-9_]+)\\)\\s*(?<parameters>[0-9a-fA-F\\s]*)\\s+$");

    private final int lineNumber;
    private final String line;

    private final int instructionAddress;

    private final int instructionCode;

    private final int[] params;

    private final String instruction_name;

    private final String[] labels;

    private boolean breakpoint;

    public Line(int lineNumber, String line) {
        this.lineNumber = lineNumber;
        this.line = line;
        this.breakpoint = false;

        Matcher matcher = REGEX_MATCHER.matcher(line);
        if (matcher.matches()){
            this.instructionAddress = Integer.parseInt(matcher.group("address"), 16);
            this.instructionCode = Integer.parseInt(matcher.group("instructionCode"), 16);
            this.instruction_name = matcher.group("instructionName");
            String parametersGroup = matcher.group("parameters");

            if (parametersGroup == null || parametersGroup.isBlank()) {
                this.params = new int[0];
            }else{
                this.params = Arrays.stream(parametersGroup.split(" ")).filter(s -> !s.isBlank()).map(s -> Integer.parseInt(s, 16)).mapToInt(Integer::intValue).toArray();

            }
            String labelsGroup = matcher.group("labels");
            if (labelsGroup == null || labelsGroup.isBlank()) {
                this.labels = new String[0];
            }else{
                this.labels = Arrays.stream(labelsGroup.split(":")).filter(s -> !s.isBlank()).map(String::trim).toArray(String[]::new);
            }
        }else{
            throw new IllegalArgumentException("Malformed immediate lnasm line: " + line);
        }
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLine() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Line line1 = (Line) o;

        if (lineNumber != line1.lineNumber) return false;
        return line.equals(line1.line);
    }

    @Override
    public int hashCode() {
        int result = lineNumber;
        result = 31 * result + line.hashCode();
        return result;
    }

    public boolean hasBreakpoint() {
        return breakpoint;
    }

    public void setBreakpoint(boolean breakpoint) {
        this.breakpoint = breakpoint;
    }
}
