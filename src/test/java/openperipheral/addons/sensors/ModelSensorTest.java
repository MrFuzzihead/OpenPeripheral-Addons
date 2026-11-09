package openperipheral.addons.sensors;

import org.junit.Assert;
import org.junit.Test;

public class ModelSensorTest {

    @Test
    public void modelPartsAreInitialised() {
        final ModelSensor model = new ModelSensor();
        Assert.assertNotNull(model.sensorBase);
        Assert.assertNotNull(model.sensorAxel);
        Assert.assertNotNull(model.sensorDishCenter);
        Assert.assertNotNull(model.sensorDishLeft);
        Assert.assertNotNull(model.sensorDishRight);
    }
}
