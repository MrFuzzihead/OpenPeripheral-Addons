package openperipheral.addons.glasses.utils;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import openmods.geometry.BoundingBoxBuilder;
import openmods.geometry.Box2d;

public class PointListBuilder implements IPointListBuilder<Point2d> {

    private static class PointListImpl extends PointList<Point2d> {

        public PointListImpl(List<Point2d> points) {
            super(points);
        }

        @Override
        protected void drawPoint(RenderState renderState, Point2d p) {
            GL11.glVertex2f(p.x, p.y);
        }

    }

    private final List<Point2d> points = Lists.newArrayList();

    private final BoundingBoxBuilder bbBuilder = BoundingBoxBuilder.create();

    private int pointCount;

    @Override
    public void add(Point2d point) {
        if (point == null) return;
        bbBuilder.addPoint(point.x, point.y);
        points.add(point);
        pointCount++;
    }

    private static Point2d toBoundingBox(Box2d bb, Point2d point) {
        return new Point2d(point.x - bb.left, point.y - bb.top);
    }

    @Override
    public PointList<Point2d> buildPointList() {
        final Box2d bb = buildBoundingBox();

        List<Point2d> relPoints = Lists.newArrayList();
        for (Point2d p : points) relPoints.add(toBoundingBox(bb, p));

        return new PointListImpl(relPoints);
    }

    @Override
    public Box2d buildBoundingBox() {
        return pointCount > 0 ? bbBuilder.build() : Box2d.NULL;
    }

}
