package openperipheral.addons.glasses;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;

public class TerminalAddonRecipeTest {

    private static final class TestHelmet extends ItemArmor {

        private TestHelmet() {
            super(ArmorMaterial.IRON, 0, 0);
        }
    }

    private static InventoryCrafting craftingGrid(int size) {
        return new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, size, 1);
    }

    @Test
    public void matchesGlassesPlusHelmet() {
        final InventoryCrafting grid = craftingGrid(2);
        final ItemStack glasses = new ItemStack(new ItemGlasses());
        grid.setInventorySlotContents(0, glasses);
        grid.setInventorySlotContents(1, new ItemStack(new TestHelmet()));

        final TerminalAddonRecipe recipe = new TerminalAddonRecipe();
        Assert.assertTrue(recipe.matches(grid, null));
        Assert.assertEquals(2, recipe.getRecipeSize());
        Assert.assertNull(recipe.getRecipeOutput());
    }

    @Test
    public void copiesGlassesGuidToCraftedHelmet() {
        final InventoryCrafting grid = craftingGrid(2);
        final ItemGlasses glassesItem = new ItemGlasses();
        final ItemStack glasses = new ItemStack(glassesItem);
        glassesItem.bindToTerminal(glasses, 1234L);
        grid.setInventorySlotContents(0, glasses);
        grid.setInventorySlotContents(1, new ItemStack(new TestHelmet()));

        final TerminalAddonRecipe recipe = new TerminalAddonRecipe();
        final ItemStack result = recipe.getCraftingResult(grid);
        Assert.assertNotNull(result);
        Assert.assertEquals(
            Long.valueOf(1234L),
            NbtGuidProviders.getTerminalGuid(result)
                .orNull());
    }

    @Test
    public void rejectsMissingOrExtraItems() {
        final TerminalAddonRecipe recipe = new TerminalAddonRecipe();

        final InventoryCrafting empty = craftingGrid(2);
        Assert.assertFalse(recipe.matches(empty, null));

        final InventoryCrafting onlyGlasses = craftingGrid(2);
        onlyGlasses.setInventorySlotContents(0, new ItemStack(new ItemGlasses()));
        Assert.assertFalse(recipe.matches(onlyGlasses, null));

        final InventoryCrafting twoGlasses = craftingGrid(2);
        twoGlasses.setInventorySlotContents(0, new ItemStack(new ItemGlasses()));
        twoGlasses.setInventorySlotContents(1, new ItemStack(new ItemGlasses()));
        Assert.assertFalse(recipe.matches(twoGlasses, null));

        final InventoryCrafting alreadyTerminal = craftingGrid(2);
        alreadyTerminal.setInventorySlotContents(0, new ItemStack(new ItemGlasses()));
        final ItemStack helmet = new ItemStack(new TestHelmet());
        NbtGuidProviders.setTerminalGuid(helmet, Optional.of(5L));
        alreadyTerminal.setInventorySlotContents(1, helmet);
        Assert.assertFalse(recipe.matches(alreadyTerminal, null));
    }
}
