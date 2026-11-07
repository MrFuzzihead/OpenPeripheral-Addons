package openperipheral.addons.utils;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;

public final class CCUtils {

    private static final int NUMBER_OF_TURTLE_TOOLS = 7;

    private static final ComputerFamily[] TURTLE_FAMILIES = { ComputerFamily.Normal, ComputerFamily.Advanced };

    public static Object[] wrap(Object... args) {
        return args;
    }

    /**
     * Adds one turtle stack with the given upgrades equipped.
     * <p>
     * {@link TurtleItemFactory} is ComputerCraft's own way of building these stacks, used by its creative tab. It
     * encodes the upgrade ids in the exact tags the turtle item reads back, so this addon no longer has to write
     * {@code leftUpgrade}/{@code rightUpgrade} itself or look the turtle items up by their registry names.
     */
    private static void addTurtleVariant(List<ItemStack> result, ComputerFamily family, ITurtleUpgrade leftUpgrade,
        ITurtleUpgrade rightUpgrade) {
        final ItemStack turtle = TurtleItemFactory
            .create(-1, null, null, family, leftUpgrade, rightUpgrade, 0, null, null);
        if (turtle != null) result.add(turtle);
    }

    public static void addUpgradedTurtles(List<ItemStack> result, ITurtleUpgrade upgrade) {
        for (ComputerFamily family : TURTLE_FAMILIES) {
            addTurtleVariant(result, family, upgrade, null);

            for (int toolId = 1; toolId < NUMBER_OF_TURTLE_TOOLS; toolId++) {
                addTurtleVariant(result, family, upgrade, ComputerCraft.getTurtleUpgrade(toolId));
            }
        }
    }

    public static boolean isTurtleValid(ITurtleAccess access) {
        World world = access.getWorld();
        if (world == null) return false;
        ChunkCoordinates coords = access.getPosition();
        return world.blockExists(coords.posX, coords.posY, coords.posZ);
    }

}
