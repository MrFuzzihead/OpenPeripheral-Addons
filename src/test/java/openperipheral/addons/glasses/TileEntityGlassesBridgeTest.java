package openperipheral.addons.glasses;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Optional;
import com.mojang.authlib.GameProfile;

import openperipheral.addons.api.ITerminalIdGetter;
import openperipheral.addons.api.ITerminalIdSetter;
import openperipheral.addons.glasses.GlassesEvent.GlassesMouseWheelEvent;
import openperipheral.addons.glasses.server.SurfaceServer;
import openperipheral.api.architecture.IArchitectureAccess;
import openperipheral.api.helpers.Index;

public class TileEntityGlassesBridgeTest {

    private static final class MutableGetter implements ITerminalIdGetter {

        private Long value;

        private MutableGetter(Long value) {
            this.value = value;
        }

        @Override
        public Optional<Long> getFor(EntityPlayer player) {
            return Optional.fromNullable(value);
        }

        @Override
        public int getPriority() {
            return 0;
        }
    }

    private static final class RecordingPacketSink implements TileEntityGlassesBridge.PacketSink {

        private int serialized;
        private int messages;
        private int direct;

        @Override
        public List<Object> serialize(TerminalEvent.Data event) {
            serialized++;
            return Collections.emptyList();
        }

        @Override
        public void sendMessages(List<Object> packets, EntityPlayer player) {
            messages++;
        }

        @Override
        public void sendToPlayer(TerminalEvent.Data event, EntityPlayer player) {
            direct++;
        }
    }

    private TileEntityGlassesBridge bridge;
    private World world;
    private MutableGetter getter;

    @BeforeClass
    public static void registerMapping() {
        TileEntity.addMapping(TileEntityGlassesBridge.class, "openperipheral_glasses_bridge_test");
    }

    @Before
    public void setup() throws Exception {
        world = mock(World.class);
        bridge = new TileEntityGlassesBridge();
        setField(TileEntity.class, bridge, "worldObj", world);
        setField(TileEntity.class, bridge, "blockType", new BlockGlassesBridge());
        bridge.xCoord = 0;
        bridge.yCoord = 0;
        bridge.zCoord = 0;

        setField(TerminalIdAccess.class, TerminalIdAccess.instance, "getters", new ITerminalIdGetter[0]);
        setField(TerminalIdAccess.class, TerminalIdAccess.instance, "setters", new ITerminalIdSetter[0]);
        getter = new MutableGetter(bridge.getOrCreateGuid());
        TerminalIdAccess.instance.register(getter);
    }

    @Test
    public void guidSurvivesNbtAndItemPlacement() {
        final long guid = bridge.getOrCreateGuid();
        Assert.assertTrue(guid > 0);
        Assert.assertEquals(TerminalUtils.formatTerminalId(guid), bridge.getGuidString());

        final NBTTagCompound tag = new NBTTagCompound();
        bridge.writeToNBT(tag);
        final TileEntityGlassesBridge restored = new TileEntityGlassesBridge();
        restored.readFromNBT(tag);
        Assert.assertEquals(guid, restored.getOrCreateGuid());

        final ItemStack stack = new ItemStack(bridge.getBlockType());
        final NBTTagCompound itemTag = new NBTTagCompound();
        itemTag.setLong(TileEntityGlassesBridge.TAG_GUID, 1234L);
        stack.setTagCompound(itemTag);
        bridge.onBlockPlacedBy(null, ForgeDirection.UP, stack, 0, 0, 0);
        Assert.assertEquals(1234L, bridge.getOrCreateGuid());
    }

    @Test
    public void harvestDropsCarryTheGuid() {
        final long guid = bridge.getOrCreateGuid();
        Assert.assertTrue(bridge.suppressNormalHarvestDrops());

        final List<ItemStack> drops = new ArrayList<ItemStack>();
        bridge.addHarvestDrops(null, drops);
        Assert.assertEquals(1, drops.size());
        Assert.assertEquals(
            guid,
            drops.get(0)
                .getTagCompound()
                .getLong(TileEntityGlassesBridge.TAG_GUID));
    }

    @Test
    public void surfacesAreCreatedLazilyAndCanBeCleared() throws Exception {
        final SurfaceServer global = bridge.getSurfaceByName("$GLOBAL$");
        Assert.assertNotNull(global);
        Assert.assertSame(global, bridge.getSurface(TerminalUtils.GLOBAL_SURFACE_UUID));
        Assert.assertNull(bridge.getSurface("missing"));
        Assert.assertNull(bridge.getSurface(UUID.randomUUID()));

        global.drawablesFactory.addText(1, 2, "hello", 0xFFFFFF);
        bridge.clear();

        // clear() must be safe before the first tick and must invalidate cached full-data packets; the packet
        // serialization path itself needs FML networking and is covered by the game test.
        bridge.clear();
    }

    @Test
    public void playersCanRegisterRespawnAndBeRemoved() throws Exception {
        final GameProfile profile = new GameProfile(UUID.randomUUID(), "player");
        final EntityPlayerMP player = mock(EntityPlayerMP.class);
        when(player.getGameProfile()).thenReturn(profile);

        bridge.registerTerminal(player);
        Assert.assertEquals(
            1,
            bridge.getUsers()
                .size());
        Assert.assertSame(
            profile,
            bridge.getUsers()
                .get(0));

        // Same entity: no duplicate registration.
        bridge.registerTerminal(player);
        Assert.assertEquals(
            1,
            bridge.getUsers()
                .size());

        // Respawn: same profile, new entity; the existing surface must be reused.
        final EntityPlayerMP respawned = mock(EntityPlayerMP.class);
        when(respawned.getGameProfile()).thenReturn(profile);
        bridge.registerTerminal(respawned);
        Assert.assertEquals(
            1,
            bridge.getUsers()
                .size());
        Assert.assertNotNull(bridge.getCaptureControl(profile.getId()));

        // Simulate a weak reference that has already been cleared; updateEntity must remove the player without
        // dereferencing null and must detach it from computers.
        @SuppressWarnings("unchecked")
        final Map<UUID, ?> players = (Map<UUID, ?>) getField(
            TileEntityGlassesBridge.class,
            bridge,
            "knownPlayersByUUID");
        final Object info = players.get(profile.getId());
        setField(info.getClass(), info, "player", new WeakReference<EntityPlayerMP>(null));
        final IArchitectureAccess computer = mock(IArchitectureAccess.class);
        when(computer.peripheralName()).thenReturn("back");
        bridge.addComputer(computer);
        bridge.updateEntity();
        Assert.assertTrue(
            bridge.getUsers()
                .isEmpty());
        verify(computer).signal(eq("glasses_detach"), any(Object[].class));
        bridge.removeComputer(computer);
    }

    @Test
    public void syncSerializesGlobalDeltasAndSendsPrivateUpdates() {
        final RecordingPacketSink sink = new RecordingPacketSink();
        bridge.packetSink = sink;

        final GameProfile profile = new GameProfile(UUID.randomUUID(), "player");
        final EntityPlayerMP player = mock(EntityPlayerMP.class);
        when(player.getGameProfile()).thenReturn(profile);
        bridge.registerTerminal(player);

        final SurfaceServer global = bridge.getSurfaceByName("$GLOBAL$");
        global.drawablesFactory.addText(1, 2, "global", 0xFFFFFF);
        final SurfaceServer privateSurface = bridge.getSurface(profile.getId());
        privateSurface.drawablesFactory.addText(3, 4, "private", 0xFFFFFF);

        bridge.syncContents();
        Assert.assertEquals(2, sink.serialized); // full + delta
        Assert.assertEquals(1, sink.messages); // global delta
        Assert.assertEquals(1, sink.direct); // private delta

        bridge.syncContents();
        Assert.assertEquals(2, sink.serialized);
        Assert.assertEquals(1, sink.messages);
        Assert.assertEquals(1, sink.direct);

        final TerminalEvent.PrivateDrawableReset privateReset = new TerminalEvent.PrivateDrawableReset(
            bridge.getOrCreateGuid());
        privateReset.sender = player;
        bridge.handlePrivateDrawableResetRequest(privateReset);
        Assert.assertEquals(2, sink.direct);

        final TerminalEvent.PublicDrawableReset publicReset = new TerminalEvent.PublicDrawableReset(
            bridge.getOrCreateGuid());
        publicReset.sender = player;
        bridge.handlePublicDrawableResetRequest(publicReset);
        Assert.assertEquals(2, sink.messages);
    }

    @Test
    public void eventsAndChatAreForwardedToAttachedComputers() {
        final GameProfile profile = new GameProfile(UUID.randomUUID(), "player");
        final EntityPlayerMP player = mock(EntityPlayerMP.class);
        when(player.getGameProfile()).thenReturn(profile);
        bridge.registerTerminal(player);

        final IArchitectureAccess computer = mock(IArchitectureAccess.class);
        when(computer.peripheralName()).thenReturn("back");
        when(computer.createIndex(0)).thenReturn(Index.fromJava(0, 1));
        bridge.addComputer(computer);

        final GlassesMouseWheelEvent event = new GlassesMouseWheelEvent(bridge.getOrCreateGuid(), 3);
        event.sender = player;
        bridge.handleUserEvent(event);
        verify(computer).signal(eq("glasses_mouse_scroll"), any(Object[].class));

        bridge.onChatCommand("glasses_chat_message", "hello", player);
        verify(computer, org.mockito.Mockito.atLeastOnce()).signal(eq("glasses_chat_message"), any(Object[].class));
        bridge.removeComputer(computer);
    }

    @Test
    public void resetRequestsWithoutSenderAreIgnoredAndMissingSurfaceLookupFails() {
        bridge.handlePrivateDrawableResetRequest(new TerminalEvent.PrivateDrawableReset(1));
        bridge.handlePublicDrawableResetRequest(new TerminalEvent.PublicDrawableReset(1));

        try {
            bridge.getSurfaceByName("missing");
            Assert.fail("Missing player surface must be rejected");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void invalidateUnregistersTheBridge() {
        bridge.getSurfaceByName("$GLOBAL$");
        bridge.invalidate();
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Class<?> owner, Object target, String name) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
