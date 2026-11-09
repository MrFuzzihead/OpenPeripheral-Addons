package openperipheral.addons.glasses.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GlassesRenderingUtilsTest {

    private Tessellator previous;
    private Tessellator tessellator;

    @Before
    public void installMockTessellator() throws Exception {
        final Field field = Tessellator.class.getDeclaredField("instance");
        field.setAccessible(true);
        clearFinal(field);
        previous = Tessellator.instance;
        tessellator = mock(Tessellator.class);
        field.set(null, tessellator);
    }

    @After
    public void restoreTessellator() throws Exception {
        final Field field = Tessellator.class.getDeclaredField("instance");
        field.setAccessible(true);
        clearFinal(field);
        field.set(null, previous);
    }

    private static void clearFinal(Field field) throws Exception {
        final Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    @Test
    public void drawsAQuadWithTheRequestedUvSubrect() {
        final IIcon icon = mock(IIcon.class);
        when(icon.getMinU()).thenReturn(0.0f);
        when(icon.getMaxU()).thenReturn(1.0f);
        when(icon.getMinV()).thenReturn(0.0f);
        when(icon.getMaxV()).thenReturn(1.0f);

        GlassesRenderingUtils.drawTexturedQuad(10, 20, icon, 8, 4, 0.5f, 0.25f);

        verify(tessellator).startDrawingQuads();
        verify(tessellator).addVertexWithUV(10, 24, 0, 0.0, 0.25);
        verify(tessellator).addVertexWithUV(18, 24, 0, 0.5, 0.25);
        verify(tessellator).addVertexWithUV(18, 20, 0, 0.5, 0.0);
        verify(tessellator).addVertexWithUV(10, 20, 0, 0.0, 0.0);
        verify(tessellator).draw();
    }

    @Test
    public void handlesNonZeroIconUvOrigins() {
        final IIcon icon = mock(IIcon.class);
        when(icon.getMinU()).thenReturn(0.25f);
        when(icon.getMaxU()).thenReturn(0.75f);
        when(icon.getMinV()).thenReturn(0.5f);
        when(icon.getMaxV()).thenReturn(1.0f);

        GlassesRenderingUtils.drawTexturedQuad(0, 0, icon, 16, 16, 1.0f, 1.0f);
        verify(tessellator).addVertexWithUV(16, 16, 0, 0.75, 1.0);
        verify(tessellator).addVertexWithUV(16, 0, 0, 0.75, 0.5);
        Assert.assertNotNull(tessellator);
    }
}
