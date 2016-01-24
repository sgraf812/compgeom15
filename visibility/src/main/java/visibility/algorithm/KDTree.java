package visibility.algorithm;

import javafx.geometry.Point2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import visibility.types.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static org.jooq.lambda.tuple.Tuple.tuple;

public class KDTree implements SpatialDataStructure {

    private static final int TRIANGLES_PER_LEAF = 1;
    private static final Comparator<TriangleRef> CMP_MIN_X = comparing(i -> i.bounds.min.getX());
    private static final Comparator<TriangleRef> CMP_MIN_Y = comparing(i -> i.bounds.min.getY());
    private static final Comparator<TriangleRef> CMP_MID_X = comparing(i -> i.bounds.mid.getX());
    private static final Comparator<TriangleRef> CMP_MID_Y = comparing(i -> i.bounds.mid.getY());
    private static final Comparator<TriangleRef> CMP_MAX_X = comparing(i -> i.bounds.max.getX());
    private static final Comparator<TriangleRef> CMP_MAX_Y = comparing(i -> i.bounds.max.getY());

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
            return seg.splitAtXorY(node.splitAtX, node.splitValue).map((startOnLeftSide, start, end) -> {
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

        assert TRIANGLES_PER_LEAF > 0;
        return new KDTree(buildTree(refs));
    }

    private static KDNode buildTree(TriangleRef[] refs) {
        // Uses the object median method

        if (refs.length < 1) {
            return null;
        }

        final BoundingRectangle bounds = Seq.seq(Stream.of(refs))
                .map(ref -> ref.bounds)
                .foldLeft(BoundingRectangle.EMPTY, BoundingRectangle::merge);

        if (refs.length <= TRIANGLES_PER_LEAF) {
            return KDNode.leaf(bounds, refs);
        }

        final Point2D extent = bounds.extent();

        // we split on refs[mid].bound.mid.
        int mid = refs.length / 2;

        boolean splitAtX = extent.getX() > extent.getY();
        final Comparator<TriangleRef> minCmp = splitAtX ? CMP_MIN_X : CMP_MIN_Y;
        final Comparator<TriangleRef> maxCmp = splitAtX ? CMP_MAX_X : CMP_MAX_Y;
        final Comparator<TriangleRef> midCmp = splitAtX ? CMP_MID_X : CMP_MID_Y;

        final TriangleRef split = ArrayUtils.quickSelect(refs, mid, midCmp);

        assert split != null; // refs.length > 1 after all

        final double splitValue = splitAtX ? split.bounds.mid.getX() : split.bounds.mid.getY();

        // Now find the index range of the elements which intersect the split hyper plane
        final TriangleRef splitDeg = new TriangleRef(null, new BoundingRectangle(split.bounds.mid, split.bounds.mid));
        // bothStart will point to the first ref whose max point is greater than or equal to splitDeg
        final int bothStart = partition(splitDeg, refs, 0, mid, maxCmp);
        // bothEnd will point to the first ref whose min point is greater than or equal to splitDeg
        final int bothEnd = partition(splitDeg, refs, mid, refs.length, minCmp);

        // Triangles in [bothStart, bothEnd) belong to both nodes, but we should refit their bounds.
        final TriangleRef[] leftRefit = new TriangleRef[bothEnd - bothStart];
        final TriangleRef[] rightRefit = new TriangleRef[bothEnd - bothStart];
        // first copy triangle refs which don't intersect the split
        int leftEnd = 0;
        int rightEnd = 0;
        // Now fit the remaining triangle refs to their new bounds and skip empty bounds.
        for (int i = bothStart; i < bothEnd; ++i) {
            final TriangleRef t = refs[i];
            final Tuple2<BoundingRectangle, BoundingRectangle> b = splitBoundingRectAt(splitAtX, splitValue, t);
            if (!b.v1.isEmpty()) {
                leftRefit[leftEnd++] = new TriangleRef(t.triangle, b.v1);
            }
            if (!b.v2.isEmpty()) {
                rightRefit[rightEnd++] = new TriangleRef(t.triangle, b.v2);
            }
        }

        final TriangleRef[] leftNodes = new TriangleRef[bothStart + leftEnd];
        final TriangleRef[] rightNodes = new TriangleRef[rightEnd + refs.length - bothEnd];

        System.arraycopy(refs, 0, leftNodes, 0, bothStart);
        System.arraycopy(leftRefit, 0, leftNodes, bothStart, leftEnd);
        System.arraycopy(rightRefit, 0, rightNodes, 0, rightEnd);
        System.arraycopy(refs, bothEnd, rightNodes, rightEnd, refs.length - bothEnd);

        for (TriangleRef r : leftNodes) {
            if (splitAtX) {
                assert r.bounds.max.getX() <= splitValue;
            } else {
                assert r.bounds.max.getY() <= splitValue;
            }
        }
        for (TriangleRef r : rightNodes) {
            if (splitAtX) {
                assert r.bounds.min.getX() >= splitValue;
            } else {
                assert r.bounds.min.getY() >= splitValue;
            }
        }

        assert leftNodes.length + rightNodes.length >= refs.length;

        if (leftNodes.length == refs.length || rightNodes.length == refs.length) {
            if (refs.length > 20) {
                //System.out.println("refs " + refs.length + " l " + leftNodes.length + " r " + rightNodes.length);
            }

            // We can't do much better because of overlaps, I'm afraid.
            // This is just a heuristic and may yield bad results on degenerate cases.
            // Applying SAH might alleviate this.
            return KDNode.leaf(bounds, refs);
        }


        KDNode left = buildTree(leftNodes);
        KDNode right = buildTree(rightNodes);

        if (splitAtX) {
            assert left == null || left.bounds.max.getX() <= splitValue;
            assert right == null || right.bounds.min.getX() >= splitValue;
        } else {
            assert left == null || left.bounds.max.getY() <= splitValue;
            assert right == null || right.bounds.min.getY() >= splitValue;
        }

        if (left == null) {
            return right;
        } else if (right == null) {
            return left;
        } else {
            return KDNode.inner(left.bounds.merge(right.bounds), left, right, splitAtX, splitValue);
        }
    }

    private static Tuple2<BoundingRectangle, BoundingRectangle> splitBoundingRectAt(boolean atX, double v, TriangleRef t) {
        // X coordinates of the BR are easy. Y is found through linear interpolation.

        final List<Point2D> left = new ArrayList<>(4);
        final List<Point2D> right = new ArrayList<>(4);
        assert t.triangle != null;
        for (Point2D p : t.triangle) {
            if ((atX && p.getX() < v) || (!atX && p.getY() < v)) {
                left.add(p);
            } else {
                right.add(p);
            }
        }

        int nleft = left.size();
        int nright = right.size();
        for (int i = 0; i < nleft; ++i) {
            for (int j = 0; j < nright; ++j) {
                Point2D p = left.get(i);
                Point2D q = right.get(j);

                // interpolate pq on x
                if (atX) {
                    double y = (v - p.getX()) / (q.getX() - p.getX()) * (q.getY() - p.getY()) + p.getY();
                    left.add(new Point2D(v, y));
                    right.add(new Point2D(v, y));
                } else {
                    double x = (v - p.getY()) / (q.getY() - p.getY()) * (q.getX() - p.getX()) + p.getX();
                    left.add(new Point2D(x, v));
                    right.add(new Point2D(x, v));
                }
            }
        }

        BoundingRectangle v1 = BoundingRectangle.fromPoints(left).intersect(t.bounds);
        BoundingRectangle v2 = BoundingRectangle.fromPoints(right).intersect(t.bounds);
        if (atX) {
            assert v1.max.getX() <= v;
            assert v2.min.getX() >= v;
        } else {
            assert v1.max.getY() <= v;
            assert v2.min.getY() >= v;
        }

        return tuple(
                v1,
                v2
        );
    }

    private static int partition(TriangleRef pivot, TriangleRef[] refs, int start, int mid, Comparator<TriangleRef> comparator) {
        int left = start;
        int right = mid - 1;

        while (true) {
            while (left < right && comparator.compare(refs[left], pivot) < 0) {
                left++;
            }
            // left is the index of a candidate which is greater than or equal to the pivot.
            // Or left == right

            while (left < right && comparator.compare(refs[right], pivot) >= 0) {
                right--;
            }
            // right is the index of a candidate which is smaller than the pivot.
            // Or left == right

            if (left < right) {
                ArrayUtils.swap(refs, left, right);
            } else {
                break;
            }
        }

        assert left == right;

        while (right >= 0 && comparator.compare(refs[right], pivot) >= 0) {
            right--;
        }

        right++;
        // right now points to the first element that is greater than or equal to pivot.

        return right;
    }


    private static class TriangleRef {
        public final Triangle triangle;
        public final BoundingRectangle bounds;
        boolean alreadyChecked; // This is for a mailboxing mechanism

        public TriangleRef(Triangle triangle) {
            this(triangle, BoundingRectangle.fromPoints(triangle));
            assert triangle != null;
        }

        public TriangleRef(Triangle triangle, BoundingRectangle bounds) {
            this.triangle = triangle;
            this.bounds = bounds;
            this.alreadyChecked = false;
        }
    }

    private static class KDNode {
        public final BoundingRectangle bounds;
        public final KDNode right;
        public final KDNode left;
        public final TriangleRef[] refs;
        public final boolean splitAtX;
        public final double splitValue;

        private KDNode(BoundingRectangle bounds, KDNode left, KDNode right, TriangleRef[] refs, boolean splitAtX, double splitValue) {
            this.bounds = bounds;
            this.right = right;
            this.left = left;
            this.refs = refs;
            this.splitAtX = splitAtX;
            this.splitValue = splitValue;
        }

        public static KDNode leaf(BoundingRectangle bounds, TriangleRef[] refs) {
            return new KDNode(bounds, null, null, refs, false, 0.0);
        }

        public static KDNode inner(BoundingRectangle bounds, KDNode left, KDNode right, boolean splitAtX, double splitValue) {
            return new KDNode(bounds, left, right, null, splitAtX, splitValue);
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
            if (node.splitAtX) {
                Point2D start = new Point2D(node.splitValue, node.bounds.min.getY());
                Point2D end = new Point2D(node.splitValue, node.bounds.max.getY());
                visitor.accept(new Segment(start, end), depth);
                visitHalfPlanes(visitor, node.left, depth+1);
                //visitHalfPlanes(visitor, node.right, depth+1);
            } else {
                Point2D start = new Point2D(node.bounds.min.getX(), node.splitValue);
                Point2D end = new Point2D(node.bounds.max.getX(), node.splitValue);
                visitor.accept(new Segment(start, end), depth);
                visitHalfPlanes(visitor, node.left, depth+1);
                //visitHalfPlanes(visitor, node.right, depth+1);
            }
        }
    }
}
