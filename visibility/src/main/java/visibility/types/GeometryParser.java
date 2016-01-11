package visibility.types;

import org.poly2tri.geometry.polygon.Polygon;

public interface GeometryParser {
    Iterable<Polygon> parseFile(String file);
}
