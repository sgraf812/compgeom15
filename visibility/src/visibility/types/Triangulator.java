package visibility.types;

/**
 * Potentially has to deal with holes.
 */
public interface Triangulator {
    Iterable<Triangle> triangulate(Iterable<Polygon> polygons);
}
