package openperipheral.addons.glasses;

import static org.mockito.Mockito.mock;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.junit.Assert;
import org.junit.Test;

import openmods.network.event.NetworkEvent;
import openperipheral.addons.Config;
import openperipheral.addons.glasses.GlassesEvent.GlassesChangeBackgroundEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetDragParamsEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetGuiVisibilityEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetKeyRepeatEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesStopCaptureEvent;
import openperipheral.addons.utils.GuiElements;

public class GuiCaptureControlTest {

    private static final class RecordingSender implements GuiCaptureControl.EventSender {

        private final List<NetworkEvent> events = new ArrayList<NetworkEvent>();

        @Override
        public void send(NetworkEvent event, EntityPlayer player) {
            events.add(event);
        }
    }

    private static GuiCaptureControl control() {
        return new GuiCaptureControl(1L, new WeakReference<EntityPlayerMP>(mock(EntityPlayerMP.class)));
    }

    @Test
    public void dragParametersMustRespectConfiguredMinimums() {
        final GuiCaptureControl control = control();
        try {
            control.setDragParameters(Config.minimalDragThreshold - 1, Config.minimalDragPeriod);
            Assert.fail("Below-minimum threshold must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            control.setDragParameters(Config.minimalDragThreshold, Config.minimalDragPeriod - 1);
            Assert.fail("Below-minimum period must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test(expected = NullPointerException.class)
    public void visibilityMapCannotBeNull() {
        control().toggleGuiElements((Map<GuiElements, Boolean>) null);
    }

    @Test
    public void sendsEveryCaptureControlEvent() {
        final GuiCaptureControl control = control();
        final RecordingSender sender = new RecordingSender();
        control.eventSender = sender;

        control.stopCapturing();
        control.setBackground(0x112233, null);
        control.setBackground(0x445566, 300);
        control.setKeyRepeat(true);
        control.setDragParameters(Config.minimalDragThreshold, Config.minimalDragPeriod);
        final Map<GuiElements, Boolean> visibility = new EnumMap<GuiElements, Boolean>(GuiElements.class);
        visibility.put(GuiElements.HOTBAR, false);
        control.toggleGuiElements(visibility);

        Assert.assertEquals(6, sender.events.size());
        Assert.assertTrue(sender.events.get(0) instanceof GlassesStopCaptureEvent);
        Assert.assertEquals(0x2A112233, ((GlassesChangeBackgroundEvent) sender.events.get(1)).backgroundColor);
        Assert.assertEquals(0xFF445566, ((GlassesChangeBackgroundEvent) sender.events.get(2)).backgroundColor);
        Assert.assertTrue(((GlassesSetKeyRepeatEvent) sender.events.get(3)).repeat);
        Assert.assertEquals(Config.minimalDragPeriod, ((GlassesSetDragParamsEvent) sender.events.get(4)).period);
        Assert.assertEquals(Config.minimalDragThreshold, ((GlassesSetDragParamsEvent) sender.events.get(4)).threshold);
        Assert.assertSame(visibility, ((GlassesSetGuiVisibilityEvent) sender.events.get(5)).visibility);
    }

    @Test(expected = IllegalStateException.class)
    public void stalePlayerReferenceIsRejected() {
        final GuiCaptureControl control = new GuiCaptureControl(1L, new WeakReference<EntityPlayerMP>(null));
        control.stopCapturing();
    }
}
