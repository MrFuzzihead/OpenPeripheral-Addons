package openperipheral.addons.sensors;

import net.minecraft.util.Vec3;

import openperipheral.api.adapter.IWorldProvider;

public interface ISensorEnvironment extends IWorldProvider {

    /**
     * Whether this environment belongs to a turtle upgrade rather than a placed sensor block. Kept as part of the
     * adapter contract even though the built-in sensor adapter does not currently branch on it.
     */
    public boolean isTurtle();

    public Vec3 getLocation();

    public int getSensorRange();
}
