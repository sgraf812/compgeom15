package visibility.geometry;

import org.poly2tri.geometry.polygon.Polygon;
import visibility.types.Triangle;
import visibility.types.Triangulator;

public class PolygonWithHolesTriangulator implements Triangulator {
    @Override
    public Iterable<Triangle> triangulate(Iterable<Polygon> polygons) {
        return null;
    }
}
