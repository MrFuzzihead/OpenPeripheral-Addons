package openperipheral.addons.sensors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.Assert;
import org.junit.Test;

import openperipheral.addons.Config;

public class TileEntitySensorTest {

    @Test
    public void exposesLocationAndRotatesOnEveryTick() {
        final TileEntitySensor sensor = new TileEntitySensor();
        sensor.xCoord = 1;
        sensor.yCoord = 2;
        sensor.zCoord = 3;

        Assert.assertEquals(1.0, sensor.getLocation().xCoord, 0.0001);
        Assert.assertEquals(2.0, sensor.getLocation().yCoord, 0.0001);
        Assert.assertEquals(3.0, sensor.getLocation().zCoord, 0.0001);
        Assert.assertFalse(sensor.isTurtle());

        final int before = sensor.getRotation();
        sensor.updateEntity();
        Assert.assertNotEquals(before, sensor.getRotation());
    }

    @Test
    public void stormRangeUsesTheConfiguredValues() throws Exception {
        final World world = mock(World.class);
        final TileEntitySensor sensor = new TileEntitySensor();
        final Field worldField = TileEntity.class.getDeclaredField("worldObj");
        worldField.setAccessible(true);
        worldField.set(sensor, world);

        when(world.isRaining()).thenReturn(false);
        when(world.isThundering()).thenReturn(false);
        Assert.assertEquals(Config.sensorRange, sensor.getSensorRange());

        when(world.isRaining()).thenReturn(true);
        when(world.isThundering()).thenReturn(true);
        Assert.assertEquals(Config.sensorRangeInStorm, sensor.getSensorRange());
    }
}
