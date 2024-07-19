package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.gui.lncpu.util.WatchedSignal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Test {

    private static File tempDir = null;

    final String immediateName;

    final long[] compiledCode;

    final IPassCondition[] passConditions;

    Test(File folder, File linkerFile) throws TestParseException{
        this.immediateName = folder.getName();

        File testCode = new File(folder, "test.lnasm");
        File passValues = new File(folder, "pass.txt");

        if(!testCode.exists()) {
            throw new TestParseException("test.lnasm not found", ResultType.ERROR);
        }

        if(!passValues.exists()) {
            throw new TestParseException("pass.txt not found", ResultType.ERROR);
        }

        if(!linkerFile.exists()) {
            throw new TestParseException("linker.cfg not found", ResultType.ERROR);
        }

        this.compiledCode = parseCompiledCode(testCode, linkerFile);
        this.passConditions = parsePassConditions(passValues);
    }

    private IPassCondition[] parsePassConditions(File passValues) throws TestParseException {
        try {
            var lines = Files.readAllLines(passValues.toPath());

            List<IPassCondition> passConditions = new ArrayList<>();

            for(var line : lines){
                // create regex to match register/[mem address] = value (prefixed with 0x or 0b or nothing)
                var regex = "([a-zA-Z]+|\\[0x[0-9a-fA-F]+\\]|\\[0b[01]+\\])\\s*=\\s*(0x[0-9a-fA-F]+|0b[01]+|[0-9]+)";
                var match = line.matches(regex);

                if(!match) {
                    throw new TestParseException("invalid pass condition: " + line, ResultType.PASS_CONDITIONS_FORMAT_ERROR);
                }

                var parts = line.split("=");

                var signal = parts[0].trim();
                var value = parts[1].trim();

                if(signal.startsWith("[")) {
                    var addressStr = signal.substring(1, signal.indexOf("]"));
                    var radix = addressStr.startsWith("0x") ? 16 : addressStr.startsWith("0b") ? 2 : 10;

                    if (radix != 10){
                        addressStr = addressStr.substring(2);
                    }

                    var address = Long.parseLong(addressStr, radix);

                    if ((address > 0xff && address < 0x2000) || address > 0x3fff){
                        throw new TestParseException("invalid memory address: 0x%04x".formatted(address), ResultType.PASS_CONDITIONS_FORMAT_ERROR);
                    }

                    radix = value.startsWith("0x") ? 16 : value.startsWith("0b") ? 2 : 10;

                    if (radix != 10){
                        value = value.substring(2);
                    }

                    var expectedValue = Long.parseLong(value, 16);

                    passConditions.add(new RAMValuePassCondition(address <= 0xFF ? address : address - 0x2000, expectedValue));
                } else {
                    WatchedSignal watchedSignal;
                    try{
                        watchedSignal = WatchedSignal.valueOf(signal.toUpperCase());
                    }catch(IllegalArgumentException e){
                        throw new TestParseException("invalid signal name (%s)".formatted(signal), ResultType.PASS_CONDITIONS_FORMAT_ERROR);
                    }

                    var radix = value.startsWith("0x") ? 16 : value.startsWith("0b") ? 2 : 10;

                    if (radix != 10){
                        value = value.substring(2);
                    }

                    var expectedValue = Long.parseLong(value, radix);

                    passConditions.add(new RegisterValuePassCondition(watchedSignal, expectedValue));
                }
            }

            return passConditions.toArray(new IPassCondition[0]);
        } catch (IOException e) {
            throw new TestParseException("unable to read pass conditions", ResultType.ERROR);
        } catch (IllegalArgumentException e) {
            throw new TestParseException("invalid signal name: " + e.getMessage(), ResultType.PASS_CONDITIONS_FORMAT_ERROR);
        }
    }

    private long[] parseCompiledCode(File testCode, File linkerFile) throws TestParseException {
        ensureTempDir();

        String[] command;

        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            command = new String[]{"cmd", "/C", "lnasm", "\"" + testCode.getAbsolutePath() +"\"", "-oB", "a.out", "-lf", linkerFile.getAbsolutePath()};
        } else {
            command = new String[]{"bash", "-c", "lnasm", "\"" + testCode.getAbsolutePath() + "\"", "-oB", "a.out", "-lf", linkerFile.getAbsolutePath()};
        }


        try{

            // get output stderr and stdout from process
            var cmd = new ProcessBuilder(command);
            cmd.directory(tempDir);
            var process = cmd.start();
            process.waitFor();

            String error = new String(process.getErrorStream().readAllBytes());

            if(process.exitValue() != 0) {
                throw new TestParseException(error, ResultType.DOES_NOT_COMPILE);
            }

            return Test.toLongArray(Files.readAllBytes(new File(tempDir, "a.out").toPath()));


        }catch(IOException | InterruptedException e) {
            throw new TestParseException("error while compiling code: " + e.getMessage(), ResultType.ERROR);
        }
    }

    private static long[] toLongArray(byte[] bytes) {
        long[] longs = new long[bytes.length];

        for(int i = 0; i < bytes.length; i++) {
            longs[i] = bytes[i] & 0xff;
        }

        return longs;

    }

    private void ensureTempDir() {
        if(tempDir == null) {
            try {
                tempDir = Files.createTempDirectory("lncpu").toFile();
            } catch (IOException e) {
                throw new IllegalStateException("An error occurred while creating temporary directory: " + e.getMessage());
            }
            // set to delete temporary directory and its contents
            tempDir.deleteOnExit();
        }
    }

    Test.Result passed(ComponentDirectory componentDirectory) {

        boolean pass = true;

        StringBuilder message = new StringBuilder("Failed conditions:\n");

        for (var passCondition : passConditions) {
            if(!passCondition.test(componentDirectory)) {
                pass = false;
                message.append(passCondition).append(" (actual: 0x%02x)\n".formatted(passCondition.getActualValue()));
            }
        }
        return new Test.Result(immediateName, pass ? ResultType.PASSED : ResultType.FAILED, pass ? "" : message.toString());
    }

    static class TestParseException extends Exception {

        public final ResultType status;

        public TestParseException(String message, ResultType status) {
            super(message);
            this.status = status;
        }
    }

    record Result(String immediateName, ResultType type, String message) {
        public Result(String immediateName, ResultType type) {
            this(immediateName, type, null);
        }

    }

    enum ResultType {
        DOES_NOT_COMPILE("Does not compile"),
        PASS_CONDITIONS_FORMAT_ERROR("Pass conditions format error"),
        ERROR("Execution error"),

        TIMEOUT("Timeout"),

        PASSED("Passed"),

        FAILED("Failed");

        private final String message;

        ResultType(String message){
            this.message = message;
        }


        @Override
        public String toString(){
            return message;
        }
    }


}
