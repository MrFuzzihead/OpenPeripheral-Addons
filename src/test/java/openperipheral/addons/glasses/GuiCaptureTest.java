package openperipheral.addons.glasses;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;

import org.junit.Assert;
import org.junit.Test;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import openmods.network.event.NetworkEvent;
import openperipheral.addons.utils.GuiElements;

public class GuiCaptureTest {

    @Test
    public void captureStoresGuidAndCanBeConfiguredWithoutOpeningTheGui() throws Exception {
        final GuiCapture capture = new GuiCapture(77L);
        Assert.assertEquals(77L, capture.getGuid());
        Assert.assertFalse(capture.doesGuiPauseGame());

        capture.setBackground(0x112233);
        capture.setDragParameters(10, 20);
        capture.updateGuiElementsState(new EnumMap<GuiElements, Boolean>(GuiElements.class) {

            {
                put(GuiElements.HOTBAR, false);
            }
        });
        capture.forceGuiElementsState();

        final Field background = GuiCapture.class.getDeclaredField("backgroundColor");
        background.setAccessible(true);
        Assert.assertEquals(0x112233, background.getInt(capture));

        final Field threshold = GuiCapture.class.getDeclaredField("dragThresholdSquared");
        threshold.setAccessible(true);
        Assert.assertEquals(100.0f, (Float) threshold.get(capture), 0.001f);

        final Field period = GuiCapture.class.getDeclaredField("dragPeriod");
        period.setAccessible(true);
        Assert.assertEquals(20, period.getInt(capture));

        final Method update = GuiCapture.class.getDeclaredMethod("updateScreen");
        update.invoke(capture);
        update.invoke(capture);
    }

    @Test
    public void mouseInputProducesButtonWheelAndDragEvents() throws Exception {
        final GuiCapture capture = new GuiCapture(5L);
        final List<NetworkEvent> events = new ArrayList<NetworkEvent>();
        capture.eventSink = new GuiCapture.EventSink() {

            @Override
            public void send(NetworkEvent event) {
                events.add(event);
            }
        };

        final Minecraft minecraft = mockMinecraft(capture);
        try (MockedStatic<Mouse> mouse = Mockito.mockStatic(Mouse.class);
            MockedStatic<Minecraft> staticMinecraft = Mockito.mockStatic(Minecraft.class)) {
            staticMinecraft.when(Minecraft::getSystemTime)
                .thenReturn(0L);
            mouse.when(Mouse::getEventButton)
                .thenReturn(0);
            mouse.when(Mouse::getEventButtonState)
                .thenReturn(true);
            mouse.when(Mouse::getEventDWheel)
                .thenReturn(0);
            mouse.when(Mouse::getEventX)
                .thenReturn(50);
            mouse.when(Mouse::getEventY)
                .thenReturn(50);

            capture.handleMouseInput();
            Assert.assertEquals(1, events.size());
            Assert.assertTrue(
                events.get(0) instanceof openperipheral.addons.glasses.GlassesEvent.GlassesMouseButtonEvent);

            // Clear the drag limiter, then move far enough to send a drag event.
            for (int i = 0; i < 12; i++) capture.updateScreen();
            mouse.when(Mouse::getEventButton)
                .thenReturn(-1);
            mouse.when(Mouse::getEventX)
                .thenReturn(100);
            mouse.when(Mouse::getEventY)
                .thenReturn(100);
            capture.handleMouseInput();
            Assert.assertTrue(events.size() >= 2);

            mouse.when(Mouse::getEventDWheel)
                .thenReturn(120);
            capture.handleMouseInput();
            Assert.assertTrue(events.size() >= 3);
        }

        capture.setBackground(0x223344);
        capture.setDragParameters(5, 5);
        Assert.assertNotNull(minecraft);
    }

    @Test
    public void keyboardInputProducesKeyEventsAndEscapeClosesCapture() throws Exception {
        final GuiCapture capture = new GuiCapture(6L);
        final List<NetworkEvent> events = new ArrayList<NetworkEvent>();
        capture.eventSink = new GuiCapture.EventSink() {

            @Override
            public void send(NetworkEvent event) {
                events.add(event);
            }
        };
        final Minecraft minecraft = mockMinecraft(capture);

        try (MockedStatic<Keyboard> keyboard = Mockito.mockStatic(Keyboard.class)) {
            keyboard.when(Keyboard::getEventKey)
                .thenReturn(Keyboard.KEY_A);
            keyboard.when(Keyboard::getEventKeyState)
                .thenReturn(true);
            keyboard.when(Keyboard::getEventCharacter)
                .thenReturn('a');
            keyboard.when(Keyboard::isRepeatEvent)
                .thenReturn(false);
            capture.handleKeyboardInput();
            Assert.assertTrue(events.get(0) instanceof openperipheral.addons.glasses.GlassesEvent.GlassesKeyDownEvent);

            keyboard.when(Keyboard::getEventKeyState)
                .thenReturn(false);
            capture.handleKeyboardInput();
            Assert.assertTrue(events.get(1) instanceof openperipheral.addons.glasses.GlassesEvent.GlassesKeyUpEvent);

            keyboard.when(Keyboard::getEventKey)
                .thenReturn(Keyboard.KEY_ESCAPE);
            capture.handleKeyboardInput();
            org.mockito.Mockito.verify(minecraft)
                .displayGuiScreen(null);
            org.mockito.Mockito.verify(minecraft)
                .setIngameFocus();
        }
    }

    private static Minecraft mockMinecraft(GuiCapture capture) throws Exception {
        final Minecraft minecraft = Mockito.mock(Minecraft.class);
        minecraft.displayWidth = 800;
        minecraft.displayHeight = 600;
        final GameSettings settings = Mockito.mock(GameSettings.class);
        settings.guiScale = 1;
        minecraft.gameSettings = settings;
        final Field mcField = GuiScreen.class.getDeclaredField("mc");
        mcField.setAccessible(true);
        mcField.set(capture, minecraft);
        final Field width = GuiScreen.class.getDeclaredField("width");
        width.setAccessible(true);
        width.set(capture, 100);
        final Field height = GuiScreen.class.getDeclaredField("height");
        height.setAccessible(true);
        height.set(capture, 100);
        return minecraft;
    }

    @Test
    public void keyRepeatCanBeToggledThroughTheStaticKeyboardFacade() {
        final GuiCapture capture = new GuiCapture(1L);
        try (org.mockito.MockedStatic<org.lwjgl.input.Keyboard> keyboard = org.mockito.Mockito
            .mockStatic(org.lwjgl.input.Keyboard.class)) {
            capture.setKeyRepeat(true);
            keyboard.verify(() -> org.lwjgl.input.Keyboard.enableRepeatEvents(true));
            capture.setKeyRepeat(false);
            keyboard.verify(() -> org.lwjgl.input.Keyboard.enableRepeatEvents(false));
        }
    }
}
