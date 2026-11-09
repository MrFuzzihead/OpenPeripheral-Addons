package openperipheral.addons.selector;

import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.InventoryPlayer;

import org.junit.Assert;
import org.junit.Test;

public class GuiSelectorTest {

    @Test
    public void selectorGuiBuildsItsPanel() {
        final TileEntitySelector selector = new TileEntitySelector();
        final ContainerSelector container = new ContainerSelector(mock(InventoryPlayer.class), selector);
        final GuiSelector gui = new GuiSelector(container);
        Assert.assertNotNull(gui.createRoot());
    }
}
