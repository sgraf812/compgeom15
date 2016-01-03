package visibility.types;

import com.sun.istack.internal.NotNull;
import javafx.geometry.Point2D;

public class Intersection {
    private final Point2D intersection;
    private final double distance;

    public Intersection(@NotNull Point2D intersection, double distance) {
        this.intersection = intersection;
        this.distance = distance;
    }

    @NotNull
    public Point2D getIntersection() {
        return intersection;
    }

    public double getDistance() {
        return distance;
    }
}
