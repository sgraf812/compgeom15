package algorithm;

import javafx.geometry.Point2D;
import org.jooq.lambda.Seq;
import org.openjdk.jmh.annotations.*;
import visibility.algorithm.KDTree;
import visibility.algorithm.NaiveIntersection;
import visibility.osm.OSMGeometryParser;
import visibility.types.BoundingRectangle;
import visibility.types.Segment;
import visibility.types.SpatialDataStructure;
import visibility.types.Triangle;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.zip.GZIPInputStream;

public class DataStructureBenchmarks {
    public static final Segment NO_INTERSECTION = new Segment(
            new Point2D(8615.438336512001, 50185.38763264),
            new Point2D(8562.44927872, 50187.798944426664)
    );

    @State(Scope.Thread)
    public static class MapState {
        private PrimitiveIterator.OfDouble x;
        private PrimitiveIterator.OfDouble y;
        public final SpatialDataStructure kdTree;
        public final SpatialDataStructure naive;
        public final BoundingRectangle bounds;
        public Segment seg;

        public MapState(String name) {
            try (InputStream file = new GZIPInputStream(this.getClass().getResourceAsStream(name))) {
                List<Triangle> triangles = new OSMGeometryParser().parseFile(file);
                this.kdTree = KDTree.fromTriangles(triangles);
                this.naive = NaiveIntersection.fromTriangles(triangles);
                this.bounds = Seq.seq(triangles)
                        .map(BoundingRectangle::fromPoints)
                        .foldLeft(BoundingRectangle.EMPTY, BoundingRectangle::merge);
            } catch (IOException e) {
                System.err.println(e.toString()); // We can't handle that anyway. This is surely an error in project config
                throw new RuntimeException(e);
            }
        }

        @Setup(Level.Iteration)
        public void resetRandom() {
            x = new Random(0).doubles(bounds.min.getX(), bounds.max.getX()).iterator();
            y = new Random(0).doubles(bounds.min.getY(), bounds.max.getY()).iterator();
        }

        @Setup(Level.Invocation)
        public void newSegment() {
            Point2D s = new Point2D(x.next(), y.next());
            Point2D e = new Point2D(x.next(), y.next());
            seg = new Segment(s, e);
        }
    }

    @State(Scope.Thread)
    public static class SmallState extends MapState {
        public SmallState() {
            super("/small.osm.gz");
        }
    }

    @State(Scope.Thread)
    public static class MediumState extends MapState {
        public MediumState() {
            super("/medium.osm.gz");
        }
    }

    @State(Scope.Thread)
    public static class LargeState extends MapState {
        public LargeState() {
            super("/large.osm.gz");
        }
    }

    @State(Scope.Thread)
    public static class VeryLargeState extends MapState {
        public VeryLargeState() {
            super("/very large.osm.gz");
        }
    }

    @Benchmark
    public Point2D kdTreeNoIntersection(LargeState state) {
        // This is the worst case
        return state.kdTree.intersectWith(NO_INTERSECTION);
    }

    @Benchmark
    public Point2D kdTreePseudoRandomlyVeryLarge(VeryLargeState state) {
        return state.kdTree.intersectWith(state.seg);
    }

    @Benchmark
    public Point2D kdTreePseudoRandomlyLarge(LargeState state) {
        return state.kdTree.intersectWith(state.seg);
    }

    @Benchmark
    public Point2D kdTreePseudoRandomlyMedium(MediumState state) {
        return state.kdTree.intersectWith(state.seg);
    }

    @Benchmark
    public Point2D kdTreePseudoRandomlySmall(SmallState state) {
        return state.kdTree.intersectWith(state.seg);
    }

    @Benchmark
    public Point2D naivePseudoRandomlySmall(SmallState state) {
        return state.naive.intersectWith(state.seg);
    }

    @Benchmark
    public Point2D naivePseudoRandomlyMedium(MediumState state) {
        return state.naive.intersectWith(state.seg);
    }

    @Benchmark
    public Point2D naivePseudoRandomlyLarge(LargeState state) {
        return state.naive.intersectWith(state.seg);
    }

    @Benchmark
    public Point2D naivePseudoRandomlyVeryLarge(VeryLargeState state) {
        return state.naive.intersectWith(state.seg);
    }
}