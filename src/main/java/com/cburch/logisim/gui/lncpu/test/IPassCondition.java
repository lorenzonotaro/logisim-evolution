package com.cburch.logisim.gui.lncpu.test;

import com.cburch.logisim.gui.lncpu.util.ComponentDirectory;

public interface IPassCondition {
    boolean test(ComponentDirectory directory);
    long getActualValue();
}
