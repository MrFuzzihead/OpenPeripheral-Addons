package openperipheral.addons.selector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import openperipheral.api.architecture.IArchitecture;
import openperipheral.api.architecture.IArchitectureAccess;
import openperipheral.api.helpers.Index;

public class TileEntitySelectorTest {

    private TileEntitySelector selector;
    private WorldServer world;

    @Before
    public void createSelectorWithMockWorld() throws Exception {
        world = mock(WorldServer.class);
        final WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        final Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(world, provider);
        // SyncMapTile iterates this field directly; Mockito mocks do not initialise it.
        final Field players = World.class.getDeclaredField("playerEntities");
        players.setAccessible(true);
        players.set(world, new ArrayList<EntityPlayer>());

        selector = new TileEntitySelector();
        final Field worldField = TileEntity.class.getDeclaredField("worldObj");
        worldField.setAccessible(true);
        worldField.set(selector, world);
    }

    private static ItemStack stack() {
        return new ItemStack(new Item());
    }

    @Test
    public void acceptsOneBasedScriptIndexesAndRejectsOutOfRange() {
        final ItemStack stack = stack();
        selector.setSlot(Index.fromJava(0, 1), stack);
        Assert.assertNotNull(selector.getSlot(0));
        Assert.assertNotNull(selector.getSlotOneBased(Index.fromJava(0, 1)));

        selector.setSlot(Index.fromJava(8, 1), stack);
        Assert.assertNotNull(selector.getSlot(8));

        try {
            selector.setSlot(Index.fromJava(9, 1), stack);
            Assert.fail("Index 10 must be rejected");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }

        try {
            selector.getSlotOneBased(Index.fromJava(-1, 1));
            Assert.fail("Index 0 must be rejected");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    @Test
    public void setSlotsUsesIndexKeysAndUpdatesGridSize() {
        final Map<Index, ItemStack> stacks = new HashMap<Index, ItemStack>();
        stacks.put(Index.fromJava(0, 1), stack());
        stacks.put(Index.fromJava(2, 1), stack());

        final IArchitecture architecture = mock(IArchitecture.class);
        when(architecture.createIndex(anyInt())).thenAnswer(new Answer<Index>() {

            @Override
            public Index answer(InvocationOnMock invocation) {
                return Index.fromJava((Integer) invocation.getArguments()[0], 1);
            }
        });

        selector.setSlots(architecture, stacks);

        Assert.assertNotNull(selector.getSlot(0));
        Assert.assertNotNull(selector.getSlot(2));
        Assert.assertNull(selector.getSlot(1));
        Assert.assertEquals(3, selector.getGridSize());
    }

    @Test
    public void gridSizeReflectsOuterSlots() {
        selector.setSlot(Index.fromJava(0, 1), stack());
        Assert.assertEquals(1, selector.getGridSize());

        selector.setSlot(Index.fromJava(1, 1), stack());
        Assert.assertEquals(2, selector.getGridSize());

        selector.setSlot(Index.fromJava(2, 1), stack());
        Assert.assertEquals(3, selector.getGridSize());
    }

    @Test
    public void slotListsMatchGridSize() {
        Assert.assertEquals(
            0,
            selector.getSlots(0)
                .size());
        Assert.assertEquals(
            1,
            selector.getSlots(1)
                .size());
        Assert.assertEquals(
            4,
            selector.getSlots(2)
                .size());
        Assert.assertEquals(
            9,
            selector.getSlots(3)
                .size());

        final List<ItemStack> all = selector.getSlots();
        Assert.assertEquals(9, all.size());
        Assert.assertNull(all.get(0));
    }

    @Test
    public void selectionAndClickSignalWorkForTheFirstGridSlot() {
        selector.setSlot(Index.fromJava(0, 1), stack());

        final Vec3 hit = Vec3.createVectorHelper(0.5, 1.0, 0.5);
        Assert.assertNotNull(selector.getSelection(hit));

        final IArchitectureAccess computer = mock(IArchitectureAccess.class);
        when(computer.createIndex(0)).thenReturn(Index.fromJava(0, 1));
        when(computer.peripheralName()).thenReturn("back");
        selector.addComputer(computer);

        final EntityPlayer player = mock(EntityPlayer.class);
        when(player.isSneaking()).thenReturn(false);
        Assert.assertTrue(selector.onBlockActivated(player, 1, 0.5f, 1.0f, 0.5f));
        verify(computer).signal(eq("slot_click"), any(Object[].class));

        when(player.isSneaking()).thenReturn(true);
        Assert.assertTrue(selector.onBlockActivated(player, 1, 0.5f, 1.0f, 0.5f));

        selector.removeComputer(computer);
    }

    @Test
    public void inventoryWrapperIsBackedByTheSelectorSlots() {
        final ItemStack stack = stack();
        final net.minecraft.inventory.IInventory inventory = selector.createInventoryWrapper();
        when(world.getTileEntity(0, 0, 0)).thenReturn(selector);
        final EntityPlayer player = mock(EntityPlayer.class);
        when(player.getDistanceSq(0.5, 0.5, 0.5)).thenReturn(4.0);

        Assert.assertTrue(inventory.isUseableByPlayer(player));
        Assert.assertEquals(9, inventory.getSizeInventory());
        Assert.assertEquals(1, inventory.getInventoryStackLimit());
        Assert.assertEquals("selector", inventory.getInventoryName());
        Assert.assertFalse(inventory.hasCustomInventoryName());
        Assert.assertTrue(inventory.isItemValidForSlot(0, stack));
        Assert.assertNull(inventory.getStackInSlotOnClosing(0));

        inventory.setInventorySlotContents(0, stack);
        Assert.assertNotNull(inventory.getStackInSlot(0));
        inventory.markDirty();
        inventory.openInventory();
        inventory.closeInventory();

        inventory.setInventorySlotContents(0, new ItemStack(new Item(), 2));
        Assert.assertNotNull(inventory.decrStackSize(0, 1));
        Assert.assertNotNull(inventory.decrStackSize(0, 1));
        Assert.assertNull(inventory.decrStackSize(0, 1));
    }

    @Test
    public void displayEntityIsCachedAndReadFromNbtResetsGrid() {
        Assert.assertNotNull(selector.getDisplayEntity());
        Assert.assertSame(selector.getDisplayEntity(), selector.getDisplayEntity());

        selector.setSlot(Index.fromJava(0, 1), stack());
        selector.readFromNBT(new NBTTagCompound());
        Assert.assertEquals(0, selector.getGridSize());
    }

    @Test
    public void guiFactoryMethodsAndActivationEdgeCases() {
        Assert.assertFalse(selector.canOpenGui(mock(EntityPlayer.class)));
        Assert.assertNotNull(selector.getServerGui(mock(EntityPlayer.class)));

        final EntityPlayer player = mock(EntityPlayer.class);
        when(player.isSneaking()).thenReturn(false);
        selector.onBlockActivated(player, 1, 0.1f, 0.0f, 0.1f);
        verify(player).openGui(any(), eq(-1), eq(world), eq(0), eq(0), eq(0));
    }

}
