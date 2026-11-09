package openperipheral.addons.narcissistic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dan200.computercraft.api.turtle.ITurtleAccess;

public class TurtleInventoryDelegateTest {

    private ITurtleAccess turtle;
    private IInventory inventory;
    private World world;
    private TurtleInventoryDelegate delegate;

    @Before
    public void setup() {
        turtle = mock(ITurtleAccess.class);
        inventory = mock(IInventory.class);
        world = mock(World.class);
        when(turtle.getWorld()).thenReturn(world);
        when(turtle.getInventory()).thenReturn(inventory);
        when(turtle.getPosition()).thenReturn(new ChunkCoordinates(1, 2, 3));
        delegate = new TurtleInventoryDelegate(turtle);
    }

    @Test
    public void delegatesEveryInventoryMethod() {
        final ItemStack stack = new ItemStack(new Item());
        when(inventory.getSizeInventory()).thenReturn(16);
        when(inventory.getStackInSlot(0)).thenReturn(stack);
        when(inventory.decrStackSize(0, 1)).thenReturn(stack);
        when(inventory.getStackInSlotOnClosing(0)).thenReturn(stack);
        when(inventory.getInventoryName()).thenReturn("turtle");
        when(inventory.hasCustomInventoryName()).thenReturn(true);
        when(inventory.getInventoryStackLimit()).thenReturn(64);
        when(inventory.isUseableByPlayer(null)).thenReturn(true);
        when(inventory.isItemValidForSlot(0, stack)).thenReturn(true);

        Assert.assertEquals(16, delegate.getSizeInventory());
        Assert.assertSame(stack, delegate.getStackInSlot(0));
        Assert.assertSame(stack, delegate.decrStackSize(0, 1));
        Assert.assertSame(stack, delegate.getStackInSlotOnClosing(0));
        Assert.assertEquals("turtle", delegate.getInventoryName());
        Assert.assertTrue(delegate.hasCustomInventoryName());
        Assert.assertEquals(64, delegate.getInventoryStackLimit());
        Assert.assertTrue(delegate.isUseableByPlayer(null));
        Assert.assertTrue(delegate.isItemValidForSlot(0, stack));

        delegate.setInventorySlotContents(0, stack);
        delegate.markDirty();
        delegate.openInventory();
        delegate.closeInventory();
        verify(inventory).setInventorySlotContents(0, stack);
        verify(inventory).markDirty();
        verify(inventory).openInventory();
        verify(inventory).closeInventory();
    }

    @Test
    public void exposesWorldAndPositionValidity() {
        when(world.blockExists(1, 2, 3)).thenReturn(true);
        Assert.assertSame(world, delegate.getWorld());
        Assert.assertEquals(1, delegate.getX());
        Assert.assertEquals(2, delegate.getY());
        Assert.assertEquals(3, delegate.getZ());
        Assert.assertTrue(delegate.isValid());

        when(world.blockExists(1, 2, 3)).thenReturn(false);
        Assert.assertFalse(delegate.isValid());
    }
}
