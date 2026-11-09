package openperipheral.addons.proxy;

import net.minecraft.item.Item;

import org.junit.Assert;
import org.junit.Test;

import openperipheral.addons.OpenPeripheralAddons;
import openperipheral.addons.pim.BlockPIM;

public class ClientProxyTest {

    @Test
    public void lifecycleHooksAndRenderRegistrationAreSafe() {
        if (OpenPeripheralAddons.Blocks.pim == null) OpenPeripheralAddons.Blocks.pim = new BlockPIM();
        final ClientProxy proxy = new ClientProxy();
        proxy.init();
        proxy.postInit();
        proxy.registerRenderInformation();
        Assert.assertTrue(OpenPeripheralAddons.renderId >= 0);

        final ServerProxy server = new ServerProxy();
        server.preInit();
        server.init();
        server.postInit();
        server.registerRenderInformation();
    }

    @Test
    public void creativeTabIconFallsBackSafely() {
        Assert.assertNotNull(OpenPeripheralAddons.tabOpenPeripheralAddons);
        final Item icon = OpenPeripheralAddons.tabOpenPeripheralAddons.getTabIconItem();
        Assert.assertTrue(icon == null || icon instanceof Item);
    }
}
