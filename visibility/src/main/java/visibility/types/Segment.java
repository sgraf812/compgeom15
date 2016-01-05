package visibility.types;

import javafx.geometry.Point2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Segment {

    private final Point2D start;
    private final Point2D end;
    private final Point2D dir;
    private final Point2D orth;
    private final double invDistance;

    public Segment(@NotNull Point2D start, @NotNull Point2D end) {
        this.start = start;
        this.end = end;
        this.dir = end.subtract(start);
        this.orth = new Point2D(-dir.getY(), dir.getX());
        this.invDistance = 1.0/start.distance(end);
    }

    @NotNull
    public Point2D getStart() {
        return start;
    }

    @NotNull
    public Point2D getEnd() {
        return end;
    }

    @Nullable
    public Intersection intersectTriangle(@NotNull Triangle t) {
        // First we check that not all points of the triangle
        // lie on the same side of the segment
        final Point2D ra = t.a.subtract(start);
        final Point2D rb = t.b.subtract(start);
        final Point2D rc = t.c.subtract(start);

        final double oa = ra.dotProduct(this.orth);
        final double ob = rb.dotProduct(this.orth);
        final double oc = rc.dotProduct(this.orth);

        // If the signs of any of the d* differ (or a zero), we have an intersection at an edge.
        // That is the case only if the product is non-positive.
        final boolean ab = oa * ob <= 0;
        final boolean bc = ob * oc <= 0;
        final boolean ca = oc * oa <= 0;

        if (!ab && !bc && !ca) {
            // No intersection
            return null;
        }

        // There is some intersection, signs didn't match.

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
        if (da < dc) {
            // The intersection with ab is nearest
            return triangleEdgeIntersected(oa, da, ob, db);
        } else {
            // The intersection with bc is nearest
            return triangleEdgeIntersected(ob, db, oc, dc);
        }
    }

    private Intersection triangleEdgeIntersected(double oa, double da, double ob, double db) {
        // Interpolate based on the orthogonal projection
        //
        final double s = (0-oa)/(ob - oa);
        final double distance = (da + s*(db - da)) * this.invDistance;
        return new Intersection(this.start.add(this.dir.multiply(distance)), distance);
    }

    private Intersection exactlyOneIntersectionIsImpossible() {
        throw new RuntimeException("There can't be exactly one intersected triangle edge with a ray");
    }
}
