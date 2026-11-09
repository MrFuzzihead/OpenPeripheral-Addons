package openperipheral.addons.peripheralproxy;

import com.google.common.base.Preconditions;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTargeted;

public class WrappedPeripheral implements IPeripheralTargeted {

    private final IPeripheral peripheral;

    public WrappedPeripheral(IPeripheral peripheral) {
        this.peripheral = Preconditions.checkNotNull(peripheral, "peripheral");
    }

    @Override
    public String getType() {
        return peripheral.getType();
    }

    @Override
    public String[] getMethodNames() {
        return peripheral.getMethodNames();
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
        throws LuaException, InterruptedException {
        return peripheral.callMethod(computer, context, method, arguments);
    }

    // CC 3.2: the Lua bridge now calls callMethodResult(...), whose IPeripheral default delegates to
    // callMethod(...). Without this override a wrapped peripheral that suspends via MethodResult
    // (e.g. OpenPeripheral's AdapterPeripheral) would lose its suspension behind this proxy.
    @Override
    public Object callMethodResult(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
        throws LuaException, InterruptedException {
        return peripheral.callMethodResult(computer, context, method, arguments);
    }

    @Override
    public void attach(IComputerAccess computer) {
        peripheral.attach(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        peripheral.detach(computer);
    }

    // CC 3.3: InventoryPeripheral resolves networked peripherals through IPeripheralTargeted to find the inventory
    // behind them (chest.pushItems(name, ...) and friends). Without this delegation the proxy would make every
    // wrapped peripheral look like it has no target. OpenPeripheral 2.1.0 made the same addition to its adapter
    // peripherals, so wrapped objects report their TileEntity here too.
    @Override
    public Object getTarget() {
        return (peripheral instanceof IPeripheralTargeted) ? ((IPeripheralTargeted) peripheral).getTarget() : null;
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other == null) return false;
        if (other instanceof WrappedPeripheral) {
            final IPeripheral otherDelegate = ((WrappedPeripheral) other).peripheral;
            return peripheral == otherDelegate || peripheral.equals(otherDelegate);
        }

        return peripheral.equals(other);
    }

    @Override
    public int hashCode() {
        return peripheral.hashCode();
    }

}
