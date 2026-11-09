package openperipheral.addons;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SmokeTestHookTest {

    private String previous;

    @Before
    public void clearProperty() {
        previous = System.getProperty(SmokeTestHook.ENABLE_PROPERTY);
        System.clearProperty(SmokeTestHook.ENABLE_PROPERTY);
    }

    @After
    public void restoreProperty() {
        if (previous != null) System.setProperty(SmokeTestHook.ENABLE_PROPERTY, previous);
        else System.clearProperty(SmokeTestHook.ENABLE_PROPERTY);
    }

    @Test
    public void hookIsInertWithoutTheProperty() {
        SmokeTestHook.scheduleExitIfRequested();
        Assert.assertFalse(SmokeTestHook.isScheduled());
    }
}
