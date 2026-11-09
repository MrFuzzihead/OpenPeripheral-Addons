package openperipheral.addons.peripheralproxy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import openmods.utils.Coord;

public class TileEntityPeripheralProxyTest {

    private TileEntityPeripheralProxy proxy;
    private World world;

    @Before
    public void setup() throws Exception {
        world = mock(World.class);
        proxy = new TileEntityPeripheralProxy();
        setField(TileEntity.class, proxy, "worldObj", world);
        setField(TileEntity.class, proxy, "blockType", new BlockPeripheralProxy());
        proxy.xCoord = 0;
        proxy.yCoord = 0;
        proxy.zCoord = 0;
    }

    @Test
    public void createsAWrapperWhenTheTargetHasAPeripheral() {
        final ForgeDirection targetDir = proxy.getOrientation()
            .up();
        final IPeripheral target = mock(IPeripheral.class);
        when(target.getType()).thenReturn("test");
        final Block targetBlock = mock(Block.class);
        when(world.getBlock(targetDir.offsetX, targetDir.offsetY, targetDir.offsetZ)).thenReturn(targetBlock);

        try (MockedStatic<ComputerCraft> cc = Mockito.mockStatic(ComputerCraft.class)) {
            cc.when(
                () -> ComputerCraft.getPeripheralAt(world, targetDir.offsetX, targetDir.offsetY, targetDir.offsetZ, 0))
                .thenReturn(target);

            final int side = targetDir.getOpposite()
                .ordinal();
            final IPeripheral peripheral = proxy.createPeripheral(side);
            Assert.assertNotNull(peripheral);
            Assert.assertTrue(peripheral instanceof WrappedPeripheral);
            Assert.assertEquals("test", peripheral.getType());

            proxy.onNeighbourChanged(targetBlock);
        }
    }

    @Test
    public void neighbourChangesRefreshTheModemOnlyWhenNeeded() {
        final ForgeDirection modemDir = proxy.getOrientation()
            .up()
            .getOpposite();
        final TileCable cable = mock(TileCable.class);
        when(cable.isPeripheralAccessAllowed()).thenReturn(true);
        when(world.getTileEntity(modemDir.offsetX, modemDir.offsetY, modemDir.offsetZ)).thenReturn(cable);

        proxy.onNeighbourChanged(null);
        verify(cable).networkChanged();
        verify(world)
            .notifyBlockOfNeighborChange(modemDir.offsetX, modemDir.offsetY, modemDir.offsetZ, proxy.getBlockType());

        final Coord neighbour = proxy.getPosition()
            .offset(
                proxy.getOrientation()
                    .up());
        proxy.onNeighbourTeChanged(neighbour.x, neighbour.y, neighbour.z);
        verify(cable, org.mockito.Mockito.atLeastOnce()).networkChanged();

        when(cable.isPeripheralAccessAllowed()).thenReturn(false);
        proxy.onNeighbourChanged(null);
        verify(cable, org.mockito.Mockito.atLeastOnce()).networkChanged();
    }

    @Test
    public void clientSideNeighbourUpdatesAreIgnored() throws Exception {
        final Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.set(world, true);
        proxy.onNeighbourChanged(null);
        proxy.onNeighbourTeChanged(0, 0, 0);
        verify(world, org.mockito.Mockito.never()).notifyBlockOfNeighborChange(anyInt(), anyInt(), anyInt(), any());
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
