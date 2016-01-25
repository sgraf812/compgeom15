package algorithm;

import javafx.geometry.Point2D;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import visibility.algorithm.KDTree;
import visibility.osm.OSMGeometryParser;
import visibility.types.Segment;
import visibility.types.SpatialDataStructure;
import visibility.types.Triangle;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DataStructureBenchmarks {
    public static final Segment NO_INTERSECTION = new Segment(
            new Point2D(8615.438336512001, 50185.38763264),
            new Point2D(8562.44927872, 50187.798944426664)
    );

    @State(Scope.Thread)
    public static class BenchmarkState {
        public final SpatialDataStructure ds;

        public BenchmarkState() {
            try (InputStream file = new GZIPInputStream(this.getClass().getResourceAsStream("/karlsruhe.osm.gz"))) {
                List<Triangle> triangles = new OSMGeometryParser().parseFile(file);
                this.ds = KDTree.fromTriangles(triangles);
            } catch (IOException e) {
                System.err.println(e); // We can't handle that anyway. This is surely an error in project config
                throw new RuntimeException(e);
            }
        }
    }

    @Benchmark
    public Point2D kdTree(BenchmarkState state) {
        return state.ds.intersectWith(NO_INTERSECTION);
    }
}