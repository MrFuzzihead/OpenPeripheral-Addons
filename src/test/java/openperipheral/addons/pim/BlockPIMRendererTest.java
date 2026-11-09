package openperipheral.addons.pim;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.junit.Assert;
import org.junit.Test;

public class BlockPIMRendererTest {

    @Test
    public void rendersTheBaseAndTheOccupiedTopLayer() {
        final BlockPIMRenderer renderer = new BlockPIMRenderer();
        final BlockPIM block = new BlockPIM();
        final IBlockAccess world = mock(IBlockAccess.class);
        final RenderBlocks renderBlocks = mock(RenderBlocks.class);
        final TileEntityPIM pim = mock(TileEntityPIM.class);
        when(pim.hasPlayer()).thenReturn(true);
        when(world.getTileEntity(1, 2, 3)).thenReturn(pim);
        when(renderBlocks.hasOverrideBlockTexture()).thenReturn(false);
        BlockPIM.Icons.black = mock(IIcon.class);

        Assert.assertTrue(renderer.renderWorldBlock(world, 1, 2, 3, block, 0, renderBlocks));
        verify(renderBlocks).setRenderBoundsFromBlock(block);
        verify(renderBlocks).setOverrideBlockTexture(BlockPIM.Icons.black);
        verify(renderBlocks).clearOverrideBlockTexture();
        verify(renderBlocks, org.mockito.Mockito.times(2)).renderStandardBlock(block, 1, 2, 3);
    }

    @Test
    public void keepsBreakingOverlayTexturesAndHandlesEmptyOccupancy() {
        final BlockPIMRenderer renderer = new BlockPIMRenderer();
        final BlockPIM block = new BlockPIM();
        final IBlockAccess world = mock(IBlockAccess.class);
        final RenderBlocks renderBlocks = mock(RenderBlocks.class);
        when(world.getTileEntity(anyInt(), anyInt(), anyInt())).thenReturn(null);
        when(renderBlocks.hasOverrideBlockTexture()).thenReturn(true);

        Assert.assertTrue(renderer.renderWorldBlock(world, 0, 0, 0, block, 0, renderBlocks));
        verify(renderBlocks, org.mockito.Mockito.never()).setOverrideBlockTexture(any(IIcon.class));
        verify(renderBlocks, org.mockito.Mockito.never()).clearOverrideBlockTexture();
    }
}
