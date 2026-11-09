package openperipheral.addons.glasses.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.common.base.Optional;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.gameevent.TickEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesChangeBackgroundEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetDragParamsEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetGuiVisibilityEvent;
import openperipheral.addons.glasses.GuiCapture;
import openperipheral.addons.glasses.NbtGuidProviders;
import openperipheral.addons.glasses.TerminalEvent;
import openperipheral.addons.utils.GuiElements;

public class TerminalManagerClientTest {

    @Test
    public void itemTooltipShowsBoundAndUnboundTerminalState() {
        final TerminalManagerClient.ForgeBusListener listener = (TerminalManagerClient.ForgeBusListener) TerminalManagerClient.instance
            .createForgeBusListener();

        final ItemStack stack = new ItemStack(new net.minecraft.item.Item());
        final List<String> tooltip = new ArrayList<String>();
        listener.onItemTooltip(new ItemTooltipEvent(stack, null, tooltip, false));
        Assert.assertTrue(tooltip.isEmpty());

        NbtGuidProviders.setTerminalGuid(stack, Optional.of(123L));
        listener.onItemTooltip(new ItemTooltipEvent(stack, null, tooltip, false));
        Assert.assertEquals(1, tooltip.size());
    }

    @Test
    public void drawableDataAndClearLifecycleUsesTheSurfaceTable() {
        final TerminalManagerClient.ForgeBusListener listener = (TerminalManagerClient.ForgeBusListener) TerminalManagerClient.instance
            .createForgeBusListener();
        final TerminalEvent.PublicDrawableData data = new TerminalEvent.PublicDrawableData(88L);
        listener.onTerminalData(data);
        final Minecraft minecraft = mock(Minecraft.class);
        minecraft.displayWidth = 800;
        minecraft.displayHeight = 600;
        final net.minecraft.client.settings.GameSettings settings = mock(
            net.minecraft.client.settings.GameSettings.class);
        settings.guiScale = 1;
        minecraft.gameSettings = settings;
        Assert.assertNull(
            TerminalManagerClient.instance.findDrawableHit(88L, new ScaledResolution(minecraft, 800, 600), 10, 10));
        listener.onTerminalClear(new TerminalEvent.PublicClear(88L));
    }

    @Test
    public void captureSettingsAreForwardedToTheOpenCaptureScreen() {
        final Minecraft minecraft = mock(Minecraft.class);
        final GuiCapture capture = new GuiCapture(42L);
        minecraft.currentScreen = capture;
        final FMLClientHandler handler = mock(FMLClientHandler.class);
        when(handler.getClient()).thenReturn(minecraft);

        try (MockedStatic<FMLClientHandler> staticHandler = Mockito.mockStatic(FMLClientHandler.class)) {
            staticHandler.when(FMLClientHandler::instance)
                .thenReturn(handler);
            final TerminalManagerClient.ForgeBusListener listener = (TerminalManagerClient.ForgeBusListener) TerminalManagerClient.instance
                .createForgeBusListener();

            listener.onBackgroundChange(new GlassesChangeBackgroundEvent(42L, 0x112233));
            final Map<GuiElements, Boolean> visibility = new EnumMap<GuiElements, Boolean>(GuiElements.class);
            visibility.put(GuiElements.HOTBAR, false);
            listener.onGuiVisibilitySet(new GlassesSetGuiVisibilityEvent(42L, visibility));
            listener.onDragParamsSet(new GlassesSetDragParamsEvent(42L, 10, 10));

            // A different GUID must not affect this capture screen.
            listener.onBackgroundChange(new GlassesChangeBackgroundEvent(43L, 0x445566));
            listener.onGuiVisibilitySet(new GlassesSetGuiVisibilityEvent(43L, visibility));
            listener.onDragParamsSet(new GlassesSetDragParamsEvent(43L, 20, 20));
        }
    }

    @Test
    public void overlayAndTickListenersAreSafeWithoutAPlayer() {
        final TerminalManagerClient.ForgeBusListener listener = (TerminalManagerClient.ForgeBusListener) TerminalManagerClient.instance
            .createForgeBusListener();
        final RenderGameOverlayEvent base = new RenderGameOverlayEvent(0, null, 0, 0);
        final Minecraft minecraft = mock(Minecraft.class);
        minecraft.thePlayer = null;
        final FMLClientHandler handler = mock(FMLClientHandler.class);
        when(handler.getClient()).thenReturn(minecraft);
        try (MockedStatic<FMLClientHandler> staticHandler = Mockito.mockStatic(FMLClientHandler.class);
            MockedStatic<Minecraft> staticMinecraft = Mockito.mockStatic(Minecraft.class)) {
            staticHandler.when(FMLClientHandler::instance)
                .thenReturn(handler);
            listener.onRenderGameOverlay(new RenderGameOverlayEvent.Pre(base, RenderGameOverlayEvent.ElementType.ALL));
            listener
                .onRenderGameOverlay(new RenderGameOverlayEvent.Post(base, RenderGameOverlayEvent.ElementType.HELMET));
            staticMinecraft.when(Minecraft::getMinecraft)
                .thenReturn(minecraft);
            final TerminalManagerClient.FmlBusListener fml = (TerminalManagerClient.FmlBusListener) TerminalManagerClient.instance
                .createFmlBusListener();
            fml.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            fml.onDisconnect(null);
        }
    }
}
