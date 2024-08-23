package com.cburch.logisim.gui.lncpu.util;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceDataSingleton;

public class LncpuUtils {
    public static void setResetButton(boolean b) {
        var value = b ? Value.TRUE : Value.FALSE;
        var resetButton = ComponentDirectory.getEntry(ComponentDirectory.RESET_BUTTON_DIRECTORY);
        var state = resetButton.state.getInstanceState(resetButton.component);

        final var data = (InstanceDataSingleton) state.getData();
        if (data == null) {
            state.setData(new InstanceDataSingleton(value));
        } else {
            data.setValue(value);
        }
        state.getInstance().fireInvalidated();
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
