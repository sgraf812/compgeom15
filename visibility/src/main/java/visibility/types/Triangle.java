package visibility.types;

import javafx.geometry.Point2D;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class Triangle implements Iterable<Point2D> {
    public final Point2D a, b, c;

    public Triangle(Point2D a, Point2D b, Point2D c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public Point2D get(int index) {
        if (index == 0) return this.a;
        else if (index == 1) return this.b;
        else if (index == 2) return this.c;
        else throw new IndexOutOfBoundsException("index");
    }

    public static Triangle fromDelaunayTriangle(DelaunayTriangle t) {
        return new Triangle(
                fromTriangulationPoint(t.points[0]),
                fromTriangulationPoint(t.points[1]),
                fromTriangulationPoint(t.points[2])
        );
    }

    private static Point2D fromTriangulationPoint(TriangulationPoint p) {
        return new Point2D(p.getX(), p.getY());
    }

    @Override
    public Iterator<Point2D> iterator() {
        return new Iterator<Point2D>() {
            private int i = 0;
            @Override
            public boolean hasNext() {
               return i < 3;
            }

            @Override
            public Point2D next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(i++);
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Point2D> action) {
        action.accept(a);
        action.accept(b);
        action.accept(c);
    }
}
