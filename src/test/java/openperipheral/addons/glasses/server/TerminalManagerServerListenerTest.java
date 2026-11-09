package openperipheral.addons.glasses.server;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.ServerChatEvent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.gameevent.TickEvent;
import openperipheral.addons.Config;
import openperipheral.addons.api.ITerminalIdGetter;
import openperipheral.addons.api.ITerminalIdSetter;
import openperipheral.addons.api.TerminalRegisterEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesMouseWheelEvent;
import openperipheral.addons.glasses.TerminalEvent;
import openperipheral.addons.glasses.TerminalIdAccess;
import openperipheral.addons.glasses.TileEntityGlassesBridge;

public class TerminalManagerServerListenerTest {

    private static final long GUID = 4242L;

    /** Mirrors the Forge event-bus transformer, which makes @Cancelable events return true at runtime. */
    private static final class CancelableChatEvent extends ServerChatEvent {

        private CancelableChatEvent(EntityPlayerMP player, String message) {
            super(player, message, new ChatComponentTranslation(message));
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    private TerminalManagerServer.ForgeListener forgeListener;
    private TerminalManagerServer.FmlListener fmlListener;
    private TileEntityGlassesBridge bridge;
    private EntityPlayerMP player;

    @Before
    public void setup() throws Exception {
        final Field getters = TerminalIdAccess.class.getDeclaredField("getters");
        getters.setAccessible(true);
        getters.set(TerminalIdAccess.instance, new ITerminalIdGetter[0]);
        final Field setters = TerminalIdAccess.class.getDeclaredField("setters");
        setters.setAccessible(true);
        setters.set(TerminalIdAccess.instance, new ITerminalIdSetter[0]);
        TerminalIdAccess.instance.register(new ITerminalIdGetter() {

            @Override
            public Optional<Long> getFor(EntityPlayer player) {
                return Optional.of(GUID);
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });

        bridge = mock(TileEntityGlassesBridge.class);
        TerminalManagerServer.instance.registerBridge(GUID, bridge);
        forgeListener = (TerminalManagerServer.ForgeListener) TerminalManagerServer.instance.createForgeListener();
        fmlListener = (TerminalManagerServer.FmlListener) TerminalManagerServer.instance.createFmlListener();

        player = mock(EntityPlayerMP.class);
        when(player.getGameProfile()).thenReturn(new GameProfile(UUID.randomUUID(), "player"));
        when(player.getCommandSenderName()).thenReturn("player");
    }

    @Test
    public void chatIsForwardedAndPrefixedCommandsAreCancelled() {
        final ServerChatEvent normal = new CancelableChatEvent(player, "hello");
        forgeListener.onServerChatEvent(normal);
        verify(bridge).onChatCommand("glasses_chat_message", "hello", player);
        Assert.assertFalse(normal.isCanceled());

        final ServerChatEvent command = new CancelableChatEvent(player, "$$command");
        forgeListener.onServerChatEvent(command);
        verify(bridge).onChatCommand("glasses_chat_command", "command", player);
        Assert.assertTrue(command.isCanceled());
    }

    @Test
    public void registrationResetAndClientEventsRequireOwnership() {
        forgeListener.onTerminalRegister(new TerminalRegisterEvent(player, GUID));
        verify(bridge).registerTerminal(player);

        final TerminalEvent.PrivateDrawableReset reset = new TerminalEvent.PrivateDrawableReset(GUID);
        reset.sender = player;
        forgeListener.onResetRequest(reset);
        verify(bridge).handlePrivateDrawableResetRequest(reset);

        final TerminalEvent.PrivateDrawableReset foreignReset = new TerminalEvent.PrivateDrawableReset(GUID + 1);
        foreignReset.sender = player;
        forgeListener.onResetRequest(foreignReset);
        verify(bridge, never()).handlePrivateDrawableResetRequest(foreignReset);

        final GlassesMouseWheelEvent event = new GlassesMouseWheelEvent(GUID, 2);
        event.sender = player;
        forgeListener.onGlassesEvent(event);
        verify(bridge).handleUserEvent(event);

        final GlassesMouseWheelEvent foreignEvent = new GlassesMouseWheelEvent(GUID + 1, 2);
        foreignEvent.sender = player;
        forgeListener.onGlassesEvent(foreignEvent);
        verify(bridge, never()).handleUserEvent(foreignEvent);
    }

    @Test
    public void playerTickRegistersTerminalsOnTheStartPhase() {
        fmlListener.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
        verify(bridge).registerTerminal(player);

        org.mockito.Mockito.clearInvocations(bridge);
        fmlListener.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        verify(bridge, never()).registerTerminal(player);
    }

    @Test
    public void chatFromANonTerminalPlayerIsIgnored() throws Exception {
        final Field getters = TerminalIdAccess.class.getDeclaredField("getters");
        getters.setAccessible(true);
        getters.set(TerminalIdAccess.instance, new ITerminalIdGetter[0]);

        forgeListener.onServerChatEvent(new CancelableChatEvent(player, "hello"));
        verify(bridge, never()).onChatCommand(eq("glasses_chat_message"), eq("hello"), eq(player));
    }

    @Test
    public void listenToAllChatCanBeDisabled() {
        final boolean previous = Config.listenToAllChat;
        try {
            Config.listenToAllChat = false;
            forgeListener.onServerChatEvent(new CancelableChatEvent(player, "hello"));
            verify(bridge, never()).onChatCommand(eq("glasses_chat_message"), eq("hello"), eq(player));
        } finally {
            Config.listenToAllChat = previous;
        }
    }
}
