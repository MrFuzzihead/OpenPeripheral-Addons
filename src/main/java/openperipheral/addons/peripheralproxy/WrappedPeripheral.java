package openperipheral.addons.peripheralproxy;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public class WrappedPeripheral implements IPeripheral {

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

    @Override
    public boolean equals(IPeripheral other) {
        return other != null && other.equals(peripheral);
    }

}
