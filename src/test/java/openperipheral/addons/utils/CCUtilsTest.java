package openperipheral.addons.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;

public class CCUtilsTest {

    @Test
    public void turtleIsInvalidWithoutAWorld() {
        final ITurtleAccess turtle = mock(ITurtleAccess.class);
        when(turtle.getWorld()).thenReturn(null);
        Assert.assertFalse(CCUtils.isTurtleValid(turtle));
    }

    @Test
    public void upgradedTurtleListUsesTheFiveToolUpgradeIds() {
        final ITurtleUpgrade upgrade = mock(ITurtleUpgrade.class);
        final ITurtleUpgrade tool = mock(ITurtleUpgrade.class);
        try (MockedStatic<ComputerCraft> cc = Mockito.mockStatic(ComputerCraft.class);
            MockedStatic<TurtleItemFactory> factory = Mockito.mockStatic(TurtleItemFactory.class)) {
            cc.when(() -> ComputerCraft.getTurtleUpgrade(Mockito.anyInt()))
                .thenReturn(tool);
            factory
                .when(
                    () -> TurtleItemFactory.create(
                        Mockito.anyInt(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(ComputerFamily.class),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.anyInt(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(new ItemStack(new Item()));

            final List<ItemStack> turtles = new ArrayList<ItemStack>();
            CCUtils.addUpgradedTurtles(turtles, upgrade);
            Assert.assertEquals(12, turtles.size());

            for (int id : new int[] { 3, 4, 5, 6, 7 }) {
                cc.verify(() -> ComputerCraft.getTurtleUpgrade(id), Mockito.atLeastOnce());
            }
            cc.verify(() -> ComputerCraft.getTurtleUpgrade(1), Mockito.never());
            cc.verify(() -> ComputerCraft.getTurtleUpgrade(2), Mockito.never());
        }
    }

    @Test
    public void turtleValidityFollowsTheWorldChunk() {
        final ITurtleAccess turtle = mock(ITurtleAccess.class);
        final World world = mock(World.class);
        when(turtle.getWorld()).thenReturn(world);
        when(turtle.getPosition()).thenReturn(new ChunkCoordinates(1, 2, 3));

        when(world.blockExists(1, 2, 3)).thenReturn(false);
        Assert.assertFalse(CCUtils.isTurtleValid(turtle));

        when(world.blockExists(1, 2, 3)).thenReturn(true);
        Assert.assertTrue(CCUtils.isTurtleValid(turtle));
        verify(turtle, org.mockito.Mockito.atLeastOnce()).getPosition();
    }
}
