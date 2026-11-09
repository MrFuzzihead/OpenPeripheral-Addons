package openperipheral.addons.glasses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import openmods.network.event.NetworkEvent;
import openmods.serializable.cls.ClassSerializersProvider;
import openperipheral.addons.glasses.GlassesEvent.GlassesKeyDownEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetGuiVisibilityEvent;
import openperipheral.addons.utils.GuiElements;

public class GlassesNetworkEventsTest {

    @Test
    public void everyRegisteredEventIsInstantiableByTheNetworkRegistry() throws Exception {
        for (Class<? extends NetworkEvent> cls : GlassesNetworkEvents.ALL) {
            Assert.assertFalse("Registered event must be concrete: " + cls, Modifier.isAbstract(cls.getModifiers()));
            Assert.assertNotNull("Event has no public no-arg constructor: " + cls, cls.getConstructor());
            Assert.assertNotNull(cls.newInstance());
        }
    }

    @Test
    public void keyDownEventSerializesInheritedGuidAndFields() throws Exception {
        final GlassesKeyDownEvent original = new GlassesKeyDownEvent(0x1234L, 'x', 42, true);

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ClassSerializersProvider.instance.writeToStream(original, new DataOutputStream(bytes));

        final GlassesKeyDownEvent decoded = new GlassesKeyDownEvent();
        ClassSerializersProvider.instance
            .readFromStream(decoded, new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        Assert.assertEquals(original.guid, decoded.guid);
        Assert.assertEquals(original.ch, decoded.ch);
        Assert.assertEquals(original.code, decoded.code);
        Assert.assertEquals(original.isRepeat, decoded.isRepeat);
        Assert.assertEquals("glasses_key_down", decoded.getEventName());
    }

    @Test
    public void guiVisibilityEventSerializesEnumMap() throws Exception {
        final Map<GuiElements, Boolean> visibility = new EnumMap<GuiElements, Boolean>(GuiElements.class);
        visibility.put(GuiElements.HOTBAR, Boolean.FALSE);
        visibility.put(GuiElements.HEALTH, Boolean.TRUE);

        final GlassesSetGuiVisibilityEvent original = new GlassesSetGuiVisibilityEvent(7L, visibility);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ClassSerializersProvider.instance.writeToStream(original, new DataOutputStream(bytes));

        final GlassesSetGuiVisibilityEvent decoded = new GlassesSetGuiVisibilityEvent();
        ClassSerializersProvider.instance
            .readFromStream(decoded, new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        Assert.assertEquals(original.guid, decoded.guid);
        Assert.assertEquals(Boolean.FALSE, decoded.visibility.get(GuiElements.HOTBAR));
        Assert.assertEquals(Boolean.TRUE, decoded.visibility.get(GuiElements.HEALTH));
    }

    @Test
    public void publicAndPrivateDataEventsCreateMatchingSurfaceSlaves() {
        final SurfaceType publicType = new TerminalEvent.PublicDrawableData(1L).getSurfaceType();
        final SurfaceType privateType = new TerminalEvent.PrivateDrawableData(1L).getSurfaceType();
        Assert.assertEquals(SurfaceType.GLOBAL, publicType);
        Assert.assertEquals(SurfaceType.PRIVATE, privateType);

        Assert.assertTrue(
            new TerminalEvent.PublicDrawableData(1L).createSurface().drawablesContainer.getClass()
                .getName()
                .endsWith("Public"));
        Assert.assertTrue(
            new TerminalEvent.PrivateDrawableData(1L).createSurface().drawablesContainer.getClass()
                .getName()
                .endsWith("Private"));
    }
}
