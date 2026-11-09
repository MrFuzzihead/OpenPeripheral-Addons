package openperipheral.addons.glasses.utils;

import org.junit.Assert;
import org.junit.Test;

import openmods.geometry.Box2d;

public class GeometryTest {

    @Test
    public void pointListBuilderConvertsToRelativeCoordinates() {
        final PointListBuilder builder = new PointListBuilder();
        builder.add(new Point2d(10, 20));
        builder.add(new Point2d(20, 10));

        final Box2d box = builder.buildBoundingBox();
        Assert.assertEquals(10f, box.left, 0.0001f);
        Assert.assertEquals(20f, box.right, 0.0001f);
        Assert.assertEquals(10f, box.top, 0.0001f);
        Assert.assertEquals(20f, box.bottom, 0.0001f);
        Assert.assertEquals(10f, box.width, 0.0001f);
        Assert.assertEquals(10f, box.height, 0.0001f);

        final IPointList<Point2d> points = builder.buildPointList();
        Assert.assertEquals(2, points.size());
        Assert.assertEquals(
            new Point2d(0, 10).toString(),
            points.iterator()
                .next()
                .toString());
    }

    @Test
    public void emptyPointListUsesNullBox() {
        final PointListBuilder builder = new PointListBuilder();
        Assert.assertSame(Box2d.NULL, builder.buildBoundingBox());
        Assert.assertEquals(
            0,
            builder.buildPointList()
                .size());
    }

    @Test
    public void nullPointsAreIgnored() {
        final PointListBuilder builder = new PointListBuilder();
        builder.add(null);
        Assert.assertSame(Box2d.NULL, builder.buildBoundingBox());
    }

    @Test
    public void colorPointListBuilderPreservesColors() {
        final ColorPointListBuilder builder = new ColorPointListBuilder();
        builder.add(new ColorPoint2d(1, 2, 0x112233, 0.5f));
        builder.add(new ColorPoint2d(5, 6, 0x445566, 0.75f));

        final Box2d box = builder.buildBoundingBox();
        Assert.assertEquals(1f, box.left, 0.0001f);
        Assert.assertEquals(5f, box.right, 0.0001f);

        final IPointList<ColorPoint2d> points = builder.buildPointList();
        Assert.assertEquals(2, points.size());
        final ColorPoint2d first = points.iterator()
            .next();
        Assert.assertEquals(0, first.x, 0.0001f);
        Assert.assertEquals(0, first.y, 0.0001f);
        Assert.assertEquals(0x112233, first.rgb);
        Assert.assertEquals(0.5f, first.opacity, 0.0001f);
    }

    @Test
    public void colorPointToStringIsBalanced() {
        final String value = new ColorPoint2d(1, 2, 0x112233, 0.5f).toString();
        Assert.assertTrue(value.startsWith("("));
        Assert.assertTrue(value.endsWith(")"));
    }
}
