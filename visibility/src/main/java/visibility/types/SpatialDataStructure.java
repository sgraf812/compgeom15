package visibility.types;

import javafx.geometry.Point2D;

public interface SpatialDataStructure {
    /**
     * Calculates the intersection point closest to the segment's start.
     * @param s The segment
     * @return Point on the segment closest to its start that intersects
     * with the geometry, or null if there is no intersection.
     */
    Point2D intersectWith(Segment s);
}
