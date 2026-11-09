package openperipheral.addons.sensors;

import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import openperipheral.addons.MetasGeneric;
import openperipheral.addons.OpenPeripheralAddons;
import openperipheral.addons.narcissistic.TurtleUpgradeNarcissistic;

public class TurtleUpgradeTest {

    @Before
    public void installItems() {
        if (OpenPeripheralAddons.Blocks.sensor == null) OpenPeripheralAddons.Blocks.sensor = new BlockSensor();
        if (OpenPeripheralAddons.Items.generic == null)
            OpenPeripheralAddons.Items.generic = new openperipheral.addons.ItemOPGeneric();
    }

    @Test
    public void sensorUpgradeExposesItsIdentityAndCraftingItem() {
        final TurtleUpgradeSensor upgrade = new TurtleUpgradeSensor();
        Assert.assertEquals(180, upgrade.getUpgradeID());
        Assert.assertEquals("openperipheral.turtle.sensor.adjective", upgrade.getUnlocalisedAdjective());
        Assert.assertEquals(dan200.computercraft.api.turtle.TurtleUpgradeType.Peripheral, upgrade.getType());
        final ItemStack stack = upgrade.getCraftingItem();
        Assert.assertNotNull(stack);
        Assert.assertNull(upgrade.useTool(null, null, null, 0));
        upgrade.update(null, null);
    }

    @Test
    public void narcissisticUpgradeUsesTheDuckAntenna() {
        final TurtleUpgradeNarcissistic upgrade = new TurtleUpgradeNarcissistic();
        Assert.assertEquals(181, upgrade.getUpgradeID());
        Assert.assertEquals("openperipheral.turtle.narcissistic.adjective", upgrade.getUnlocalisedAdjective());
        Assert.assertEquals(dan200.computercraft.api.turtle.TurtleUpgradeType.Peripheral, upgrade.getType());
        final ItemStack stack = upgrade.getCraftingItem();
        Assert.assertNotNull(stack);
        Assert.assertTrue(MetasGeneric.duckAntenna.isA(stack));
        Assert.assertNull(upgrade.useTool(null, null, null, 0));
        upgrade.update(null, null);
    }
}
