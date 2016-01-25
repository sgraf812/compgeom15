package visibility.types;

import javafx.geometry.Point2D;

import java.util.function.BiFunction;

public class BoundingRectangle {
    public static final BoundingRectangle EMPTY =
            new BoundingRectangle(
                    new Point2D(Double.MAX_VALUE, Double.MAX_VALUE),
                    new Point2D(-Double.MAX_VALUE, -Double.MAX_VALUE));
    public static final BoundingRectangle EVERYTHING =
            new BoundingRectangle(
                    new Point2D(-Double.MAX_VALUE, -Double.MAX_VALUE),
                    new Point2D(Double.MAX_VALUE, Double.MAX_VALUE));
    public final Point2D min;
    public final Point2D max;
    public final Point2D mid;

    public BoundingRectangle(Point2D min, Point2D max) {
        this.min = min;
        this.max = max;
        this.mid = min.add(max).multiply(0.5);
    }

    public Point2D extent() {
        return this.max.subtract(this.min);
    }

    public BoundingRectangle merge(BoundingRectangle other) {
        return new BoundingRectangle(
                componentWise(Math::min, this.min, other.min),
                componentWise(Math::max, this.max, other.max)
        );
    }

    public static BoundingRectangle fromPoints(Iterable<Point2D> t) {
        Point2D min = new Point2D(Double.MAX_VALUE, Double.MAX_VALUE);
        Point2D max = new Point2D(-Double.MAX_VALUE, -Double.MAX_VALUE);
        for (Point2D p : t) {
            min = componentWise(Math::min, p, min);
            max = componentWise(Math::max, p, max);
        }
        return new BoundingRectangle(min, max);
    }

    public static Point2D componentWise(BiFunction<Double, Double, Double> folder, Point2D a, Point2D b) {
        return new Point2D(folder.apply(a.getX(), b.getX()), folder.apply(a.getY(), b.getY()));
    }

    public BoundingRectangle intersect(BoundingRectangle other) {
        return new BoundingRectangle(
                componentWise(Math::max, this.min, other.min),
                componentWise(Math::min, this.max, other.max)
        );
    }

    public boolean isEmpty() {
        return min.getX() > max.getX() || min.getY() > max.getY();
    }
}
