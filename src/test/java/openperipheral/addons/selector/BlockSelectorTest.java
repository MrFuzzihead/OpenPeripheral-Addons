package openperipheral.addons.selector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

import org.junit.Assert;
import org.junit.Test;

public class BlockSelectorTest {

    @Test
    public void selectionHighlightUsesTheClickedSlot() {
        final BlockSelector block = new BlockSelector();
        final World world = mock(World.class);
        final TileEntitySelector selector = mock(TileEntitySelector.class);
        when(world.getTileEntity(1, 2, 3)).thenReturn(selector);
        final AxisAlignedBB selection = AxisAlignedBB.getBoundingBox(0.1, 0.1, 0.1, 0.2, 0.2, 0.2);
        when(selector.getSelection(any(Vec3.class))).thenReturn(selection);

        final MovingObjectPosition target = new MovingObjectPosition(0, 0, 0, 1, Vec3.createVectorHelper(1.5, 3, 3.5));
        final DrawBlockHighlightEvent event = new DrawBlockHighlightEvent(null, null, target, 0, null, 0);

        Assert.assertFalse(block.onSelected(world, 1, 2, 3, event));
        Assert.assertSame(selection, block.getSelectedBoundingBoxFromPool(world, 1, 2, 3));

        Assert.assertNotSame(selection, block.getSelectedBoundingBoxFromPool(world, 4, 5, 6));

        when(world.getTileEntity(1, 2, 3)).thenReturn(null);
        Assert.assertFalse(block.onSelected(world, 1, 2, 3, event));
        Assert.assertNotNull(block.getSelectedBoundingBoxFromPool(world, 1, 2, 3));
    }

    @Test
    public void blockIsNotOpaque() {
        Assert.assertFalse(new BlockSelector().isOpaqueCube());
    }
}
