package openperipheral.addons.glasses.server;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

import openperipheral.addons.api.ITerminalIdGetter;
import openperipheral.addons.api.ITerminalIdSetter;
import openperipheral.addons.glasses.TerminalIdAccess;

public class TerminalManagerServerTest {

    private static final class FixedGetter implements ITerminalIdGetter {

        private final long value;

        private FixedGetter(long value) {
            this.value = value;
        }

        @Override
        public Optional<Long> getFor(EntityPlayer player) {
            return Optional.of(value);
        }

        @Override
        public int getPriority() {
            return 0;
        }
    }

    @Before
    public void resetRegistry() throws Exception {
        setField("getters", new ITerminalIdGetter[0]);
        setField("setters", new ITerminalIdSetter[0]);
        TerminalIdAccess.instance.register(new FixedGetter(1234L));
    }

    private static void setField(String name, Object value) throws Exception {
        final Field field = TerminalIdAccess.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(TerminalIdAccess.instance, value);
    }

    @Test
    public void acceptsOnlyTheSendersOwnTerminal() {
        final EntityPlayer player = mock(EntityPlayer.class);
        Assert.assertTrue(TerminalManagerServer.isEventOwner(player, 1234L));
        Assert.assertFalse(TerminalManagerServer.isEventOwner(player, 5678L));
        Assert.assertFalse(TerminalManagerServer.isEventOwner(null, 1234L));
    }
}
