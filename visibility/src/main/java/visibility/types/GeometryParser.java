package visibility.types;

import java.io.InputStream;
import java.util.List;

public interface GeometryParser {
    List<Triangle> parseFile(InputStream file);
}
