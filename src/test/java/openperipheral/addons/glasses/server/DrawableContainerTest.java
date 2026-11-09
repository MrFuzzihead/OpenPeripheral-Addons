package openperipheral.addons.glasses.server;

import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import openperipheral.addons.glasses.drawable.Drawable;
import openperipheral.addons.glasses.drawable.DrawableType;
import openperipheral.addons.glasses.utils.ColorPoint2d;
import openperipheral.addons.glasses.utils.Point2d;
import openperipheral.api.architecture.IArchitecture;
import openperipheral.api.helpers.Index;

public class DrawableContainerTest {

    private static final class FakeArchitecture implements IArchitecture {

        @Override
        public String architecture() {
            return "test";
        }

        @Override
        public Object wrapObject(Object target) {
            return target;
        }

        @Override
        public Index createIndex(int value) {
            return Index.fromJava(value, 1);
        }

        @Override
        public openperipheral.api.converter.IConverter getConverter() {
            return null;
        }
    }

    private DrawableContainerMaster.Public container;
    private DrawableFactory factory;
    private IArchitecture architecture;

    @Before
    public void setup() {
        container = new DrawableContainerMaster.Public(123);
        factory = new DrawableFactory(container);
        architecture = new FakeArchitecture();
    }

    @Test
    public void factoryCreatesEveryDrawableType() {
        final Drawable text = factory.addText(1, 2, "hello", null);
        final Drawable box = factory.addBox(0, 0, 10, 10, null, null);
        final Drawable gradient = factory.addGradientBox(0, 0, 10, 10, 0xFFFFFF, 1, 0, 1, 1);
        final Drawable icon = factory.addIcon(0, 0, "minecraft:stone", null);
        final Drawable liquid = factory.addLiquid(0, 0, 10, 10, "water");
        final Drawable triangle = factory
            .addTriangle(new Point2d(0, 0), new Point2d(1, 0), new Point2d(0, 1), null, null);
        final Drawable gradientTriangle = factory.addGradientTriangle(
            new ColorPoint2d(0, 0, 0xFFFFFF, 1),
            new ColorPoint2d(1, 0, 0xFFFFFF, 1),
            new ColorPoint2d(0, 1, 0xFFFFFF, 1));
        final Drawable quad = factory
            .addQuad(new Point2d(0, 0), new Point2d(1, 0), new Point2d(1, 1), new Point2d(0, 1), null, null);
        final Drawable gradientQuad = factory.addGradientQuad(
            new ColorPoint2d(0, 0, 0xFFFFFF, 1),
            new ColorPoint2d(1, 0, 0xFFFFFF, 1),
            new ColorPoint2d(1, 1, 0xFFFFFF, 1),
            new ColorPoint2d(0, 1, 0xFFFFFF, 1));
        final Drawable line = factory.addLine(new Point2d(0, 0), new Point2d(1, 1), null, null);
        final Drawable gradientLine = factory
            .addGradientLine(new ColorPoint2d(0, 0, 0xFFFFFF, 1), new ColorPoint2d(1, 1, 0xFFFFFF, 1));
        final Drawable lineList = factory.addLineList(null, null, new Point2d(0, 0), new Point2d(1, 1));
        final Drawable gradientLineList = factory
            .addGradientLineList(new ColorPoint2d(0, 0, 0xFFFFFF, 1), new ColorPoint2d(1, 1, 0xFFFFFF, 1));
        final Drawable polygon = factory
            .addPolygon(null, null, new Point2d(0, 0), new Point2d(1, 0), new Point2d(0, 1));
        final Drawable gradientPolygon = factory.addGradientPolygon(
            new ColorPoint2d(0, 0, 0xFFFFFF, 1),
            new ColorPoint2d(1, 0, 0xFFFFFF, 1),
            new ColorPoint2d(0, 1, 0xFFFFFF, 1));
        final Drawable point = factory.addPoint(new Point2d(0, 0), null, null);

        final Drawable[] all = { text, box, gradient, icon, liquid, triangle, gradientTriangle, quad, gradientQuad,
            line, gradientLine, lineList, gradientLineList, polygon, gradientPolygon, point };
        for (Drawable drawable : all) {
            Assert.assertNotNull(drawable);
            Assert.assertTrue(drawable.getId() >= 0);
        }
        Assert.assertEquals(DrawableType.TEXT.ordinal(), text.getType());
    }

    @Test
    public void containerSupportsLookupIterationAndClear() {
        final Drawable first = factory.addText(0, 0, "hello", 0xFFFFFF);
        final Drawable second = factory.addBox(0, 0, 1, 1, 0xFFFFFF, 1.0f);

        Assert.assertSame(first, container.getById(Index.fromJava(0, 1)));
        Assert.assertSame(second, container.getById(Index.fromJava(1, 1)));

        final Set<Index> ids = container.getAllIds(architecture);
        Assert.assertEquals(2, ids.size());
        final Map<Index, Drawable> objects = container.getAllObjects(architecture);
        Assert.assertEquals(2, objects.size());
        Assert.assertSame(first, objects.get(Index.fromJava(0, 1)));

        Assert.assertTrue(container.hasUpdates());
        Assert.assertNotNull(container.createFullDataEvent());
        Assert.assertNotNull(container.createUpdateDataEvent());
        Assert.assertTrue(
            container.createClearPacket() instanceof openperipheral.addons.glasses.TerminalEvent.PublicClear);

        container.clear();
        Assert.assertNotNull(container.createClearPacket());
        try {
            first.delete();
            Assert.fail("Cleared drawables are already deleted");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void privateSurfaceUsesPrivateEvents() {
        final SurfaceServer privateSurface = SurfaceServer.createPrivateSurface(7);
        final IDrawableFactory privateFactory = privateSurface.drawablesFactory;
        final Drawable drawable = privateFactory.addText(0, 0, "x", 0xFFFFFF);
        Assert.assertNotNull(drawable);
        Assert.assertTrue(
            privateSurface.drawablesContainer
                .createClearPacket() instanceof openperipheral.addons.glasses.TerminalEvent.PrivateClear);
        Assert.assertNotNull(SurfaceServer.createPublicSurface(8).drawablesContainer.createFullDataEvent());

        final DrawableContainerMaster.Private privateContainer = new DrawableContainerMaster.Private(9);
        final Drawable privateDrawable = privateContainer
            .addObject(new openperipheral.addons.glasses.drawable.SolidBox(0, 0, 1, 1, 0xFFFFFF, 1.0f));
        Assert.assertSame(privateDrawable, privateContainer.getById(Index.fromJava(0, 1)));
        privateContainer.clear();
    }

    @Test
    public void structuredObjectStateAndPropertyHooksWork() throws Exception {
        final Drawable drawable = factory.addBox(0, 0, 1, 1, 0xFFFFFF, 1.0f);
        final Map<Index, Drawable> all = container.getAllObjects(architecture);
        Assert.assertSame(drawable, all.get(Index.fromJava(0, 1)));
        Assert.assertEquals(
            Index.fromJava(0, 1)
                .intValue(),
            drawable.getId(architecture)
                .intValue());

        drawable.onPropertyGet(Drawable.class.getField("z"));
        drawable.onPropertySet(Drawable.class.getField("z"), (short) 3);
        drawable.onPropertySet(Drawable.class.getField("z"), "key", (short) 3);
        drawable.onPropertyGet(Drawable.class.getField("z"), "key");

        drawable.delete();
        try {
            drawable.getId(architecture);
            Assert.fail("Deleted drawable must reject getId");
        } catch (IllegalStateException expected) {
            // expected
        }
    }
}
