package types;

import javafx.geometry.Point2D;
import org.openjdk.jmh.annotations.*;
import visibility.types.Intersection;
import visibility.types.Segment;
import visibility.types.Triangle;

import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TriangleIntersectionBenchmarks {
    private static final Segment HIT = new Segment(new Point2D(0, 0), new Point2D(5, 5));
    private static final Segment NO_HIT = new Segment(new Point2D(0, 0), new Point2D(-5, 5));
    private static final Triangle TRIANGLE = new Triangle(
            new Point2D(0, 5),
            new Point2D(2, 1),
            new Point2D(5, 0)
    );

    @Benchmark
    public Intersection intersectTriangleHit() {
        return HIT.intersectTriangle(TRIANGLE);
    }

    @Benchmark
    public Intersection intersectTriangleNoHit() {
        return NO_HIT.intersectTriangle(TRIANGLE);
    }

    @State(Scope.Thread)
    public static class RandomTriangles {
        private PrimitiveIterator.OfDouble d;
        public Segment seg;

        @Setup(Level.Iteration)
        public void resetRandom() {
            d = new Random(0).doubles(0, 5).iterator();
        }

        @Setup(Level.Invocation)
        public void newSegment() {
            Point2D s = new Point2D(d.next(), d.next());
            Point2D e = new Point2D(d.next(), d.next());
            seg = new Segment(s, e);
        }
    }

    @Benchmark
    public Intersection intersectTrianglePseudoRandomly(RandomTriangles state) {
        return state.seg.intersectTriangle(TRIANGLE);
    }
}
