package algorithm;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.runner.RunWith;
import visibility.types.Intersection;
import visibility.types.Segment;
import visibility.types.Triangle;

import static org.jooq.lambda.tuple.Tuple.tuple;
import static org.junit.Assert.*;

@RunWith(JUnitQuickcheck.class)
public class SegmentProperties {

    private static final double DEG_TO_RAD = Math.PI / 180;
    private static final double EPSILON = 0.00001;

    @Property(trials = 1000000)
    public void intersectingWithATriangle(
            double ax, double ay,
            double bx, double by,
            double cx, double cy,
            @InRange(min = "0", max = "1") double sr, @InRange(min = "0", max = "360") double stheta,
            @InRange(min = "0", max = "1") double er, @InRange(min = "0", max = "360") double etheta) {
        Triangle t = new Triangle(new Point2D(ax, ay), new Point2D(bx, by), new Point2D(cx, cy));

        computeCircumcircle(t).map((center, radius) -> {
            Point2D start = polarToCartesian(sr * radius, stheta).add(center);
            Point2D end = polarToCartesian(er * radius, etheta).add(center);
            Segment s = new Segment(start, end);
            Intersection i = s.intersectTriangle(t);

            if (i != null) {
                // the intersecting point should be within the triangle
                assertTrue(isApproximatelyInsideTriangle(t, i.getIntersection()));

                if (!t.isInside(start)) {
                    // there should be no nearer intersection
                    double dist = i.getDistance() * (1-EPSILON);
                    Point2D p = start.add(end.subtract(start).normalize().multiply(dist));
                    assertFalse(t.isInside(p));
                }
            }
            return null;
        });
    }

    private static boolean isApproximatelyInsideTriangle(Triangle t, Point2D p) {
        Point3D b = t.barycentricCoordinates(p);
        return b.getX() > -EPSILON && b.getY() > -EPSILON && b.getZ() > -EPSILON;
    }

    private static Tuple2<Point2D, Double> computeCircumcircle(Triangle t) {
        Point2D center = t.a.add(t.b).add(t.c).multiply(1 / 3.0);
        double radius = t.a.distance(center);
        return tuple(center, radius);
    }

    private static Point2D polarToCartesian(double radius, double theta) {
        return new Point2D(Math.cos(theta * DEG_TO_RAD), Math.sin(theta * DEG_TO_RAD)).multiply(radius);
    }
}
