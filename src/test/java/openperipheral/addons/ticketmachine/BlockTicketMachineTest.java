package openperipheral.addons.ticketmachine;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import org.junit.Assert;
import org.junit.Test;

public class BlockTicketMachineTest {

    @Test
    public void registersFrontAndSideIcons() {
        final BlockTicketMachine block = new BlockTicketMachine();
        final IIconRegister registry = mock(IIconRegister.class);
        final IIcon icon = mock(IIcon.class);
        when(registry.registerIcon(anyString())).thenReturn(icon);

        block.registerBlockIcons(registry);
        Assert.assertSame(icon, BlockTicketMachine.iconFrontEmpty);
        Assert.assertSame(icon, BlockTicketMachine.iconFrontTicket);
    }
}
