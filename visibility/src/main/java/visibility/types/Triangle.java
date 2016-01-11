package visibility.types;

import javafx.geometry.Point2D;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

public class Triangle {
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
}
