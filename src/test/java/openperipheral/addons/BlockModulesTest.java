package openperipheral.addons;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.crafting.IRecipe;

import org.junit.Assert;
import org.junit.Test;

public class BlockModulesTest {

    @Test
    public void moduleRecipesAreNoOpsWhenTheOptionalBlocksAreMissing() {
        final List<IRecipe> recipes = new ArrayList<IRecipe>();
        ModuleComputerCraft.registerRecipes(recipes);
        ModuleRailcraft.registerRecipes(recipes);
        Assert.assertTrue(recipes.isEmpty());
    }
}
