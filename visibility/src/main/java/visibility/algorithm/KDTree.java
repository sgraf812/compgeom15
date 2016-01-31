package visibility.algorithm;

import javafx.geometry.Point2D;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import visibility.types.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static org.jooq.lambda.tuple.Tuple.tuple;

public class KDTree implements SpatialDataStructure {

    private static final double TRAVERSAL_COST = 1;
    private static final double INTERSECTION_COST = 0.004; // median was 0.00347, avg 0.00375, so we should be good

    private final KDNode root;

    private KDTree(@Nullable KDNode root) {
        this.root = root;
    }

    @Override
    public Point2D intersectWith(Segment s) {
        if (root == null) {
            return null;
        }

        return intersect(root, s);
    }

    private static Point2D intersect(KDNode node, Segment seg) {
        if (node.isLeaf()) {
            Intersection min = null;
            for (TriangleRef r : node.refs) {
                Intersection i = seg.intersectTriangle(r.triangle);
                if (i != null) {
                    if (min == null || min.getDistance() > i.getDistance()) {
                        min = i;
                    }
                }
            }
            return min == null ? null : min.getIntersection();
        } else {
            boolean splitAtX = node.splittingPlane.dimension == Dimension.X;
            double splitValue = node.splittingPlane.splitValue;
            return seg.splitAtXorY(splitAtX, splitValue).map((startOnLeftSide, start, end) -> {
                if (startOnLeftSide) {
                    Point2D p = intersect(node.left, start);
                    return p != null || end == null ? p : intersect(node.right, end);
                } else {
                    Point2D p = intersect(node.right, start);
                    return p != null || end == null ? p : intersect(node.left, end);
                }
            });
        }
    }


    public static KDTree fromTriangles(List<Triangle> triangles) {
        TriangleRef[] refs = Seq.seq(triangles)
                .map(TriangleRef::new)
                .toArray(TriangleRef[]::new);

        final BoundingRectangle bounds = Seq.seq(Stream.of(refs))
                .map(ref -> ref.bounds)
                .foldLeft(BoundingRectangle.EMPTY, BoundingRectangle::merge);

        return new KDTree(buildTreeSAH(refs, bounds, 0, Double.MAX_VALUE));
    }

    private static KDNode buildTreeSAH(TriangleRef[] refs, BoundingRectangle bounds, int depth, double lastCost) {
        double minCost = Double.MAX_VALUE;
        SplittingPlane minPlane = null;
        SplittingPlaneAffiliation minPlaneAffiliation = null;
        int minnleft = Integer.MAX_VALUE;
        int minnright = Integer.MAX_VALUE;

        List<TriangleEvent> events = new ArrayList<>(refs.length * 2);

        for (Dimension d : Dimension.values()) {
            for (TriangleRef ref : refs) {
                BoundingRectangle br = bounds.intersect(ref.bounds);
                if (d.getValue(br.min) == d.getValue(br.max)) {
                    // The triangle is planar after clipping it to the current Voxel
                    events.add(new TriangleEvent(ref.triangle, new SplittingPlane(d.getValue(br.min), d), EventType.PLANAR));
                } else {
                    events.add(new TriangleEvent(ref.triangle, new SplittingPlane(d.getValue(br.min), d), EventType.START));
                    events.add(new TriangleEvent(ref.triangle, new SplittingPlane(d.getValue(br.max), d), EventType.END));
                }
            }

            events.sort((a, b) -> {
                int i1 = (int) Math.signum(a.p.splitValue - b.p.splitValue);
                int i2 = (int) Math.signum(a.type.ord - b.type.ord);
                return i1 != 0 ? i1 : i2;
            }); // As per the order in the paper: Sort by splitValue, then by EventType: END < PLANAR < START

            // Now we sweep over the event list and find the minimum cost splitting plane for this dimension.
            int nleft = 0;
            int nright = refs.length;

            for (int i = 0; i < events.size(); ) {
                TriangleEvent evt = events.get(i);
                int pend = 0;
                int pplanar = 0;
                int pstart = 0;
                final SplittingPlane p = evt.p;

                // aggregate all p* for the same splitValue
                while (evt.p.splitValue == p.splitValue) {
                    switch (evt.type) {
                        case END:
                            pend++;
                            break;
                        case PLANAR:
                            pplanar++;
                            break;
                        case START:
                            pstart++;
                            break;
                    }

                    if (++i < events.size()) {
                        evt = events.get(i);
                    } else {
                        break;
                    }
                }

                // Now we got all p* values for the current splitValue.
                // Move plane *onto* p.
                nright -= pplanar;
                nright -= pend;

                Tuple2<Double, SplittingPlaneAffiliation> result = surfaceAreaHeuristic(bounds, p, nleft, nright, pplanar);
                if (result.v1 < minCost) {
                    minCost = result.v1;
                    minPlane = p;
                    minPlaneAffiliation = result.v2;
                    minnleft = nleft;
                    minnright = nright;
                    switch (minPlaneAffiliation) {
                        case LEFT:
                            minnleft += pplanar;
                            break;
                        case RIGHT:
                            minnright += pplanar;
                            break;
                    }
                }

                // Move plane *beyond* p
                nleft += pstart;
                nleft += pplanar;

                assert nleft + nright >= refs.length;
                assert minnleft + minnright >= refs.length;
            }

            events.clear();
        }

        if (INTERSECTION_COST * refs.length < minCost || depth > 30 || lastCost <= minCost) {
            return KDNode.leaf(bounds, refs);
        }

        assert refs.length > 1;

        // We found the best splitting plane and the associated costs, all that is left to do is split.
        TriangleRef[] left = new TriangleRef[minnleft];
        TriangleRef[] right = new TriangleRef[minnright];
        int l = 0;
        int r = 0;
        BoundingRectangle lv = BoundingRectangle.EMPTY;
        BoundingRectangle rv = BoundingRectangle.EMPTY;
        for (TriangleRef ref : refs) {
            if (minPlane.dimension.getValue(ref.bounds.min) == minPlane.splitValue
                    && minPlane.dimension.getValue(ref.bounds.max) == minPlane.splitValue) {
                if (minPlaneAffiliation == SplittingPlaneAffiliation.RIGHT) {
                    right[r++] = ref;
                    rv = rv.merge(ref.bounds);
                } else {
                    left[l++] = ref;
                    lv = lv.merge(ref.bounds);
                }
            } else {
                if (minPlane.dimension.getValue(ref.bounds.min) < minPlane.splitValue) {
                    left[l++] = ref;
                    lv = lv.merge(ref.bounds);
                }
                if (minPlane.dimension.getValue(ref.bounds.max) > minPlane.splitValue) {
                    right[r++] = ref;
                    rv = rv.merge(ref.bounds);
                }
                // In the remaining case where min == max == splitValue, the triangle
                // is planar (extent == 0) and will we dealt with in the other if branch
                // according to minPlaneAffiliation.
            }
        }

        lv = splitBoundingRect(lv.intersect(bounds), minPlane).v1; // I hope this is accurate enough. don't want clip all triangles
        rv = splitBoundingRect(rv.intersect(bounds), minPlane).v2;

        assert l == left.length;
        assert r == right.length;

        try {

            KDNode leftChild = buildTreeSAH(left, lv, depth + 1, minCost);
            KDNode rightChild = buildTreeSAH(right, rv, depth + 1, minCost);

            assert leftChild != null;
            assert rightChild != null;

            return KDNode.inner(bounds, leftChild, rightChild, minPlane);

        } catch (StackOverflowError ignored) {
            // In case we mess up and recurse endlessly.
            System.out.println("Building up the KDTree blew up the stack");
            return KDNode.leaf(bounds, refs);
        }
    }

    private static Tuple2<Double, SplittingPlaneAffiliation> surfaceAreaHeuristic(BoundingRectangle V, SplittingPlane p, int nleft, int nright, int nplanar) {
        Tuple2<BoundingRectangle, BoundingRectangle> split = splitBoundingRect(V, p);

        final double pleft = surfaceArea(split.v1) / surfaceArea(V);
        final double pright = surfaceArea(split.v2) / surfaceArea(V);

        final double costWithLeftAffiliation = calculateCost(pleft, pright, nleft + nplanar, nright);
        final double costWithRightAffiliation = calculateCost(pleft, pright, nleft, nright + nplanar);

        return costWithLeftAffiliation < costWithRightAffiliation
                ? tuple(costWithLeftAffiliation, SplittingPlaneAffiliation.LEFT)
                : tuple(costWithRightAffiliation, SplittingPlaneAffiliation.RIGHT);
    }

    private static double calculateCost(double pleft, double pright, int nleft, int nright) {
        final double lambda = nleft == 0 || nright == 0 ? 0.8 : 1.0;
        // lambda doesn't seem to work for us...
        return (TRAVERSAL_COST + INTERSECTION_COST * (pleft * nleft + pright * nright));
    }

    private static double surfaceArea(BoundingRectangle v) {
        Point2D e = v.extent();
        return 2 * (e.getX() + e.getY());
    }

    private static Tuple2<BoundingRectangle, BoundingRectangle> splitBoundingRect(BoundingRectangle bounds, SplittingPlane p) {
        switch (p.dimension) {
            case X:
                return tuple(
                        new BoundingRectangle(
                                bounds.min,
                                new Point2D(Math.min(bounds.max.getX(), p.splitValue), bounds.max.getY())),
                        new BoundingRectangle(
                                new Point2D(Math.max(bounds.min.getX(), p.splitValue), bounds.min.getY()),
                                bounds.max)
                );
            case Y:
                return tuple(
                        new BoundingRectangle(
                                bounds.min,
                                new Point2D(bounds.max.getX(), Math.min(bounds.max.getY(), p.splitValue))),
                        new BoundingRectangle(
                                new Point2D(bounds.min.getX(), Math.max(bounds.min.getY(), p.splitValue)),
                                bounds.max)
                );
        }
        assert false;
        return tuple(null, null);
    }


    private static class TriangleRef {
        public final Triangle triangle;
        public final BoundingRectangle bounds;
        //boolean alreadyChecked; // This is for a mailboxing mechanism

        public TriangleRef(Triangle triangle) {
            this(triangle, BoundingRectangle.fromPoints(triangle));
            assert triangle != null;
        }

        public TriangleRef(Triangle triangle, BoundingRectangle bounds) {
            this.triangle = triangle;
            this.bounds = bounds;
        }
    }

    private static class KDNode {
        public final BoundingRectangle bounds;
        public final KDNode right;
        public final KDNode left;
        public final TriangleRef[] refs;
        public final SplittingPlane splittingPlane;

        private KDNode(BoundingRectangle bounds, KDNode left, KDNode right, TriangleRef[] refs, SplittingPlane splittingPlane) {
            this.bounds = bounds;
            this.right = right;
            this.left = left;
            this.refs = refs;
            this.splittingPlane = splittingPlane;
        }

        public static KDNode leaf(BoundingRectangle bounds, TriangleRef[] refs) {
            return new KDNode(bounds, null, null, refs, null);
        }

        public static KDNode inner(BoundingRectangle bounds, KDNode left, KDNode right, SplittingPlane p) {
            return new KDNode(bounds, left, right, null, p);
        }

        public boolean isLeaf() {
            boolean isLeaf = left == null;
            assert isLeaf == (right == null);
            assert isLeaf == (refs != null);
            assert !isLeaf || refs.length > 0;
            return left == null;
        }
    }

    public void visitHalfPlanes(BiConsumer<Segment, Integer> visitor) {
        if (root == null) return;

        visitHalfPlanes(visitor, root, 0);
    }

    public void visitHalfPlanes(BiConsumer<Segment, Integer> visitor, KDNode node, int depth) {
        if (node.isLeaf()) { //|| depth > 8) {
            return;
        } else {
            if (node.splittingPlane.dimension == Dimension.X) {
                Point2D start = new Point2D(node.splittingPlane.splitValue, node.bounds.min.getY());
                Point2D end = new Point2D(node.splittingPlane.splitValue, node.bounds.max.getY());
                visitor.accept(new Segment(start, end), depth);
                visitHalfPlanes(visitor, node.left, depth + 1);
                //visitHalfPlanes(visitor, node.right, depth+1);
            } else {
                Point2D start = new Point2D(node.bounds.min.getX(), node.splittingPlane.splitValue);
                Point2D end = new Point2D(node.bounds.max.getX(), node.splittingPlane.splitValue);
                visitor.accept(new Segment(start, end), depth);
                visitHalfPlanes(visitor, node.left, depth + 1);
                //visitHalfPlanes(visitor, node.right, depth+1);
            }
        }
    }

    private static class SplittingPlane {
        public final double splitValue;
        public final Dimension dimension;

        private SplittingPlane(double splitValue, Dimension dimension) {
            this.splitValue = splitValue;
            this.dimension = dimension;
        }
    }

    private static class TriangleEvent {
        public final Triangle t;
        public final SplittingPlane p;
        public final EventType type;

        TriangleEvent(Triangle t, SplittingPlane p, EventType type) {
            this.t = t;
            this.p = p;
            this.type = type;
        }
    }

    private enum EventType {
        END(0),
        PLANAR(1),
        START(2);

        private final int ord;

        EventType(int i) {
            this.ord = i;
        }
    }

    private enum Dimension {
        X,
        Y;

        public double getValue(Point2D p) {
            switch (this) {
                case X:
                    return p.getX();
                case Y:
                    return p.getY();
            }
            assert false;
            return 0;
        }
    }

    private enum SplittingPlaneAffiliation {
        LEFT,
        RIGHT
    }
}
