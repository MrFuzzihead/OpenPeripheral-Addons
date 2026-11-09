package openperipheral.addons.glasses.drawable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import openmods.geometry.Box2d;
import openperipheral.addons.glasses.utils.Point2d;

public class DrawableTest {

    private static final class RecordingOwner implements openperipheral.addons.glasses.IOwnerProxy {

        @Override
        public void removeContainer(int containerId) {}

        @Override
        public void markElementModified(int elementId) {}
    }

    @Test
    public void boxBoundingBoxAndLocalTransform() {
        final SolidBox box = new SolidBox(10, 20, 30, 40, 0x123456, 0.5f);
        box.onUpdate();

        final Box2d bounds = box.getBoundingBox();
        Assert.assertEquals(10f, bounds.left, 0.0001f);
        Assert.assertEquals(20f, bounds.top, 0.0001f);
        Assert.assertEquals(30f, bounds.width, 0.0001f);
        Assert.assertEquals(40f, bounds.height, 0.0001f);

        final Point2d local = box.transformToLocal(15, 25, 100, 100);
        Assert.assertEquals(5f, local.x, 0.0001f);
        Assert.assertEquals(5f, local.y, 0.0001f);
    }

    @Test
    public void anchorsAffectLocalTransform() {
        final SolidBox box = new SolidBox(10, 20, 30, 40, 0x123456, 0.5f);
        box.setOwner(new RecordingOwner());
        box.setScreenAnchor(Drawable.HorizontalAlignment.RIGHT, Drawable.VerticalAlignment.BOTTOM);
        box.onUpdate();

        final Point2d local = box.transformToLocal(115, 125, 100, 100);
        Assert.assertEquals(5f, local.x, 0.0001f);
        Assert.assertEquals(5f, local.y, 0.0001f);

        box.setObjectAnchor(Drawable.HorizontalAlignment.MIDDLE, Drawable.VerticalAlignment.MIDDLE);
        final Point2d middle = box.transformToLocal(115, 125, 100, 100);
        Assert.assertEquals(20f, middle.x, 0.0001f);
        Assert.assertEquals(25f, middle.y, 0.0001f);
    }

    @Test
    public void rotationIsAppliedToLocalCoordinates() {
        final SolidBox box = new SolidBox(10, 20, 30, 40, 0x123456, 0.5f);
        box.rotation = 90;
        box.onUpdate();

        final Point2d local = box.transformToLocal(10, 25, 100, 100);
        Assert.assertEquals(5f, local.x, 0.0001f);
        Assert.assertEquals(0f, local.y, 0.0001f);
    }

    @Test
    public void emptyShapesAreInvisibleAndUseNullBox() {
        final SolidLineStrip strip = new SolidLineStrip();
        Assert.assertFalse(strip.isVisible());
        Assert.assertEquals(0f, strip.getBox().width, 0.0001f);
        Assert.assertEquals(0f, strip.getBox().height, 0.0001f);

        strip.onUpdate();
        Assert.assertFalse(strip.isVisible());
        Assert.assertEquals(0f, strip.getBox().width, 0.0001f);
        Assert.assertEquals(0f, strip.getBox().height, 0.0001f);
    }

    @Test
    public void shortLineAndTriangleAreInvisible() {
        final SolidLine line = new SolidLine(null, null, 0xFFFFFF, 1);
        line.onUpdate();
        Assert.assertFalse(line.isVisible());

        final SolidTriangle triangle = new SolidTriangle(new Point2d(0, 0), null, null, 0xFFFFFF, 1);
        triangle.onUpdate();
        Assert.assertFalse(triangle.isVisible());
    }

    @Test
    public void polygonWithTooFewPointsDoesNotBuildRenderCommands() {
        final SolidPolygon polygon = new SolidPolygon(0xFFFFFF, 1, new Point2d(0, 0), new Point2d(1, 1));
        polygon.onUpdate();
        Assert.assertFalse(polygon.isVisible());
        Assert.assertFalse(polygon.canRender());
    }

    @Test
    public void typeAndVisibilityMetadataAreConsistent() {
        final SolidBox box = new SolidBox(0, 0, 1, 1, 0xFFFFFF, 1);
        Assert.assertEquals(DrawableType.BOX.ordinal(), box.getType());
        Assert.assertEquals("box", box.getTypeName());
        Assert.assertTrue(box.shouldRender());
        Assert.assertTrue(box.isClickable());

        box.visible = false;
        Assert.assertFalse(box.shouldRender());
    }

    @Test
    public void alignmentSerializesAllFourAnchorsAndClampsCorruptValues() throws Exception {
        final SolidBox box = new SolidBox(10, 20, 30, 40, 0xFFFFFF, 1);
        final Field alignmentField = Drawable.class.getDeclaredField("alignment");
        alignmentField.setAccessible(true);
        final Object alignment = alignmentField.get(box);
        final Method write = alignment.getClass()
            .getDeclaredMethod("writeToStream", java.io.DataOutput.class);
        final Method read = alignment.getClass()
            .getDeclaredMethod("readFromStream", java.io.DataInput.class);
        write.setAccessible(true);
        read.setAccessible(true);

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        write.invoke(alignment, new DataOutputStream(bytes));
        Assert.assertEquals(1, bytes.size());

        final java.lang.reflect.Constructor<?> constructor = alignment.getClass()
            .getDeclaredConstructor();
        constructor.setAccessible(true);
        final Object decoded = constructor.newInstance();
        read.invoke(decoded, new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        final ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
        write.invoke(decoded, new DataOutputStream(roundTrip));
        Assert.assertArrayEquals(bytes.toByteArray(), roundTrip.toByteArray());

        final byte corrupt = (byte) 0xFF;
        read.invoke(decoded, new DataInputStream(new ByteArrayInputStream(new byte[] { corrupt })));
        final ByteArrayOutputStream clamped = new ByteArrayOutputStream();
        write.invoke(decoded, new DataOutputStream(clamped));
        Assert.assertEquals(0, clamped.toByteArray()[0]);
    }

    @Test
    public void allAnchorCombinationsProduceAUsableTransform() {
        final SolidBox box = new SolidBox(10, 20, 30, 40, 0xFFFFFF, 1);
        box.setOwner(new RecordingOwner());
        for (Drawable.HorizontalAlignment horizontal : Drawable.HorizontalAlignment.VALUES) {
            for (Drawable.VerticalAlignment vertical : Drawable.VerticalAlignment.VALUES) {
                box.setScreenAnchor(horizontal, vertical);
                box.setObjectAnchor(horizontal, vertical);
                box.setAlignment(horizontal, vertical);
                box.onUpdate();
                final Point2d point = box.transformToLocal(50, 60, 100, 100);
                Assert.assertNotNull(point);
            }
        }
    }

    @Test
    public void allDrawableTypesCanBeCreatedAndRoundTripTheirId() {
        for (DrawableType type : DrawableType.TYPES) {
            final Drawable drawable = type.create();
            Assert.assertNotNull(type.name(), drawable);
            Assert.assertEquals(type.ordinal(), drawable.getType());
            Assert.assertEquals(
                type.name()
                    .toLowerCase(),
                drawable.getTypeName());
        }
    }

    @Test
    public void solidBoxVisibilityDependsOnOpacity() {
        final SolidBox box = new SolidBox(0, 0, 1, 1, 0xFFFFFF, 0);
        box.onUpdate();
        Assert.assertFalse(box.isVisible());
    }
}
