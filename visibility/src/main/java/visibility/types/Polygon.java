package visibility.types;

import javafx.geometry.Point2D;

import java.util.List;

public class Polygon {
    private List<Point2D> outerFace;
    private List<List<Point2D>> holes;

    public Polygon(List<Point2D> outerFace, List<List<Point2D>> holes) {
        this.outerFace = outerFace;
        this.holes = holes;
    }

    public List<Point2D> getOuterFace() {
        return outerFace;
    }

    public List<List<Point2D>> getHoles() {
        return holes;
    }
}
