package visibility.gui;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.primitives.Point;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.point.TPoint;

public class Viewport {

    private final Rectangle2D viewport;

    public Viewport(Rectangle2D viewport) {
        this.viewport = viewport;
    }

    static Viewport fromPolygons(Iterable<Polygon> polygons) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Polygon p : polygons) {
            for (Point vert : p.getPoints()) {
                minX = Math.min(minX, vert.getX());
                minY = Math.min(minY, vert.getY());
                maxX = Math.max(maxX, vert.getX());
                maxY = Math.max(maxY, vert.getY());
            }
        }

        return new Viewport(new Rectangle2D(minX, minY, maxX - minX, maxY - minY));
    }

    public Point screenToViewport(Bounds screen, double x, double y) {
        return new TPoint(
                viewport.getMinX() + viewport.getWidth()*(x - screen.getMinX())/(screen.getWidth()),
                viewport.getMaxY() - viewport.getHeight()*(y - screen.getMinY())/(screen.getHeight())
        );
    }

    public Point viewportToScreen(Bounds screen, Point p) {
        return new TPoint(
                screen.getMinX() + screen.getWidth()*(p.getX() - viewport.getMinX())/(viewport.getWidth()),
                screen.getMaxY() - screen.getHeight()*(p.getY() - viewport.getMinY())/(viewport.getHeight())
        );
    }
}
