package openperipheral.addons.utils;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class GuiUtilsTest {

    @Test
    public void everyGuiElementCanBeToggledAndReadBack() {
        for (GuiElements element : GuiElements.values()) {
            GuiUtils.setGuiElementState(element, false);
            Assert.assertFalse(element.name(), GuiUtils.getGuiElementState(element));
            GuiUtils.setGuiElementState(element, true);
            Assert.assertTrue(element.name(), GuiUtils.getGuiElementState(element));
        }
    }

    @Test
    public void stateCanBeStoredMutatedAndRestored() {
        GuiUtils.setGuiElementState(GuiElements.HOTBAR, true);
        GuiUtils.setGuiElementState(GuiElements.HEALTH, false);

        final Map<GuiElements, Boolean> saved = GuiUtils.storeGuiElementsState();
        Assert.assertEquals(GuiElements.values().length, saved.size());

        GuiUtils.setGuiElementState(GuiElements.HOTBAR, false);
        GuiUtils.setGuiElementState(GuiElements.HEALTH, true);

        GuiUtils.loadGuiElementsState(saved);
        Assert.assertTrue(GuiUtils.getGuiElementState(GuiElements.HOTBAR));
        Assert.assertFalse(GuiUtils.getGuiElementState(GuiElements.HEALTH));
    }

    @Test
    public void nullValuesInStateMapAreIgnored() {
        final Map<GuiElements, Boolean> saved = GuiUtils.storeGuiElementsState();
        saved.put(GuiElements.FOOD, null);
        GuiUtils.loadGuiElementsState(saved);
        Assert.assertNotNull(GuiUtils.getGuiElementState(GuiElements.FOOD));
    }
}
