package openperipheral.addons.ticketmachine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import openmods.inventory.GenericInventory;

public class TileEntityTicketMachineTest {

    private TileEntityTicketMachine machine;
    private GenericInventory inventory;
    private WorldServer world;
    private Item ticket;

    @BeforeClass
    public static void registerTileEntityMapping() {
        TileEntity.addMapping(TileEntityTicketMachine.class, "openperipheral_ticketmachine_test");
    }

    @Before
    public void setup() throws Exception {
        world = mock(WorldServer.class);
        final WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        setField(World.class, world, "provider", provider);
        setField(World.class, world, "playerEntities", new ArrayList<EntityPlayer>());

        machine = new TileEntityTicketMachine();
        setField(TileEntity.class, machine, "worldObj", world);
        ticket = new Item();
        setField(TileEntityTicketMachine.class, machine, "ticketItem", ticket);

        inventory = mock(GenericInventory.class);
        machine.inventory = inventory;
        machine.owner.setValue("owner");
    }

    private void paperAndInk() {
        final ItemStack paper = new ItemStack(new Item(), 5);
        final ItemStack ink = new ItemStack(new Item(), 5);
        when(inventory.getStackInSlot(0)).thenReturn(paper);
        when(inventory.getStackInSlot(1)).thenReturn(ink);
        when(inventory.isItemValidForSlot(eq(0), any(ItemStack.class))).thenReturn(true);
        when(inventory.isItemValidForSlot(eq(1), any(ItemStack.class))).thenReturn(true);
        when(inventory.getStackInSlot(2)).thenReturn(null);
    }

    @Test
    public void createTicketConsumesInputsAndInsertsTheRequestedAmount() {
        paperAndInk();

        Assert.assertTrue(machine.createTicket("destination", 3));

        final ArgumentCaptor<ItemStack> output = ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory).setInventorySlotContents(eq(2), output.capture());
        Assert.assertEquals(3, output.getValue().stackSize);
        Assert.assertEquals(
            "destination",
            output.getValue()
                .getTagCompound()
                .getString("dest"));
        verify(inventory).decrStackSize(0, 3);
        verify(inventory).decrStackSize(1, 3);
    }

    @Test
    public void createTicketStacksOntoMatchingOutput() {
        paperAndInk();
        final ItemStack output = TileEntityTicketMachine.createTicketStack(ticket, "owner", "destination", 1);
        when(inventory.getStackInSlot(2)).thenReturn(output);

        Assert.assertTrue(machine.createTicket("destination", 2));
        Assert.assertEquals(3, output.stackSize);
        verify(inventory).decrStackSize(0, 2);
        verify(inventory).decrStackSize(1, 2);
    }

    @Test
    public void rejectsFullOrMismatchedOutputWithoutConsumingInputs() {
        paperAndInk();
        final ItemStack mismatched = TileEntityTicketMachine.createTicketStack(ticket, "owner", "other", 1);
        when(inventory.getStackInSlot(2)).thenReturn(mismatched);

        try {
            machine.createTicket("destination", 1);
            Assert.fail("Mismatched output must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        verify(inventory, never()).decrStackSize(anyInt(), anyInt());

        final ItemStack full = TileEntityTicketMachine.createTicketStack(ticket, "owner", "destination", 1);
        full.stackSize = full.getMaxStackSize();
        when(inventory.getStackInSlot(2)).thenReturn(full);
        try {
            machine.createTicket("destination", 1);
            Assert.fail("Full output must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void exposesOwnerAndGuiAndNbtLifecycle() {
        Assert.assertEquals("owner", machine.getOwner());
        Assert.assertTrue(machine.canOpenGui(mock(EntityPlayer.class)));
        Assert.assertNotNull(machine.getServerGui(mock(EntityPlayer.class)));
        Assert.assertSame(inventory, machine.getInventory());

        final EntityPlayer player = mock(EntityPlayer.class);
        when(player.getCommandSenderName()).thenReturn("placer");
        machine.onBlockPlacedBy(player, null, null, 0, 0, 0);
        Assert.assertEquals("placer", machine.getOwner());

        final NBTTagCompound tag = new NBTTagCompound();
        machine.writeToNBT(tag);
        machine.readFromNBT(tag);
    }

    @Test
    public void inventoryChangesUpdateTheTicketIconFlag() {
        when(inventory.getStackInSlot(2)).thenReturn(new ItemStack(new Item()));
        machine.onInventoryChanged(inventory, 2);
        Assert.assertTrue(machine.hasTicket.getValue());

        when(inventory.getStackInSlot(2)).thenReturn(null);
        machine.onInventoryChanged(inventory, 2);
        Assert.assertFalse(machine.hasTicket.getValue());
    }

    @Test
    public void createTicketFailsWithoutRailcraftItem() throws Exception {
        setField(TileEntityTicketMachine.class, machine, "ticketItem", null);
        try {
            machine.createTicket("destination", 1);
            Assert.fail("Missing ticket item must be rejected");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
