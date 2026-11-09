package openperipheral.addons.pim;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mojang.authlib.GameProfile;

import openperipheral.api.architecture.IArchitectureAccess;

public class TileEntityPIMTest {

    private TileEntityPIM pim;
    private World world;
    private EntityPlayer player;
    private InventoryPlayer inventory;

    @Before
    public void setup() throws Exception {
        world = mock(World.class);
        when(world.getBlockMetadata(anyInt(), anyInt(), anyInt())).thenReturn(0);

        pim = new TileEntityPIM();
        setField(TileEntity.class, pim, "worldObj", world);
        pim.xCoord = 0;
        pim.yCoord = 0;
        pim.zCoord = 0;

        inventory = mock(InventoryPlayer.class);
        player = mock(EntityPlayer.class);
        when(player.getCommandSenderName()).thenReturn("player");
        when(player.getGameProfile()).thenReturn(new GameProfile(UUID.randomUUID(), "player"));
        player.posX = 0.5;
        player.posY = 0.0;
        player.posZ = 0.5;
        final Field inv = EntityPlayer.class.getField("inventory");
        inv.setAccessible(true);
        inv.set(player, inventory);
    }

    @Test
    public void occupiesTheBlockWhenAValidPlayerStepsOnIt() {
        final IArchitectureAccess computer = mock(IArchitectureAccess.class);
        pim.addComputer(computer);

        pim.trySetPlayer(player);

        Assert.assertSame(player, pim.getPlayer());
        verify(world).playSoundEffect(0.5, 0.1, 0.5, "random.click", 0.3F, 0.6F);
        verify(world).setBlockMetadataWithNotify(0, 0, 0, 1, 3);
        verify(computer).signal(eq("player_on"), any(Object[].class));
    }

    @Test
    public void delegatesThePlayerInventory() {
        pim.trySetPlayer(player);

        when(inventory.getSizeInventory()).thenReturn(36);
        when(inventory.getStackInSlot(3)).thenReturn(new ItemStack(new Item()));
        when(inventory.decrStackSize(3, 2)).thenReturn(new ItemStack(new Item(), 2));
        when(inventory.getInventoryStackLimit()).thenReturn(64);

        Assert.assertEquals(36, pim.getSizeInventory());
        Assert.assertNotNull(pim.getStackInSlot(3));
        Assert.assertNotNull(pim.decrStackSize(3, 2));
        Assert.assertEquals(64, pim.getInventoryStackLimit());
        Assert.assertEquals("player", pim.getInventoryName());
        Assert.assertTrue(pim.isItemValidForSlot(0, new ItemStack(new Item())));
        Assert.assertTrue(pim.isUseableByPlayer(player));

        final ItemStack stack = new ItemStack(new Item());
        pim.setInventorySlotContents(4, stack);
        verify(inventory).setInventorySlotContents(4, stack);

        pim.openInventory();
        pim.closeInventory();
        pim.markDirty();
        Assert.assertNull(pim.getStackInSlotOnClosing(0));
        Assert.assertFalse(pim.hasCustomInventoryName());
    }

    @Test
    public void emptyPimHasNoInventoryOrPlayer() {
        Assert.assertNull(pim.getPlayer());
        Assert.assertEquals(0, pim.getSizeInventory());
        Assert.assertNull(pim.getStackInSlot(0));
        Assert.assertNull(pim.decrStackSize(0, 1));
        Assert.assertEquals(0, pim.getInventoryStackLimit());
        Assert.assertFalse(pim.isItemValidForSlot(0, null));
        Assert.assertEquals("pim", pim.getInventoryName());
    }

    @Test
    public void updateEntityClearsAPlayerThatWalkedAway() {
        pim.trySetPlayer(player);
        player.posX = 10;

        pim.updateEntity();

        verify(world).setBlockMetadataWithNotify(0, 0, 0, 0, 3);
        Assert.assertNull(pim.getPlayer());
    }

    @Test
    public void updateEntityClearsStaleMetadataWhenTheWeakReferenceIsGone() throws Exception {
        when(world.getBlockMetadata(0, 0, 0)).thenReturn(1);
        setField(TileEntityPIM.class, pim, "player", new WeakReference<EntityPlayer>(null));

        pim.updateEntity();

        verify(world).setBlockMetadataWithNotify(0, 0, 0, 0, 3);
    }

    @Test
    public void computersCanBeAddedAndRemoved() {
        final IArchitectureAccess computer = mock(IArchitectureAccess.class);
        pim.addComputer(computer);
        pim.removeComputer(computer);
        pim.removeComputer(computer);
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
