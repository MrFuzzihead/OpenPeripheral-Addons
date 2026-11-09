package openperipheral.addons;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MetasGenericTest {

    @Before
    public void installGenericItem() {
        if (OpenPeripheralAddons.Items.generic == null) OpenPeripheralAddons.Items.generic = new ItemOPGeneric();
    }

    @Test
    public void createsStacksAndRecognisesItsOwnMeta() {
        final MetasGeneric meta = MetasGeneric.duckAntenna;
        final ItemStack one = meta.newItemStack();
        final ItemStack two = meta.newItemStack(2);

        Assert.assertEquals(meta.ordinal(), one.getItemDamage());
        Assert.assertEquals(1, one.stackSize);
        Assert.assertEquals(2, two.stackSize);
        Assert.assertTrue(meta.isA(one));
        Assert.assertTrue(meta.isA(two));

        Assert.assertFalse(meta.isA(new ItemStack(new Item())));
        Assert.assertFalse(meta.isA(new ItemStack(OpenPeripheralAddons.Items.generic, 1, meta.ordinal() + 1)));
    }
}
