package openperipheral.addons.pim;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

public class BlockPIMTest {

    @Test
    public void geometryAndDropBehaviourAreCorrect() {
        final BlockPIM block = new BlockPIM();
        Assert.assertFalse(block.isOpaqueCube());
        Assert.assertFalse(block.shouldDropFromTeAfterBreak());
        Assert.assertTrue(block.isSideSolid(null, 0, 0, 0, ForgeDirection.DOWN));
        Assert.assertFalse(block.isSideSolid(null, 0, 0, 0, ForgeDirection.UP));
    }

    @Test
    public void collisionOnlyUpdatesTheServerSideTileEntity() throws Exception {
        final BlockPIM block = new BlockPIM();
        final World world = mock(World.class);
        final EntityPlayer player = mock(EntityPlayer.class);
        final TileEntityPIM pim = mock(TileEntityPIM.class);
        when(world.getTileEntity(0, 0, 0)).thenReturn(pim);

        block.onEntityCollidedWithBlock(world, 0, 0, 0, player);
        verify(world).markBlockForUpdate(0, 0, 0);
        verify(pim).trySetPlayer(player);

        final Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.set(world, true);
        final Entity nonPlayer = mock(Entity.class);
        block.onEntityCollidedWithBlock(world, 0, 0, 0, nonPlayer);
        verifyNoInteractions(nonPlayer);
    }

    @Test
    public void registersBothIconVariants() {
        final BlockPIM block = new BlockPIM();
        final IIconRegister registry = mock(IIconRegister.class);
        final IIcon icon = mock(IIcon.class);
        when(registry.registerIcon(anyString())).thenReturn(icon);

        block.registerBlockIcons(registry);
        Assert.assertSame(icon, BlockPIM.Icons.black);
        Assert.assertSame(icon, BlockPIM.Icons.blue);
    }
}
