package visibility.algorithm;

import javafx.geometry.Point2D;
import visibility.types.Intersection;
import visibility.types.Segment;
import visibility.types.SpatialDataStructure;
import visibility.types.Triangle;

public class NaiveIntersection implements SpatialDataStructure {

    private final Iterable<Triangle> triangles;

    private NaiveIntersection(Iterable<Triangle> triangles) {

        this.triangles = triangles;
    }

    @Override
    public Point2D intersectWith(Segment s) {
        Intersection min = null;

        for (Triangle t : triangles) {
            Intersection i = s.intersectTriangle(t);
            if (i != null) {
                if (min == null || min.getDistance() > i.getDistance()) {
                    min = i;
                }
            }
        }

        return min != null ? min.getIntersection() : null;
    }

    public static NaiveIntersection fromTriangles(Iterable<Triangle> triangles) {
        return new NaiveIntersection(triangles);
    }
}
