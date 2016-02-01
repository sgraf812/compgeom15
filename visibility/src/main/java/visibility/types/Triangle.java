package visibility.types;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;


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

    public boolean isInside(Point2D p) {
        Point3D b = barycentricCoordinates(p);
        return b.getX() >= 0 && b.getY() >= 0 && b.getZ() >= 0;
    }

    public Point3D barycentricCoordinates(Point2D p) {
        Point2D v0 = b.subtract(a);
        Point2D v1 = c.subtract(a);
        Point2D v2 = p.subtract(a);
        double denom = v0.getX() * v1.getY() - v1.getX() * v0.getY();
        double u = (v2.getX() * v1.getY() - v1.getX() * v2.getY()) / denom;
        double v = (v0.getX() * v2.getY() - v2.getX() * v0.getY()) / denom;
        return new Point3D(u, v, 1.0-u-v);
    }

    public boolean isEndPointinTriangle(Point2D testpoint) {
        // TODO Auto-generated method stub
        if (((testpoint.getX() == this.a.getX()) && (testpoint.getY() == this.a.getY()))
                || ((testpoint.getX() == this.b.getX()) && (testpoint.getY() == this.b.getY()))
                || ((testpoint.getX() == this.c.getX()) && (testpoint.getY() == this.c.getY())))
            return true;
        else
            return false;
    }
}