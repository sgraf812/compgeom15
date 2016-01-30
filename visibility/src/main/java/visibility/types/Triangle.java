package visibility.types;

import javafx.geometry.Point2D;


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
        double vx2 = p.getX() - a.getX();
        double vy2 = p.getY() - a.getY();
        double vx1 = this.b.getX() - this.a.getX();
        double vy1 = this.b.getY() - this.a.getY();
        double vx0 = this.c.getX() - this.a.getX();
        double vy0 = this.c.getY() - this.a.getY();

        double dot00 = vx0 * vx0 + vy0 * vy0;
        double dot01 = vx0 * vx1 + vy0 * vy1;
        double dot02 = vx0 * vx2 + vy0 * vy2;
        double dot11 = vx1 * vx1 + vy1 * vy1;
        double dot12 = vx1 * vx2 + vy1 * vy2;
        double invDenom = (double) (1.0 / (dot00 * dot11 - dot01 * dot01));
        double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return ((u >= 0) && (v >= 0) && (u + v <= 1));
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