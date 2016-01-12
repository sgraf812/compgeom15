package visibility.types;

import java.util.List;

public interface GeometryParser {
    List<Triangle> parseFile(String file);
}
