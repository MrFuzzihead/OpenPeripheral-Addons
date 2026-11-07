package openperipheral.addons.peripheralproxy;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import openmods.api.INeighbourAwareTile;
import openmods.api.INeighbourTeAwareTile;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.Coord;
import openperipheral.api.architecture.cc.ICustomPeripheralProvider;

public class TileEntityPeripheralProxy extends OpenTileEntity
    implements ICustomPeripheralProvider, INeighbourAwareTile, INeighbourTeAwareTile {

    private Block attachedBlock;

    @Override
    public IPeripheral createPeripheral(int side) {
        final ForgeDirection rotation = getOrientation().up();
        if (rotation.getOpposite()
            .ordinal() != side) return null;

        final int targetX = xCoord + rotation.offsetX;
        final int targetY = yCoord + rotation.offsetY;
        final int targetZ = zCoord + rotation.offsetZ;

        final IPeripheral peripheral = ComputerCraft.getPeripheralAt(worldObj, targetX, targetY, targetZ, 0);
        if (peripheral != null) {
            attachedBlock = worldObj.getBlock(targetX, targetY, targetZ);
            return new WrappedPeripheral(peripheral);
        } else {
            attachedBlock = null;
            return null;
        }
    }

    @Override
    public void onNeighbourChanged(Block block) {
        if (!worldObj.isRemote && (block == null || block == attachedBlock)) refreshModem();
    }

    @Override
    public void onNeighbourTeChanged(int x, int y, int z) {
        if (worldObj.isRemote) return;
        if (getPosition().offset(getOrientation().up())
            .equals(new Coord(x, y, z))) refreshModem();
    }

    /**
     * Tells the modem this proxy is plugged into that the network's peripheral list may have changed. The modem caches
     * that list and only rebuilds it when a block adjacent to the <em>modem</em> changes, so a block appearing or
     * vanishing on the far side of this proxy has to be reported explicitly.
     * <p>
     * {@link TileCable#networkChanged()} only invalidates the cache - the modem's own tick does the rescan - so this is
     * safe on every neighbour update, and it leaves the peripheral's assigned id alone.
     */
    private void refreshModem() {
        final ForgeDirection modemDir = getOrientation().up()
            .getOpposite();
        final int modemX = xCoord + modemDir.offsetX;
        final int modemY = yCoord + modemDir.offsetY;
        final int modemZ = zCoord + modemDir.offsetZ;

        final TileEntity modemTile = worldObj.getTileEntity(modemX, modemY, modemZ);
        // A modem that is not exposing its peripheral is left alone: enabling it is the player's decision, and
        // ComputerCraft refuses to enable one until it has something to expose anyway.
        if (modemTile instanceof TileCable && ((TileCable) modemTile).isPeripheralAccessAllowed()) {
            ((TileCable) modemTile).networkChanged();
        }

        worldObj.notifyBlockOfNeighborChange(modemX, modemY, modemZ, getBlockType());
    }
}
