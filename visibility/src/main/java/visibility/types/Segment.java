package visibility.types;

import javafx.geometry.Point2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.tuple.Tuple3;

import static org.jooq.lambda.tuple.Tuple.tuple;

public class Segment {

    private final Point2D start;
    private final Point2D end;
    private final double distanceSquared;
    private final Point2D dir;
    private final Point2D orth;
    private final double dxdy;
    private final double dydx;

    public Segment(@NotNull Point2D start, @NotNull Point2D end) {
        this.start = start;
        this.end = end;
        Point2D delta = start.subtract(end);
        this.distanceSquared = delta.dotProduct(delta);
        this.dir = end.subtract(start).normalize();
        this.orth = new Point2D(-dir.getY(), dir.getX());
        this.dxdy = dir.getX()/(dir.getY() == 0 ? Double.MIN_VALUE : dir.getY());
        this.dydx = dir.getY()/(dir.getX() == 0 ? Double.MIN_VALUE : dir.getX());
    }

    private Segment(Point2D start, Point2D end, Point2D dir, Point2D orth, double dxdy, double dydx) {
        this.start = start;
        this.end = end;
        Point2D delta = start.subtract(end);
        this.distanceSquared = delta.dotProduct(delta);
        this.dir = dir;
        this.orth = orth;
        this.dxdy = dxdy;
        this.dydx = dydx;
    }

    @NotNull
    public final Point2D getStart() {
        return start;
    }

    @NotNull
    public final Point2D getEnd() {
        return end;
    }

    @NotNull
    public final Tuple3<Boolean, Segment, Segment> splitAtXorY(boolean splitAtX, double splitValue) {
        Point2D s = this.start;
        Point2D e = this.end;
        if (splitAtX) {
            double y = dydx * (splitValue - s.getX()) + s.getY();
            Point2D split = new Point2D(splitValue, y);
            return splitAtPoint(s.getX() < splitValue, e.getX() < splitValue, split);
        } else {
            double x = dxdy * (splitValue - s.getY()) + s.getX();
            Point2D split = new Point2D(x, splitValue);
            return splitAtPoint(s.getY() < splitValue, e.getY() < splitValue, split);
        }
    }

    @NotNull
    private Tuple3<Boolean, Segment, Segment> splitAtPoint(boolean startLeft, boolean endLeft, Point2D split) {
        if (startLeft == endLeft) {
            return tuple(startLeft, this, null);
        } else {
            Segment a = new Segment(this.start, split, this.dir, this.orth, this.dxdy, this.dydx);
            Segment b = new Segment(split, this.end, this.dir, this.orth, this.dxdy, this.dydx);
            return tuple(startLeft, a, b);
        }
    }

    @Nullable
    public final Intersection intersectTriangle(@NotNull Triangle t) {
        // First we check that not all points of the triangle
        // lie on the same side of the segment
        final Point2D ra = t.a.subtract(start);
        final Point2D rb = t.b.subtract(start);
        final Point2D rc = t.c.subtract(start);

        final double oa = ra.dotProduct(this.orth);
        final double ob = rb.dotProduct(this.orth);
        final double oc = rc.dotProduct(this.orth);

        // If the signs of any of the o* differ (or are zero), we have an intersection at an edge.
        // That is the case only if the product is non-positive.
        final boolean ab = oa * ob <= 0;
        final boolean bc = ob * oc <= 0;
        final boolean ca = oc * oa <= 0;

        if (!ab && !bc && !ca) {
            // No intersection
            return null;
        }

        // There is some intersection, signs didn't match.

        if (t.isInside(start)) {
            // This should be pretty clear.
            return new Intersection(start, 0);
        }

        final double da = ra.dotProduct(this.dir);
        final double db = rb.dotProduct(this.dir);
        final double dc = rc.dotProduct(this.dir);

        if (ab) {
            if (bc) {
                return intersectionWithNearestEdge(oa, ob, oc, da, db, dc);
            } else if (ca) {
                return intersectionWithNearestEdge(oc, oa, ob, dc, da, db);
            } else {
                return exactlyOneIntersectionIsImpossible();
            }
        } else if (bc) {
            if (ca) {
                return intersectionWithNearestEdge(ob, oc, oa, db, dc, da);
            } else {
                return exactlyOneIntersectionIsImpossible();
            }
        } else {
            return exactlyOneIntersectionIsImpossible();
        }
    }

    private Intersection intersectionWithNearestEdge(double oa, double ob, double oc, double da, double db, double dc) {
        // The code assumes that ab and bc are the intersected edges.
        // *b will the point shared by both intersected edges.
        // Calling code will permute the parameters appropriately to reduce duplication.
        double ab = distanceOfIntersection(oa, da, ob, db);
        double bc = distanceOfIntersection(ob, db, oc, dc);

        return ab < bc ? intersectionAtDistance(ab) : intersectionAtDistance(bc);
    }

    private static double distanceOfIntersection(double oa, double da, double ob, double db) {
        // Interpolate based on the orthogonal projection
        //
        final double s = (0 - oa) / (ob - oa);
        return da + s * (db - da);
    }

    private Intersection intersectionAtDistance(double distance) {
        if (distance >= 0 && distance*distance <= this.distanceSquared) {
            return new Intersection(this.start.add(this.dir.multiply(distance)), distance);
        } else {
            return null;
        }
    }

    private Intersection exactlyOneIntersectionIsImpossible() {
        throw new RuntimeException("There can't be exactly one intersected triangle edge with a ray");
    }
}
