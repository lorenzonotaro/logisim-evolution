package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.gui.lncpu.util.WatchedSignal;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.MemState;

import java.io.File;
import java.util.*;

class TestSuite implements Simulator.Listener{

    static final String ROM_DIRECTORY = "ROM/STORAGE_ROM";

    static final String RESET_BUTTON_DIRECTORY = "RESET_BTN";

    private final Project project;
    private final ILogger logger;

    private final ITestSuiteListener listener;

    private final Test[] tests;

    private int notLoaded, passed, failed, timeout;

    volatile Status status;

    private final Object lock;

    private Set<Test.Result> testResults;

    private volatile boolean stopRequested;

    TestSuite(File directory, Project project, ILogger logger, ITestSuiteListener listener) {

        testResults = new HashSet<>();

        this.project = project;
        this.logger = logger;
        this.listener = listener;
        this.status = Status.NOT_CONFIGURED;
        this.lock = new Object();

        printf("Parsing tests...\n");

        var tests = new ArrayList<Test>();

        var linkerFile = new File(directory, "linker.cfg");

        if(!linkerFile.exists()) {
            printf("linker.cfg not found.\n");
            this.tests = new Test[0];
            return;
        }

        var testsInDir = directory.listFiles(File::isDirectory);

        if (testsInDir == null) {
            printf("No tests found.\n");
            this.tests = new Test[0];
            return;
        }

        // for each subdirectory
        for (var subDir : testsInDir) {
            try{
                var test = new Test(subDir, linkerFile);
                tests.add(test);
            }catch (Test.TestParseException e){
                testResults.add(new Test.Result(subDir.getName(), e.status, e.getMessage()));
                printf("'%s' not loaded: %s.\n", subDir.getName(), e.status == Test.ResultType.DOES_NOT_COMPILE ? "does not compile" : (e.status == Test.ResultType.PASS_CONDITIONS_FORMAT_ERROR ? "pass conditions format error" : "unknown error"));
                notLoaded++;
            }
        }

        this.tests = tests.toArray(new Test[0]);

        printf("Loaded %d test(s).\n", tests.size());

        setStatus(Status.READY);
    }

    private void printf(String format, Object... args){
        logger.log(format, args);
    }

    void run() {
        var simulator = project.getSimulator();

        simulator.setAutoPropagation(true);
        simulator.setAutoTicking(false);
        simulator.setTickFrequency(4096);
        simulator.addSimulatorListener(this);

        for (var test : tests) {
            if (stopRequested) {
                printf("Stopping.\n");
                break;
            }

            printf("Running '%s'...", test.immediateName);

            // reset simulator
            simulator.reset();

            sleep(200); //TODO: find another way of ensuring the CircuitState is updated other than waiting...

            // load compiled code into ROM
            var romEntry = ComponentDirectory.getEntry(ROM_DIRECTORY);
            var memState = (MemState) romEntry.state.getData(romEntry.component);

            memState.getContents().clear();
            memState.getContents().set(0, test.compiledCode);

            setResetButton(true);

            sleep(50);

            setResetButton(false);

            simulator.setAutoTicking(true);

            setStatus(Status.EXECUTING);


            boolean timedOut = false;

            // wait on lock
            synchronized (lock) {
                try {
                    long start = System.currentTimeMillis();
                    while(status == Status.EXECUTING && !stopRequested) {
                        lock.wait(2000);

                        if (System.currentTimeMillis() - start >= 2000 && status == Status.EXECUTING) {
                            timedOut = true;
                            break;
                        }

                    }
                } catch (InterruptedException e) {
                    testResults.add(new Test.Result(test.immediateName, Test.ResultType.ERROR, "Interrupted"));
                    printf("Interrupted.\n");
                    break;
                }
            }

            simulator.setAutoTicking(false);
            if (timedOut) {
                this.timeout++;
                this.testResults.add(new Test.Result(test.immediateName, Test.ResultType.TIMEOUT, "Timeout"));
                printf("TIMEOUT\n");
            }else{
                var result = test.passed();

                if (result.type() == Test.ResultType.PASSED) {
                    this.passed++;
                    printf("PASSED\n");
                }else{
                    this.failed++;
                    printf("FAILED\n");
                }

                testResults.add(result);
            }

        }

        simulator.removeSimulatorListener(this);


        printf("Done.\n");

        setStatus(Status.DONE);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    void printSummary() {
        printf("TOTAL: %d\n", tests.length + notLoaded);
        printf("Not loaded: %d\n", notLoaded);
        printf("Passed: %d\n", passed);
        printf("Failed: %d\n", failed);
        printf("Timeout: %d\n", timeout);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        listener.onTestSuiteStatusChange(status);
    }

    public void stop() {
        this.stopRequested = true;
    }

    private void setResetButton(boolean b) {
        var value = b ? Value.TRUE : Value.FALSE;
        var resetButton = ComponentDirectory.getEntry(RESET_BUTTON_DIRECTORY);
        var state = resetButton.state.getInstanceState(resetButton.component);

        final var data = (InstanceDataSingleton) state.getData();
        if (data == null) {
            state.setData(new InstanceDataSingleton(value));
        } else {
            data.setValue(value);
        }
        state.getInstance().fireInvalidated();
    }

    @Override
    public void simulatorReset(Simulator.Event e) {

    }

    @Override
    public void simulatorStateChanged(Simulator.Event e) {

    }

    @Override
    public void propagationCompleted(Simulator.Event e) {
        if (WatchedSignal.NOT_HLT.getValue().toLongValue() == 0){
            setStatus(Status.READY);

            // notify waiting thread
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public void exportTestTXT(File file) {
        if (!file.getName().endsWith(".txt")) {
            file = new File(file.getAbsolutePath() + ".txt");
        }

        try (var writer = new java.io.PrintWriter(file)) {
            var results = testResults.stream().sorted(Comparator.comparingInt(o -> o.type().ordinal())).toList();
            for (var result : results) {
                writer.println("======TEST======" + result.immediateName() + ": " + result.type());
                if (result.message() != null) {
                    writer.println("Message:");
                    writer.println(result.message());
                }
            }
        } catch (java.io.IOException e) {
            printf("Error exporting test results: %s\n", e.getMessage());
        }
    }

    public enum Status{
        READY, EXECUTING, DONE, NOT_CONFIGURED
    }

}
