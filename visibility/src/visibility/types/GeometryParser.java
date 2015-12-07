package visibility.types;

public interface GeometryParser {
    Iterable<Polygon> parseFile(String file);
}
