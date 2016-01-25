package algorithm;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.geometry.Point2D;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import visibility.algorithm.KDTree;
import visibility.algorithm.NaiveIntersection;
import visibility.osm.OSMGeometryParser;
import visibility.types.GeometryParser;
import visibility.types.Segment;
import visibility.types.Triangle;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitQuickcheck.class)
public class KDTreeProperties {

    private static KDTree kdTree;
    private static NaiveIntersection naive;

    @BeforeClass
    public static void setUp() {
        try (InputStream file = new GZIPInputStream(KDTreeProperties.class.getResourceAsStream("/karlsruhe.osm.gz"))) {
            GeometryParser parser = new OSMGeometryParser();
            List<Triangle> triangles = parser.parseFile(file);
            kdTree = KDTree.fromTriangles(triangles);
            naive = NaiveIntersection.fromTriangles(triangles);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The Karlsruhe map is contained directly within those bounds
     */
    @Property(trials = 100000)
    public void sameOutputAsNaiveImplementation(
            @InRange(min="8604.9", max = "8630.5") double sx,
            @InRange(min="50181.6", max="50193") double sy,
            @InRange(min="8604.9", max = "8630.5") double ex,
            @InRange(min="50181.6", max="50193") double ey) {
        Segment s = new Segment(new Point2D(sx, sy), new Point2D(ex, ey));

        Point2D expected = naive.intersectWith(s);
        Point2D actual = kdTree.intersectWith(s);


        assertTrue((expected != null) == (actual != null));

        if (expected != null) {
            assertEquals(expected.getX(), actual.getX(), 10e-7);
            assertEquals(expected.getY(), actual.getY(), 10e-7);
        }
    }
}
