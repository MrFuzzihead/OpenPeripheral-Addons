package openperipheral.addons.sensors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mojang.authlib.GameProfile;

import openperipheral.addons.OpcAccess;
import openperipheral.addons.sensors.AdapterSensor.SupportedEntityTypes;
import openperipheral.api.meta.IEntityPartialMetaBuilder;
import openperipheral.api.meta.IMetaProviderProxy;

public class AdapterSensorTest {

    private AdapterSensor adapter;
    private ISensorEnvironment environment;
    private World world;
    private IEntityPartialMetaBuilder metaBuilder;

    @Before
    public void setup() {
        adapter = new AdapterSensor();
        world = mock(World.class);
        environment = mock(ISensorEnvironment.class);
        when(environment.getWorld()).thenReturn(world);
        when(environment.getLocation()).thenReturn(Vec3.createVectorHelper(10, 20, 30));
        when(environment.getSensorRange()).thenReturn(5);
        when(environment.isTurtle()).thenReturn(false);

        metaBuilder = mock(IEntityPartialMetaBuilder.class);
        OpcAccess.entityMetaBuilder = metaBuilder;
        AdapterSensor.colorResolver = new AdapterSensor.ColorResolver() {

            @Override
            public int resolve(int color) {
                return 1;
            }
        };
    }

    @Test
    public void scanRangeIncludesTheSphereButNotTheSensor() {
        final int rangeSq = 9;
        Assert.assertFalse(AdapterSensor.isInScanRange(0, 0, 0, rangeSq));
        Assert.assertTrue(AdapterSensor.isInScanRange(3, 0, 0, rangeSq));
        Assert.assertTrue(AdapterSensor.isInScanRange(0, 3, 0, rangeSq));
        Assert.assertTrue(AdapterSensor.isInScanRange(0, 0, 3, rangeSq));
        Assert.assertTrue(AdapterSensor.isInScanRange(2, 2, 1, rangeSq));
        Assert.assertFalse(AdapterSensor.isInScanRange(4, 0, 0, rangeSq));
        Assert.assertFalse(AdapterSensor.isInScanRange(3, 3, 0, rangeSq));
    }

    @Test
    public void zeroRangeOnlyContainsTheSensor() {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Assert.assertFalse(AdapterSensor.isInScanRange(x, y, z, 0));
                }
            }
        }
    }

    @Test
    public void supportedTypesMapToExpectedEntities() {
        Assert.assertEquals(EntityLiving.class, SupportedEntityTypes.MOB.cls);
        Assert.assertEquals(EntityMinecart.class, SupportedEntityTypes.MINECART.cls);
        Assert.assertEquals(EntityItem.class, SupportedEntityTypes.ITEM.cls);
        Assert.assertEquals(EntityItemFrame.class, SupportedEntityTypes.ITEM_FRAME.cls);
        Assert.assertEquals(EntityPainting.class, SupportedEntityTypes.PAINTING.cls);
    }

    @Test
    public void sonicScanDescribesSolidLiquidAirAndUnknownBlocks() {
        final Block solid = mock(Block.class);
        when(solid.getMaterial()).thenReturn(Material.rock);
        when(solid.getMapColor(anyInt())).thenReturn(MapColor.stoneColor);
        when(solid.getDamageValue(any(World.class), anyInt(), anyInt(), anyInt())).thenReturn(0);

        when(world.blockExists(anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(solid);
        when(world.isAirBlock(anyInt(), anyInt(), anyInt())).thenReturn(false);

        final List<Map<String, Object>> scan = adapter.sonicScan(environment);
        Assert.assertFalse(scan.isEmpty());
        for (Map<String, Object> entry : scan) {
            Assert.assertEquals("SOLID", entry.get("type"));
            Assert.assertNotNull(entry.get("color"));
        }

        // Second pass exercises the color cache.
        adapter.sonicScan(environment);

        final Map<String, Object> target = adapter.sonicScanTarget(environment, 0, -1, 0);
        Assert.assertNotNull(target);
        Assert.assertEquals("SOLID", target.get("type"));
        Assert.assertNull(adapter.sonicScanTarget(environment, 0, 0, 0));
        Assert.assertNull(adapter.sonicScanTarget(environment, 20, 0, 0));

        final Block liquid = mock(Block.class);
        when(liquid.getMaterial()).thenReturn(Material.water);
        when(liquid.getMapColor(anyInt())).thenReturn(MapColor.waterColor);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(liquid);
        Assert.assertEquals(
            "LIQUID",
            adapter.sonicScanTarget(environment, 0, -1, 0)
                .get("type"));

        final Block unknown = mock(Block.class);
        when(unknown.getMaterial()).thenReturn(Material.plants);
        when(unknown.getMapColor(anyInt())).thenReturn(MapColor.ironColor);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(unknown);
        when(world.isAirBlock(anyInt(), anyInt(), anyInt())).thenReturn(false);
        Assert.assertEquals(
            "UNKNOWN",
            adapter.sonicScanTarget(environment, 0, -1, 0)
                .get("type"));

        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(null);
        Assert.assertEquals(
            "AIR",
            adapter.sonicScanTarget(environment, 0, -1, 0)
                .get("type"));

        when(world.blockExists(anyInt(), anyInt(), anyInt())).thenReturn(false);
        Assert.assertNull(adapter.sonicScanTarget(environment, 0, -1, 0));
    }

    @Test
    public void listEntityIdsUsesTheWorldQuery() {
        final EntityLiving mob = mock(EntityLiving.class);
        when(mob.getEntityId()).thenReturn(42);
        when(world.getEntitiesWithinAABB(eq(EntityLiving.class), any(AxisAlignedBB.class)))
            .thenReturn(Collections.singletonList(mob));

        final List<Integer> ids = adapter.getEntityIds(environment, SupportedEntityTypes.MOB);
        Assert.assertEquals(Collections.singletonList(42), ids);
        Assert.assertEquals(Collections.singletonList(42), adapter.getMobIds(environment));

        when(world.getEntitiesWithinAABB(eq(EntityMinecart.class), any(AxisAlignedBB.class)))
            .thenReturn(Collections.emptyList());
        Assert.assertTrue(
            adapter.getMinecartIds(environment)
                .isEmpty());
    }

    @Test
    public void entityAndPlayerDetailsAreRangeChecked() {
        final EntityLiving mob = mock(EntityLiving.class);
        setField(mob, "boundingBox", AxisAlignedBB.getBoundingBox(10, 20, 30, 11, 21, 31));
        when(mob.getEntityId()).thenReturn(42);
        when(world.getEntityByID(42)).thenReturn(mob);
        final IMetaProviderProxy proxy = mock(IMetaProviderProxy.class);
        when(metaBuilder.createProxy(eq(mob), any(Vec3.class))).thenReturn(proxy);

        Assert.assertSame(proxy, adapter.getEntityData(environment, 42, SupportedEntityTypes.MOB));
        Assert.assertSame(proxy, adapter.getMobData(environment, 42));

        final EntityPlayer player = mock(EntityPlayer.class);
        setField(player, "boundingBox", AxisAlignedBB.getBoundingBox(10, 20, 30, 11, 21, 31));
        final GameProfile profile = new GameProfile(UUID.randomUUID(), "player");
        when(player.getGameProfile()).thenReturn(profile);
        when(world.getPlayerEntityByName("player")).thenReturn(player);
        when(world.func_152378_a(profile.getId())).thenReturn(player);
        when(metaBuilder.createProxy(eq(player), any(Vec3.class))).thenReturn(proxy);

        Assert.assertSame(proxy, adapter.getPlayerByName(environment, "player"));
        Assert.assertSame(proxy, adapter.getPlayerByUUID(environment, profile.getId()));

        final List<EntityPlayer> players = new ArrayList<EntityPlayer>();
        players.add(player);
        when(world.getEntitiesWithinAABB(eq(EntityPlayer.class), any(AxisAlignedBB.class))).thenReturn(players);
        Assert.assertEquals(Collections.singletonList(profile), adapter.getPlayers(environment));
    }

    private static void setField(Object target, String name, Object value) {
        try {
            final Field field = net.minecraft.entity.Entity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void invalidEntityLookupsThrow() {
        when(world.getEntityByID(1)).thenReturn(null);
        try {
            adapter.getEntityData(environment, 1, SupportedEntityTypes.MOB);
            Assert.fail("Missing entity must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        final EntityLiving mob = mock(EntityLiving.class);
        setField(mob, "boundingBox", AxisAlignedBB.getBoundingBox(100, 100, 100, 101, 101, 101));
        when(world.getEntityByID(2)).thenReturn(mob);
        try {
            adapter.getEntityData(environment, 2, SupportedEntityTypes.MOB);
            Assert.fail("Out-of-range entity must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
