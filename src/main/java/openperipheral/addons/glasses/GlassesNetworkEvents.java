package openperipheral.addons.glasses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import openmods.network.event.NetworkEvent;
import openmods.network.event.NetworkEventManager;
import openperipheral.addons.glasses.GlassesEvent.GlassesChangeBackgroundEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesComponentMouseButtonEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesComponentMouseWheelEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesKeyDownEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesKeyUpEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesMouseButtonEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesMouseDragEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesMouseWheelEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetDragParamsEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetGuiVisibilityEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetKeyRepeatEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSignalCaptureEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesStopCaptureEvent;

/**
 * Single source of truth for the network event IDs registered by this mod.
 * <p>
 * Keeping the list in one place lets unit tests verify that every registered event is public, has the no-arg
 * constructor required by {@code NetworkEventRegistry.createPacketType()}, and is actually registered.
 */
public final class GlassesNetworkEvents {

    private GlassesNetworkEvents() {}

    public static final List<Class<? extends NetworkEvent>> ALL = Collections.unmodifiableList(
        Arrays.<Class<? extends NetworkEvent>>asList(
            TerminalEvent.PrivateDrawableReset.class,
            TerminalEvent.PublicDrawableReset.class,
            TerminalEvent.PrivateClear.class,
            TerminalEvent.PublicClear.class,
            TerminalEvent.PrivateDrawableData.class,
            TerminalEvent.PublicDrawableData.class,
            GlassesSignalCaptureEvent.class,
            GlassesStopCaptureEvent.class,
            GlassesMouseWheelEvent.class,
            GlassesMouseButtonEvent.class,
            GlassesComponentMouseWheelEvent.class,
            GlassesComponentMouseButtonEvent.class,
            GlassesMouseDragEvent.class,
            GlassesKeyDownEvent.class,
            GlassesKeyUpEvent.class,
            GlassesChangeBackgroundEvent.class,
            GlassesSetDragParamsEvent.class,
            GlassesSetKeyRepeatEvent.class,
            GlassesSetGuiVisibilityEvent.class));

    public static void registerAll(NetworkEventManager.RegistrationContext context) {
        for (Class<? extends NetworkEvent> cls : ALL) context.register(cls);
    }
}
