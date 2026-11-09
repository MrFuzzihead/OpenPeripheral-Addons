package openperipheral.addons.glasses;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

import openperipheral.addons.api.ITerminalIdGetter;
import openperipheral.addons.api.ITerminalIdSetter;
import openperipheral.addons.api.ITerminalItem;

public class TerminalIdAccessTest {

    private static class FakeTerminalItem extends Item implements ITerminalItem {

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

    private static class FakeGetter implements ITerminalIdGetter {

        private final int priority;
        private final Long value;

        private FakeGetter(int priority, Long value) {
            this.priority = priority;
            this.value = value;
        }

        @Override
        public Optional<Long> getFor(EntityPlayer player) {
            return Optional.fromNullable(value);
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    private static class FakeSetter implements ITerminalIdSetter {

        private final int priority;
        private final boolean accepts;
        private boolean called;

        private FakeSetter(int priority, boolean accepts) {
            this.priority = priority;
            this.accepts = accepts;
        }

        @Override
        public boolean setFor(EntityPlayer player, long terminalId) {
            called = true;
            return accepts;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    @Before
    public void resetRegistry() throws Exception {
        setField("getters", new ITerminalIdGetter[0]);
        setField("setters", new ITerminalIdSetter[0]);
    }

    private static void setField(String name, Object value) throws Exception {
        final Field field = TerminalIdAccess.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(TerminalIdAccess.instance, value);
    }

    @Test
    public void returnsFirstPresentGetterInPriorityOrder() {
        final TerminalIdAccess access = TerminalIdAccess.instance;
        access.register(new FakeGetter(10, null));
        access.register(new FakeGetter(0, 42L));
        access.register(new FakeGetter(5, 7L));

        Assert.assertEquals(
            Long.valueOf(42L),
            access.getIdFrom(null)
                .orNull());
    }

    @Test
    public void returnsAbsentWhenNoGetterHasAValue() {
        final TerminalIdAccess access = TerminalIdAccess.instance;
        access.register(new FakeGetter(0, null));
        Assert.assertFalse(
            access.getIdFrom(null)
                .isPresent());
    }

    @Test
    public void usesFirstAcceptingSetterInPriorityOrder() {
        final TerminalIdAccess access = TerminalIdAccess.instance;
        final FakeSetter rejecting = new FakeSetter(0, false);
        final FakeSetter accepting = new FakeSetter(5, true);
        access.register(rejecting);
        access.register(accepting);

        Assert.assertTrue(access.setIdFor(null, 9L));
        Assert.assertTrue(rejecting.called);
        Assert.assertTrue(accepting.called);
    }

    @Test
    public void itemInterfaceGetterAndSetterDelegateToTheItem() {
        final FakeTerminalItem item = new FakeTerminalItem();
        final ItemStack stack = new ItemStack(item);

        final TerminalIdAccess.InterfaceGetter getter = new TerminalIdAccess.InterfaceGetter();
        Assert.assertFalse(
            getter.getFor(stack)
                .isPresent());

        final TerminalIdAccess.InterfaceSetter setter = new TerminalIdAccess.InterfaceSetter();
        Assert.assertTrue(setter.setFor(stack, 123L));
        Assert.assertEquals(
            Long.valueOf(123L),
            getter.getFor(stack)
                .orNull());
    }
}
