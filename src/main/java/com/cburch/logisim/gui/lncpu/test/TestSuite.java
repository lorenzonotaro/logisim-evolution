package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;
import com.cburch.logisim.gui.lncpu.util.WatchedSignal;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.MemState;

import java.io.File;
import java.util.ArrayList;

class TestSuite implements Simulator.Listener{

    static final String ROM_DIRECTORY = "ROM/STORAGE_ROM";

    static final String RESET_BUTTON_DIRECTORY = "RESET_BTN";

    private final ComponentDirectory componentDirectory;
    private final Project project;
    private final ILogger logger;

    private final ITestSuiteListener listener;

    private final Test[] tests;

    private int notLoaded, passed, failed, timeout;

    private final ComponentDirectory.Entry notHltEntry, romEntry;

    volatile Status status;

    private final Object lock;

    private volatile boolean stopRequested;

    TestSuite(File directory, ComponentDirectory componentDirectory, Project project, ILogger logger, ITestSuiteListener listener) {
        this.componentDirectory = componentDirectory;
        this.project = project;
        this.logger = logger;
        this.listener = listener;
        this.status = Status.NOT_CONFIGURED;
        this.notHltEntry = componentDirectory.get(WatchedSignal.NOT_HLT.directory);
        this.romEntry = componentDirectory.get(ROM_DIRECTORY);
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
                printf("'%s' not loaded: %s.\n", subDir.getName(), e.getMessage());
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

            // load compiled code into ROM
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

                        if (System.currentTimeMillis() - start >= 2000) {
                            timedOut = true;
                            break;
                        }

                    }
                } catch (InterruptedException e) {
                    printf("Interrupted.\n");
                    break;
                }
            }

            simulator.setAutoTicking(false);
            if (timedOut) {
                this.timeout++;
                printf("TIMEOUT\n");
            }else{
                boolean passed = test.passed(componentDirectory);

                if (passed) {
                    this.passed++;
                    printf("PASSED\n");
                }else{
                    this.failed++;
                    printf("FAILED\n");
                }
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

    @Override
    public void simulatorReset(Simulator.Event e) {

    }

    @Override
    public void simulatorStateChanged(Simulator.Event e) {

    }

    @Override
    public void propagationCompleted(Simulator.Event e) {
        if (WatchedSignal.NOT_HLT.getValue(componentDirectory).toLongValue() == 0){
            setStatus(Status.READY);

            // notify waiting thread
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public enum Status{
        READY, EXECUTING, DONE, NOT_CONFIGURED
    }

}