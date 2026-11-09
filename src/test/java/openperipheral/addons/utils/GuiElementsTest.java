package openperipheral.addons.utils;

import org.junit.Assert;
import org.junit.Test;

public class GuiElementsTest {

    @Test
    public void enumContainsEveryVanillaElementWeExpose() {
        Assert.assertEquals(13, GuiElements.values().length);
        Assert.assertNotNull(GuiElements.valueOf("OVERLAY"));
        Assert.assertNotNull(GuiElements.valueOf("OBJECTIVES"));
    }
}
