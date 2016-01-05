package visibility.types;

import javafx.geometry.Point2D;
import org.jetbrains.annotations.NotNull;

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
