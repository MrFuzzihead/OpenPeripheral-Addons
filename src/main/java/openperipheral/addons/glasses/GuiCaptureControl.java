package openperipheral.addons.glasses;

import java.lang.ref.WeakReference;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.google.common.base.Preconditions;

import openmods.network.event.NetworkEvent;
import openperipheral.addons.Config;
import openperipheral.addons.glasses.GlassesEvent.GlassesChangeBackgroundEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetDragParamsEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetGuiVisibilityEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetKeyRepeatEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesStopCaptureEvent;
import openperipheral.addons.utils.GuiElements;
import openperipheral.api.adapter.AdapterSourceName;
import openperipheral.api.adapter.Asynchronous;
import openperipheral.api.adapter.method.Arg;
import openperipheral.api.adapter.method.Optionals;
import openperipheral.api.adapter.method.ScriptCallable;
import openperipheral.api.adapter.method.ScriptObject;

@ScriptObject
@Asynchronous
@AdapterSourceName("glasses_capture")
public class GuiCaptureControl {

    interface EventSender {

        void send(NetworkEvent event, EntityPlayer player);
    }

    private static final EventSender DEFAULT_EVENT_SENDER = new EventSender() {

        @Override
        public void send(NetworkEvent event, EntityPlayer player) {
            event.sendToPlayer(player);
        }
    };

    // Test seam: production always uses DEFAULT_EVENT_SENDER.
    EventSender eventSender = DEFAULT_EVENT_SENDER;

    private final long guid;
    private final WeakReference<EntityPlayerMP> player;

    public GuiCaptureControl(long guid, WeakReference<EntityPlayerMP> player) {
        this.guid = guid;
        this.player = player;
    }

    protected EntityPlayer getPlayer() {
        EntityPlayer player = this.player.get();
        if (player == null) throw new IllegalStateException("Object is no longer valid");
        return player;
    }

    @ScriptCallable(description = "Stops capture for player")
    public void stopCapturing() {
        EntityPlayer player = getPlayer();
        eventSender.send(new GlassesStopCaptureEvent(guid), player);
    }

    @ScriptCallable(description = "Set background on capture mode screen")
    public void setBackground(@Arg(name = "background") int background, @Optionals @Arg(name = "alpha") Integer alpha) {
        EntityPlayer player = getPlayer();
        final int a = alpha != null ? Math.max(0, Math.min(255, alpha)) : 0x2A;
        eventSender.send(new GlassesChangeBackgroundEvent(guid, background & 0x00FFFFFF | (a << 24)), player);
    }

    @ScriptCallable(description = "When enabled, holding key down for long time will generate multiple events")
    public void setKeyRepeat(@Arg(name = "isEnabled") boolean keyRepeat) {
        EntityPlayer player = getPlayer();
        eventSender.send(new GlassesSetKeyRepeatEvent(guid, keyRepeat), player);
    }

    @ScriptCallable(description = "Set minimal distance and minimum period needed for ")
    public void setDragParameters(@Arg(name = "distance") int threshold, @Arg(name = "delay") int period) {
        Preconditions.checkArgument(
            threshold >= Config.minimalDragThreshold,
            "Distance must be not less than %s",
            Config.minimalDragThreshold);
        Preconditions.checkArgument(
            period >= Config.minimalDragPeriod,
            "Update period must be not less than %s",
            Config.minimalDragPeriod);
        EntityPlayer player = getPlayer();
        eventSender.send(new GlassesSetDragParamsEvent(guid, period, threshold), player);
    }

    @ScriptCallable(description = "Sets visiblity state of various vanilla GUI elements")
    public void toggleGuiElements(@Arg(name = "visibility") Map<GuiElements, Boolean> visibility) {
        Preconditions.checkNotNull(visibility, "visibility");
        EntityPlayer player = getPlayer();
        eventSender.send(new GlassesSetGuiVisibilityEvent(guid, visibility), player);
    }
}
