package openperipheral.addons.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.base.Optional;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import openmods.Log;
import openmods.inventory.GenericInventory;
import openperipheral.addons.MetasGeneric;
import openperipheral.addons.OpcAccess;
import openperipheral.addons.OpenPeripheralAddons;
import openperipheral.addons.glasses.GlassesNetworkEvents;
import openperipheral.addons.glasses.ItemGlasses;
import openperipheral.addons.glasses.NbtGuidProviders;
import openperipheral.addons.glasses.TerminalAddonRecipe;
import openperipheral.addons.glasses.TerminalEvent;
import openperipheral.addons.glasses.TerminalUtils;
import openperipheral.addons.glasses.TileEntityGlassesBridge;
import openperipheral.addons.glasses.drawable.Drawable;
import openperipheral.addons.glasses.server.SurfaceServer;
import openperipheral.addons.pim.TileEntityPIM;
import openperipheral.addons.selector.TileEntitySelector;
import openperipheral.addons.sensors.AdapterSensor;
import openperipheral.addons.sensors.TileEntitySensor;
import openperipheral.addons.ticketmachine.TileEntityTicketMachine;
import openperipheral.api.helpers.Index;

@Mod(
    modid = GameTestMod.MODID,
    name = "OpenPeripheral Addons Game Tests",
    version = "1.0",
    acceptableRemoteVersions = "*")
public class GameTestMod {

    public static final String MODID = "openperipheraladdons_gametest";

    private static final boolean ENABLED = Boolean.getBoolean("openperipheraladdons.gameTest");

    private static final int TEST_Y = 100;

    @Mod.EventHandler
    public void init(FMLInitializationEvent evt) {
        if (!ENABLED) return;
        Log.info("OpenPeripheral Addons game tests enabled");
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent evt) {
        if (!ENABLED) return;

        try {
            final MinecraftServer server = FMLCommonHandler.instance()
                .getMinecraftServerInstance();
            final World world = server.worldServerForDimension(0);
            check(world != null, "Overworld is missing");

            testApi();
            testRegistration();
            testTerminalItems(world);
            testGlassesBridge(world);
            testSelector(world);
            testSensor(world);
            testPim(world);
            testNetworkEvents();
            if (Loader.isModLoaded("Railcraft")) testTicketMachine(world);

            Log.info("OPENPERIPHERAL_ADDONS_GAMETEST_OK");
            FMLCommonHandler.instance()
                .exitJava(0, false);
        } catch (Throwable t) {
            t.printStackTrace();
            Log.severe(t, "OPENPERIPHERAL_ADDONS_GAMETEST_FAILED");
            FMLCommonHandler.instance()
                .exitJava(1, false);
        }
    }

    private static void testApi() {
        check(OpcAccess.adapterRegistry != null, "Peripheral adapter registry is missing");
        check(OpcAccess.itemStackMetaBuilder != null, "ItemStack metadata builder is missing");
        check(OpcAccess.entityMetaBuilder != null, "Entity metadata builder is missing");
        OpcAccess.checkApiPresent();
    }

    private static void testRegistration() {
        check(OpenPeripheralAddons.Blocks.glassesBridge != null, "Glasses bridge block is missing");
        check(OpenPeripheralAddons.Blocks.pim != null, "PIM block is missing");
        check(OpenPeripheralAddons.Blocks.sensor != null, "Sensor block is missing");
        check(OpenPeripheralAddons.Blocks.selector != null, "Selector block is missing");
        check(OpenPeripheralAddons.Items.glasses != null, "Glasses item is missing");
        check(OpenPeripheralAddons.Items.keyboard != null, "Keyboard item is missing");
        check(OpenPeripheralAddons.Items.generic != null, "Generic item is missing");

        for (MetasGeneric meta : MetasGeneric.values()) {
            final ItemStack stack = meta.newItemStack();
            check(stack != null && stack.getItem() != null, "Meta item is missing: " + meta);
            check(meta.isA(stack), "Meta item does not identify itself: " + meta);
        }

        check(hasRecipeFor(Item.getItemFromBlock(OpenPeripheralAddons.Blocks.pim)), "PIM recipe is missing");
        check(hasRecipeFor(Item.getItemFromBlock(OpenPeripheralAddons.Blocks.sensor)), "Sensor recipe is missing");
        check(
            hasRecipeFor(Item.getItemFromBlock(OpenPeripheralAddons.Blocks.glassesBridge)),
            "Glasses bridge recipe is missing");
        check(hasRecipeFor(Item.getItemFromBlock(OpenPeripheralAddons.Blocks.selector)), "Selector recipe is missing");
        check(hasRecipeFor(OpenPeripheralAddons.Items.glasses), "Glasses recipe is missing");
        check(hasRecipeFor(OpenPeripheralAddons.Items.keyboard), "Keyboard recipe is missing");
        check(
            hasRecipeFor(
                MetasGeneric.duckAntenna.newItemStack()
                    .getItem()),
            "Duck antenna recipe is missing");

        if (Loader.isModLoaded("ComputerCraft")) {
            check(openperipheral.addons.ModuleComputerCraft.Blocks.peripheralProxy != null, "Proxy is missing");

            final List<ItemStack> turtles = new ArrayList<ItemStack>();
            openperipheral.addons.ModuleComputerCraft.listSensorTurtles(turtles);
            check(!turtles.isEmpty(), "Creative sensor turtle list is empty");
        }
    }

    private static boolean hasRecipeFor(Item item) {
        @SuppressWarnings("unchecked")
        final List<IRecipe> recipes = CraftingManager.getInstance()
            .getRecipeList();
        for (IRecipe recipe : recipes) {
            final ItemStack output = recipe.getRecipeOutput();
            if (output != null && output.getItem() == item) return true;
        }
        return false;
    }

    private static void testTerminalItems(World world) {
        final ItemGlasses glassesItem = OpenPeripheralAddons.Items.glasses;
        final ItemStack glasses = new ItemStack(glassesItem);
        final long guid = 0xABC123L;
        glassesItem.bindToTerminal(glasses, guid);
        check(
            Long.valueOf(guid)
                .equals(glassesItem.getTerminalGuid(glasses)),
            "Glasses did not store the terminal guid");

        final ItemStack helmet = new ItemStack(Items.diamond_helmet);
        NbtGuidProviders.setTerminalGuid(helmet, Optional.of(guid));
        check(
            Long.valueOf(guid)
                .equals(
                    NbtGuidProviders.getTerminalGuid(helmet)
                        .orNull()),
            "NBT terminal tag round-trip failed");

        final InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 2, 1);
        grid.setInventorySlotContents(0, glasses);
        grid.setInventorySlotContents(1, new ItemStack(Items.diamond_helmet));

        final TerminalAddonRecipe recipe = new TerminalAddonRecipe();
        check(recipe.matches(grid, world), "Terminal addon recipe did not match glasses + helmet");
        final ItemStack result = recipe.getCraftingResult(grid);
        check(result != null && result.getItem() == Items.diamond_helmet, "Terminal addon recipe result is wrong");
        check(
            Long.valueOf(guid)
                .equals(
                    NbtGuidProviders.getTerminalGuid(result)
                        .orNull()),
            "Terminal addon recipe did not copy the guid");
    }

    private static void testGlassesBridge(World world) {
        final int x = 0;
        final int z = 0;
        world.setBlock(x, TEST_Y, z, OpenPeripheralAddons.Blocks.glassesBridge, 0, 3);
        final TileEntity te = world.getTileEntity(x, TEST_Y, z);
        check(te instanceof TileEntityGlassesBridge, "Glasses bridge TE was not created: " + te);

        final TileEntityGlassesBridge bridge = (TileEntityGlassesBridge) te;
        bridge.updateEntity();

        final long guid = bridge.getOrCreateGuid();
        check(guid > 0, "Glasses bridge generated an invalid guid");
        check(
            TerminalUtils.formatTerminalId(guid)
                .equals(bridge.getGuidString()),
            "Bridge guid string does not match");

        final SurfaceServer global = bridge.getSurfaceByName("$GLOBAL$");
        check(global != null, "Global surface is null");
        check(global == bridge.getSurface(TerminalUtils.GLOBAL_SURFACE_UUID), "Global surface lookup is inconsistent");

        final Drawable text = global.drawablesFactory.addText(1, 2, "gametest", 0xFFFFFF);
        check(text != null, "Could not add a drawable");
        check(text.getId() == 0, "First drawable id was not zero");
        check("text".equals(text.getTypeName()), "Drawable type name is wrong");

        bridge.syncContents();
        check(
            bridge.getUsers()
                .isEmpty(),
            "Unexpected bridge users");

        bridge.handlePublicDrawableResetRequest(new TerminalEvent.PublicDrawableReset(guid));
        bridge.handlePrivateDrawableResetRequest(new TerminalEvent.PrivateDrawableReset(guid));
        bridge.clear();
        bridge.syncContents();
    }

    private static void testSelector(World world) {
        final int x = 10;
        final int z = 0;
        world.setBlock(x, TEST_Y, z, OpenPeripheralAddons.Blocks.selector, 0, 3);
        final TileEntity te = world.getTileEntity(x, TEST_Y, z);
        check(te instanceof TileEntitySelector, "Selector TE was not created: " + te);

        final TileEntitySelector selector = (TileEntitySelector) te;
        final ItemStack stack = new ItemStack(Items.diamond, 4);
        selector.setSlot(Index.fromJava(0, 1), stack);
        check(selector.getSlot(0) != null, "setSlot did not store the item");
        check(selector.getGridSize() == 1, "Grid size did not update after setSlot");

        final java.util.Map<Index, ItemStack> slots = new java.util.HashMap<Index, ItemStack>();
        slots.put(Index.fromJava(0, 1), new ItemStack(Items.diamond));
        slots.put(Index.fromJava(2, 1), new ItemStack(Items.emerald));
        selector.setSlots(new TestArchitecture(), slots);

        check(selector.getSlot(0) != null, "setSlots did not store slot 0");
        check(selector.getSlot(2) != null, "setSlots did not store slot 2");
        check(selector.getGridSize() == 3, "Grid size did not become 3x3");
        check(
            selector.getSlots()
                .size() == 9,
            "Selector does not expose nine slots");
    }

    private static final class TestArchitecture implements openperipheral.api.architecture.IArchitecture {

        @Override
        public String architecture() {
            return "gametest";
        }

        @Override
        public Object wrapObject(Object target) {
            return target;
        }

        @Override
        public Index createIndex(int value) {
            return Index.fromJava(value, 1);
        }

        @Override
        public openperipheral.api.converter.IConverter getConverter() {
            return null;
        }
    }

    private static void testSensor(World world) {
        final int x = 20;
        final int z = 0;
        world.setBlock(x, TEST_Y - 1, z, net.minecraft.init.Blocks.stone, 0, 3);
        world.setBlock(x, TEST_Y, z, OpenPeripheralAddons.Blocks.sensor, 0, 3);
        final TileEntity te = world.getTileEntity(x, TEST_Y, z);
        check(te instanceof TileEntitySensor, "Sensor TE was not created: " + te);

        final TileEntitySensor sensor = (TileEntitySensor) te;
        final AdapterSensor adapter = new AdapterSensor();

        final List<Map<String, Object>> scan = adapter.sonicScan(sensor);
        check(scan != null && !scan.isEmpty(), "Sonic scan returned no blocks");
        boolean foundStone = false;
        for (Map<String, Object> entry : scan) {
            if (Integer.valueOf(0)
                .equals(entry.get("x"))
                && Integer.valueOf(-1)
                    .equals(entry.get("y"))
                && Integer.valueOf(0)
                    .equals(entry.get("z"))) {
                check("SOLID".equals(entry.get("type")), "Stone was not classified as SOLID: " + entry);
                foundStone = true;
            }
        }
        check(foundStone, "Sonic scan did not include the block below the sensor");

        final Map<String, Object> target = adapter.sonicScanTarget(sensor, 0, -1, 0);
        check(target != null && "SOLID".equals(target.get("type")), "sonicScanTarget did not find stone");
        check(adapter.sonicScanTarget(sensor, 0, 0, 0) == null, "sonicScanTarget accepted the sensor block");
        check(adapter.sonicScanTarget(sensor, 20, 0, 0) == null, "sonicScanTarget accepted an out-of-range block");
        check(
            adapter.getEntityIds(sensor, AdapterSensor.SupportedEntityTypes.MOB) != null,
            "getEntityIds returned null");
        check(
            adapter.getPlayers(sensor)
                .isEmpty(),
            "Unexpected players near the sensor");
    }

    private static void testPim(World world) {
        final int x = 30;
        final int z = 0;
        world.setBlock(x, TEST_Y, z, OpenPeripheralAddons.Blocks.pim, 0, 3);
        final TileEntity te = world.getTileEntity(x, TEST_Y, z);
        check(te instanceof TileEntityPIM, "PIM TE was not created: " + te);

        final TileEntityPIM pim = (TileEntityPIM) te;
        check(!pim.hasPlayer(), "PIM claims a player without one");
        check(pim.getPlayer() == null, "PIM returned a player without one");
        check(pim.getSizeInventory() == 0, "PIM reports inventory slots without a player");
        check(pim.getStackInSlot(0) == null, "PIM returned a stack without a player");
        check("pim".equals(pim.getInventoryName()), "PIM fallback name is wrong");
        pim.updateEntity();
    }

    private static void testNetworkEvents() throws Exception {
        for (Class<? extends openmods.network.event.NetworkEvent> cls : GlassesNetworkEvents.ALL) {
            check(cls.getConstructor() != null, "Event has no public no-arg constructor: " + cls);
            check(cls.newInstance() != null, "Event cannot be instantiated: " + cls);
        }
    }

    private static void testTicketMachine(World world) {
        final int x = 40;
        final int z = 0;
        world.setBlock(x, TEST_Y, z, openperipheral.addons.ModuleRailcraft.Blocks.ticketMachine, 0, 3);
        final TileEntity te = world.getTileEntity(x, TEST_Y, z);
        check(te instanceof TileEntityTicketMachine, "Ticket machine TE was not created: " + te);

        final TileEntityTicketMachine machine = (TileEntityTicketMachine) te;
        final GenericInventory inventory = (GenericInventory) machine.getInventory();
        inventory.setInventorySlotContents(0, new ItemStack(Items.paper, 5));

        ItemStack ink = null;
        for (ItemStack candidate : OreDictionary.getOres("dyeBlack")) {
            if (inventory.isItemValidForSlot(1, candidate)) {
                ink = candidate.copy();
                break;
            }
        }
        check(ink != null, "No black ink accepted by the ticket machine");
        ink.stackSize = 5;
        inventory.setInventorySlotContents(1, ink);

        check(machine.createTicket("gametest", 3), "createTicket returned false");
        final ItemStack output = inventory.getStackInSlot(2);
        check(output != null && output.stackSize == 3, "createTicket did not produce three tickets: " + output);
        check(inventory.getStackInSlot(0).stackSize == 2, "Paper was not consumed correctly");
        check(inventory.getStackInSlot(1).stackSize == 2, "Ink was not consumed correctly");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
