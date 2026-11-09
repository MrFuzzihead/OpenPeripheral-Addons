package openperipheral.addons.peripheralproxy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTargeted;

public class WrappedPeripheralTest {

    @Test
    public void delegatesAllPeripheralCalls() throws Exception {
        final IPeripheral delegate = mock(IPeripheral.class);
        when(delegate.getType()).thenReturn("chest");
        when(delegate.getMethodNames()).thenReturn(new String[] { "getSize" });

        final WrappedPeripheral wrapper = new WrappedPeripheral(delegate);
        final IComputerAccess computer = mock(IComputerAccess.class);
        final ILuaContext context = mock(ILuaContext.class);
        final Object[] args = new Object[] { 1 };

        Assert.assertEquals("chest", wrapper.getType());
        Assert.assertArrayEquals(new String[] { "getSize" }, wrapper.getMethodNames());
        wrapper.callMethod(computer, context, 0, args);
        wrapper.callMethodResult(computer, context, 0, args);
        wrapper.attach(computer);
        wrapper.detach(computer);

        verify(delegate).callMethod(computer, context, 0, args);
        verify(delegate).callMethodResult(computer, context, 0, args);
        verify(delegate).attach(computer);
        verify(delegate).detach(computer);
    }

    @Test
    public void delegatesTargetedPeripherals() {
        final TargetedPeripheral delegate = mock(TargetedPeripheral.class);
        final Object target = new Object();
        when(delegate.getTarget()).thenReturn(target);

        Assert.assertSame(target, new WrappedPeripheral(delegate).getTarget());
    }

    @Test
    public void equalityComparesWrappedPeripherals() {
        final IPeripheral delegate = mock(IPeripheral.class);
        final WrappedPeripheral first = new WrappedPeripheral(delegate);
        final WrappedPeripheral second = new WrappedPeripheral(delegate);

        Assert.assertTrue(first.equals(first));
        Assert.assertTrue(first.equals(second));
        Assert.assertTrue(second.equals(first));
        Assert.assertFalse(first.equals(mock(IPeripheral.class)));
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullDelegate() {
        new WrappedPeripheral(null);
    }

    private interface TargetedPeripheral extends IPeripheral, IPeripheralTargeted {
    }
}
