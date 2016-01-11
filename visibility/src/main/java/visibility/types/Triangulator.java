package visibility.types;

import org.poly2tri.geometry.polygon.Polygon;

/**
 * Potentially has to deal with holes.
 */
public interface Triangulator {
    Iterable<Triangle> triangulate(Iterable<Polygon> polygons);
}
