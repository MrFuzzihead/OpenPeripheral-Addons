package openperipheral.addons.ticketmachine;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Test;

import openmods.utils.ItemUtils;

public class TicketMachineTest {

    @Test
    public void createsRequestedNumberOfTickets() {
        final Item ticket = new Item();
        final ItemStack stack = TileEntityTicketMachine.createTicketStack(ticket, "owner", "destination", 3);

        Assert.assertEquals(ticket, stack.getItem());
        Assert.assertEquals(3, stack.stackSize);
        Assert.assertEquals(
            "owner",
            ItemUtils.getItemTag(stack)
                .getString("owner"));
        Assert.assertEquals(
            "destination",
            ItemUtils.getItemTag(stack)
                .getString("dest"));
    }

    @Test
    public void handlesMissingOwner() {
        final ItemStack stack = TileEntityTicketMachine.createTicketStack(new Item(), null, "destination", 1);
        Assert.assertEquals(
            "",
            ItemUtils.getItemTag(stack)
                .getString("owner"));
    }

    @Test
    public void validatesDestinationAndAmount() {
        final Item ticket = new Item();

        assertInvalid(ticket, null, 1);
        assertInvalid(ticket, "", 1);
        assertInvalid(ticket, repeat('x', 33), 1);
        assertInvalid(ticket, "destination", 0);
        assertInvalid(ticket, "destination", 65);
    }

    @Test
    public void respectsTicketStackSize() {
        final Item single = new Item();
        single.setMaxStackSize(1);

        try {
            TileEntityTicketMachine.createTicketStack(single, "owner", "destination", 2);
            Assert.fail("Amount above the ticket stack size must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsMissingRailcraftItem() {
        TileEntityTicketMachine.createTicketStack(null, "owner", "destination", 1);
    }

    private static void assertInvalid(Item ticket, String destination, int amount) {
        try {
            TileEntityTicketMachine.createTicketStack(ticket, "owner", destination, amount);
            Assert.fail("Expected invalid ticket arguments: " + destination + " / " + amount);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static String repeat(char value, int count) {
        final StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) builder.append(value);
        return builder.toString();
    }
}
