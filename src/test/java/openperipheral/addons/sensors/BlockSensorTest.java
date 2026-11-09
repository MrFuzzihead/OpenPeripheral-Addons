package openperipheral.addons.sensors;

import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

public class BlockSensorTest {

    @Test
    public void sensorBlockExposesItsRenderingAndCollisionContract() {
        final BlockSensor block = new BlockSensor();
        Assert.assertTrue(block.canCollideCheck(0, true));
        Assert.assertFalse(block.renderAsNormalBlock());
        Assert.assertFalse(block.isOpaqueCube());
        Assert.assertTrue(block.isSideSolid((IBlockAccess) null, 0, 0, 0, ForgeDirection.DOWN));
        Assert.assertFalse(block.isSideSolid((IBlockAccess) null, 0, 0, 0, ForgeDirection.NORTH));
    }
}
