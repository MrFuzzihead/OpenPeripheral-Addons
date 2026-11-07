package openperipheral.addons.peripheralproxy;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTargeted;

public class WrappedPeripheral implements IPeripheralTargeted {

    private final IPeripheral peripheral;

    public WrappedPeripheral(IPeripheral peripheral) {
        this.peripheral = peripheral;
    }

    @Override
    public String getType() {
        return (peripheral != null) ? peripheral.getType() : null;
    }

    @Override
    public String[] getMethodNames() {
        return (peripheral != null) ? peripheral.getMethodNames() : null;
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
        throws LuaException, InterruptedException {
        return (peripheral != null) ? peripheral.callMethod(computer, context, method, arguments) : null;
    }

    // CC 3.2: the Lua bridge now calls callMethodResult(...), whose IPeripheral default delegates to
    // callMethod(...). Without this override a wrapped peripheral that suspends via MethodResult
    // (e.g. OpenPeripheral's AdapterPeripheral) would lose its suspension behind this proxy.
    @Override
    public Object callMethodResult(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
        throws LuaException, InterruptedException {
        return (peripheral != null) ? peripheral.callMethodResult(computer, context, method, arguments) : null;
    }

    @Override
    public void attach(IComputerAccess computer) {
        if (peripheral != null) peripheral.attach(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        if (peripheral != null) peripheral.detach(computer);
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
        return other != null && other.equals(peripheral);
    }

}
