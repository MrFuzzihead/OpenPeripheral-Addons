package openperipheral.addons.glasses;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;

import openperipheral.addons.api.ITerminalItem;

public class TerminalMetaProviderTest {

    private static final class TestTerminalItem extends Item implements ITerminalItem {

        private Long guid;

        @Override
        public Long getTerminalGuid(ItemStack stack) {
            return guid;
        }

        @Override
        public void bindToTerminal(ItemStack stack, long guid) {
            this.guid = guid;
        }
    }

    @Test
    public void itemTerminalProviderAddsBridgeNameOnlyWhenBound() {
        final TestTerminalItem item = new TestTerminalItem();
        final ItemStack stack = new ItemStack(item);
        final ItemTerminalMetaProvider provider = new ItemTerminalMetaProvider();

        Assert.assertEquals("op_terminal", provider.getKey());
        Assert.assertTrue((((Map<?, ?>) provider.getMeta(item, stack))).isEmpty());

        item.bindToTerminal(stack, 1234L);
        final Map<?, ?> meta = (Map<?, ?>) provider.getMeta(item, stack);
        Assert.assertEquals(TerminalUtils.formatTerminalId(1234L), meta.get("bridge_name"));
    }

    @Test
    public void nbtTerminalProviderOnlyAppliesToTerminalItems() {
        final Item item = new Item();
        final ItemStack stack = new ItemStack(item);
        final NbtTerminalMetaProvider provider = new NbtTerminalMetaProvider();

        Assert.assertEquals("op_terminal_embedded", provider.getKey());
        Assert.assertEquals(Item.class, provider.getTargetClass());
        Assert.assertFalse(provider.canApply(item, stack));
        Assert.assertTrue((((Map<?, ?>) provider.getMeta(item, stack))).isEmpty());

        NbtGuidProviders.setTerminalGuid(stack, Optional.of(77L));
        Assert.assertTrue(provider.canApply(item, stack));
        final Map<?, ?> meta = (Map<?, ?>) provider.getMeta(item, stack);
        Assert.assertEquals(TerminalUtils.formatTerminalId(77L), meta.get("bridge_name"));
    }

    @Test
    public void glassesBridgeTooltipShowsTheGuid() {
        final ItemGlassesBridge bridge = new ItemGlassesBridge(new BlockGlassesBridge());
        final ItemStack stack = new ItemStack(bridge);
        final List<String> tooltip = new ArrayList<String>();

        bridge.addInformation(stack, null, tooltip, false);
        Assert.assertTrue(tooltip.isEmpty());

        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong(TileEntityGlassesBridge.TAG_GUID, 1234L);
        stack.setTagCompound(tag);
        bridge.addInformation(stack, null, tooltip, false);
        Assert.assertEquals(1, tooltip.size());
        Assert.assertFalse(
            tooltip.get(0)
                .isEmpty());
    }

    @Test
    public void keyboardExplainsThatGlassesAreRequiredAndDoesNothingServerSide() throws Exception {
        resetTerminalIdAccess();
        TerminalIdAccess.instance.register(new TerminalIdAccess.InterfaceGetter());

        final ItemKeyboard keyboard = new ItemKeyboard();
        final ItemStack stack = new ItemStack(keyboard);
        final net.minecraft.world.World world = mock(net.minecraft.world.World.class);
        final java.lang.reflect.Field remote = net.minecraft.world.World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.set(world, true);

        final net.minecraft.entity.player.EntityPlayer player = mock(net.minecraft.entity.player.EntityPlayer.class);
        final net.minecraft.entity.player.InventoryPlayer inventory = mock(
            net.minecraft.entity.player.InventoryPlayer.class);
        final java.lang.reflect.Field inventoryField = net.minecraft.entity.player.EntityPlayer.class
            .getField("inventory");
        inventoryField.setAccessible(true);
        inventoryField.set(player, inventory);

        Assert.assertSame(stack, keyboard.onItemRightClick(stack, world, player));
        org.mockito.Mockito.verify(player)
            .addChatMessage(org.mockito.ArgumentMatchers.any());

        remote.set(world, false);
        final net.minecraft.entity.player.EntityPlayer serverPlayer = mock(
            net.minecraft.entity.player.EntityPlayer.class);
        inventoryField.set(serverPlayer, inventory);
        keyboard.onItemRightClick(stack, world, serverPlayer);
        org.mockito.Mockito.verifyNoInteractions(serverPlayer);
    }

    private static void resetTerminalIdAccess() throws Exception {
        final java.lang.reflect.Field getters = TerminalIdAccess.class.getDeclaredField("getters");
        getters.setAccessible(true);
        getters.set(TerminalIdAccess.instance, new openperipheral.addons.api.ITerminalIdGetter[0]);
        final java.lang.reflect.Field setters = TerminalIdAccess.class.getDeclaredField("setters");
        setters.setAccessible(true);
        setters.set(TerminalIdAccess.instance, new openperipheral.addons.api.ITerminalIdSetter[0]);
    }
}
