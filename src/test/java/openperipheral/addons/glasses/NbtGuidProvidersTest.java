package openperipheral.addons.glasses;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;

import openmods.utils.ItemUtils;

public class NbtGuidProvidersTest {

    private static ItemStack newStack() {
        return new ItemStack(new Item());
    }

    @Test
    public void absentTerminalTagIsNotCreatedByReads() {
        final ItemStack stack = newStack();
        Assert.assertFalse(NbtGuidProviders.hasTerminalCapabilities(stack));
        Assert.assertFalse(
            NbtGuidProviders.getTerminalGuid(stack)
                .isPresent());
        Assert.assertNull(stack.getTagCompound());
    }

    @Test
    public void setAndGetGuidRoundTrip() {
        final ItemStack stack = newStack();
        NbtGuidProviders.setTerminalGuid(stack, Optional.of(42L));

        Assert.assertTrue(NbtGuidProviders.hasTerminalCapabilities(stack));
        Assert.assertEquals(
            Long.valueOf(42L),
            NbtGuidProviders.getTerminalGuid(stack)
                .orNull());

        NbtGuidProviders.setTerminalGuid(stack, Optional.of(43L));
        Assert.assertEquals(
            Long.valueOf(43L),
            NbtGuidProviders.getTerminalGuid(stack)
                .orNull());
    }

    @Test
    public void unbindingRemovesStaleGuid() {
        final ItemStack stack = newStack();
        NbtGuidProviders.setTerminalGuid(stack, Optional.of(42L));
        NbtGuidProviders.setTerminalGuid(stack, Optional.<Long>absent());

        Assert.assertTrue(NbtGuidProviders.hasTerminalCapabilities(stack));
        Assert.assertFalse(
            NbtGuidProviders.getTerminalGuid(stack)
                .isPresent());
    }

    @Test
    public void getOrCreateTerminalTagIsStable() {
        final ItemStack stack = newStack();
        final NBTTagCompound first = NbtGuidProviders.getOrCreateTerminalTag(stack);
        final NBTTagCompound second = NbtGuidProviders.getOrCreateTerminalTag(stack);
        Assert.assertSame(first, second);
        Assert.assertNotNull(
            ItemUtils.getItemTag(stack)
                .getTag("OPA-Terminal"));
    }

    @Test
    public void nbtSetterRequiresAnExistingTerminalTag() {
        final ItemStack stack = newStack();
        Assert.assertFalse(new NbtGuidProviders.NbtSetter().setFor(stack, 7L));

        NbtGuidProviders.getOrCreateTerminalTag(stack);
        Assert.assertTrue(new NbtGuidProviders.NbtSetter().setFor(stack, 7L));
        Assert.assertEquals(
            Long.valueOf(7L),
            new NbtGuidProviders.NbtGetter().getFor(stack)
                .orNull());
    }
}
