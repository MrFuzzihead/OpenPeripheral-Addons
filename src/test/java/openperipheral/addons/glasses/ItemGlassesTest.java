package openperipheral.addons.glasses;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;

public class ItemGlassesTest {

    @Test
    public void bindsAndReadsGuid() {
        final ItemGlasses glasses = new ItemGlasses();
        final ItemStack stack = new ItemStack(glasses);

        Assert.assertNull(glasses.getTerminalGuid(stack));

        glasses.bindToTerminal(stack, 12345L);
        Assert.assertEquals(Long.valueOf(12345L), glasses.getTerminalGuid(stack));
    }

    @Test
    public void readsLegacyStringGuid() {
        final ItemGlasses glasses = new ItemGlasses();
        final ItemStack stack = new ItemStack(glasses);

        final NBTTagCompound openp = new NBTTagCompound();
        openp.setString("guid", TerminalUtils.formatTerminalId(54L));
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("openp", openp);
        stack.setTagCompound(tag);

        Assert.assertEquals(Long.valueOf(54L), glasses.getTerminalGuid(stack));
    }

    @Test
    public void wrongOpenpTagTypeDoesNotThrowAndIsReplaced() {
        final ItemGlasses glasses = new ItemGlasses();
        final ItemStack stack = new ItemStack(glasses);

        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("openp", "not a compound");
        stack.setTagCompound(tag);

        Assert.assertNull(glasses.getTerminalGuid(stack));

        glasses.bindToTerminal(stack, 9L);
        Assert.assertEquals(Long.valueOf(9L), glasses.getTerminalGuid(stack));
    }

    @Test
    public void readsNbtProviderTerminalTag() {
        final ItemStack stack = new ItemStack(new ItemGlasses());
        NbtGuidProviders.setTerminalGuid(stack, Optional.of(77L));
        Assert.assertEquals(
            Long.valueOf(77L),
            NbtGuidProviders.getTerminalGuid(stack)
                .orNull());
    }
}
