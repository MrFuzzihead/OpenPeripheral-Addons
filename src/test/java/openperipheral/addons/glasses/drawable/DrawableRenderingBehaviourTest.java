package openperipheral.addons.glasses.drawable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import openmods.geometry.Box2d;
import openperipheral.addons.glasses.utils.Point2d;

public class DrawableRenderingBehaviourTest {

    @Test
    public void gradientBoxClampsOpacityAndReportsVisibility() {
        final GradientBox box = new GradientBox(0, 0, 10, 20, 0x112233, 2, 0x445566, -1, 1);
        box.onUpdate();
        Assert.assertTrue(box.isVisible());
        Assert.assertEquals(0x445566, box.color2);
        Assert.assertEquals(1.0f, box.opacity1, 0.0001f);
        Assert.assertEquals(0.0f, box.opacity2, 0.0001f);
        Assert.assertSame(
            Box2d.class,
            box.getBoundingBox()
                .getClass());

        final GradientBox invisible = new GradientBox(0, 0, 1, 1, 0xFFFFFF, 0, 0xFFFFFF, 0, 1);
        invisible.onUpdate();
        Assert.assertFalse(invisible.isVisible());
        Assert.assertEquals(DrawableType.GRADIENT, invisible.getTypeEnum());
    }

    @Test
    public void pointClampsSizeAndOpacityAndHasAZeroSizedBox() {
        final Point point = new Point(new Point2d(4, 5), 0xFFFFFF, 2);
        point.size = -2;
        point.onUpdate();
        Assert.assertTrue(point.isVisible());
        Assert.assertEquals(1.0f, point.size, 0.0001f);
        Assert.assertEquals(1.0f, point.opacity, 0.0001f);
        Assert.assertEquals(0f, point.getBoundingBox().width, 0.0001f);
        Assert.assertEquals(0f, point.getBoundingBox().height, 0.0001f);
    }

    @Test
    public void itemIconClampsDamageAndResolvesRegisteredItems() {
        final Item item = mock(Item.class);
        try (MockedStatic<GameRegistry> registry = Mockito.mockStatic(GameRegistry.class)) {
            registry.when(() -> GameRegistry.findItem("mod", "item"))
                .thenReturn(item);
            registry.when(() -> GameRegistry.findItem("mod", "missing"))
                .thenReturn(null);

            final ItemIcon icon = new ItemIcon(0, 0, "mod:item", 3);
            icon.damageBar = 2;
            icon.onUpdate();
            Assert.assertTrue(icon.isVisible());
            Assert.assertEquals(1.0f, icon.damageBar, 0.0001f);
            Assert.assertEquals(16, Math.round(icon.getBoundingBox().width));

            final ItemIcon missing = new ItemIcon(0, 0, "mod:missing", 0);
            missing.onUpdate();
            Assert.assertFalse(missing.isVisible());

            final ItemIcon malformed = new ItemIcon(0, 0, "not-an-id", 0);
            malformed.onUpdate();
            Assert.assertFalse(malformed.isVisible());

            final ItemIcon zeroScale = new ItemIcon(0, 0, "mod:item", 0);
            zeroScale.scale = 0;
            zeroScale.onUpdate();
            Assert.assertFalse(zeroScale.isVisible());
        }
    }

    @Test
    public void textMeasuresItsBoundingBoxAndVisibility() throws Exception {
        final Minecraft minecraft = mock(Minecraft.class);
        final FontRenderer font = mock(FontRenderer.class);
        final Field fontField = Minecraft.class.getDeclaredField("fontRenderer");
        fontField.setAccessible(true);
        fontField.set(minecraft, font);
        when(font.getStringWidth("hello")).thenReturn(30);

        final FMLClientHandler handler = mock(FMLClientHandler.class);
        when(handler.getClient()).thenReturn(minecraft);

        try (MockedStatic<FMLClientHandler> staticHandler = Mockito.mockStatic(FMLClientHandler.class)) {
            staticHandler.when(FMLClientHandler::instance)
                .thenReturn(handler);

            final Text text = new Text(1, 2, "hello", 0xFFFFFF);
            text.scale = 2;
            text.onUpdate();
            Assert.assertEquals(60, Math.round(text.getBoundingBox().width));
            Assert.assertEquals(16, Math.round(text.getBoundingBox().height));
            Assert.assertTrue(text.isVisible());

            final Text empty = new Text(0, 0, "", 0xFFFFFF);
            empty.alpha = 0;
            empty.onUpdate();
            Assert.assertEquals(0, Math.round(empty.getBoundingBox().width));
            Assert.assertFalse(empty.isVisible());
            Assert.assertEquals(DrawableType.TEXT, empty.getTypeEnum());
        }
    }

    @Test
    public void liquidIconUsesFluidIconsAndVisibility() {
        final Fluid fluid = mock(Fluid.class);
        final IIcon icon = mock(IIcon.class);
        when(fluid.getFlowingIcon()).thenReturn(icon);
        when(icon.getIconWidth()).thenReturn(16);
        when(icon.getIconHeight()).thenReturn(16);

        LiquidIcon.fluidIcons = new LiquidIcon.FluidIconResolver() {

            @Override
            public IIcon resolve(String fluidId) {
                return "water".equals(fluidId) ? icon : null;
            }
        };

        {
            final LiquidIcon liquid = new LiquidIcon(0, 0, 32, 32, "water");
            liquid.onUpdate();
            Assert.assertTrue(liquid.isVisible());
            Assert.assertEquals(32, Math.round(liquid.getBoundingBox().width));

            final LiquidIcon missing = new LiquidIcon(0, 0, 16, 16, "missing");
            missing.onUpdate();
            Assert.assertFalse(missing.isVisible());

            final LiquidIcon transparent = new LiquidIcon(0, 0, 16, 16, "water");
            transparent.alpha = 0;
            transparent.onUpdate();
            Assert.assertFalse(transparent.isVisible());
        }
    }

    @Test
    public void itemIconAndLiquidIconExposeTheirTypeNames() {
        Assert.assertEquals("item", new ItemIcon(0, 0, null, 0).getTypeName());
        Assert.assertEquals("liquid", new LiquidIcon(0, 0, 1, 1, null).getTypeName());
        Assert.assertEquals("gradient", new GradientBox().getTypeName());
        Assert.assertEquals("point", new Point().getTypeName());
    }
}
