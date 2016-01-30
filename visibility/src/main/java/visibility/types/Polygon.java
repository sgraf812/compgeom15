package visibility.types;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;

public class Polygon {
    private List<Point2D> vertices;

    public Polygon() {
        this.vertices = new ArrayList<Point2D>();
    }

    public Polygon(List<Point2D> vertices) {
        this.vertices = vertices;
    }

    public void SetVertices(ArrayList<Point2D> vertices) {
        this.vertices = vertices;
    }

    public void SetPointwithIndex(int index, Point2D point) {
        this.vertices.set(index, point);
    }

    public int GetPointsNumber() {
        return this.vertices.size();
    }

    public Point2D GetPoint(int index) {
        return this.vertices.get(index);
    }

    public void removePoint(int index) {
        this.vertices.remove(index);
    }

    public void AddPoint(Point2D point) {
        this.vertices.add(point);
    }

    public void AddPointwithIndex(int index, Point2D point) {
        this.vertices.add(index, point);
    }

    public void ChangetoUnclockweise() {
        if (IsClockweise()) {
            int length = vertices.size() - 1;

            for (int i = 0; i < (length + 1) / 2; i++) {
                Point2D s = vertices.get(i);
                vertices.set(i, vertices.get(length - i));
                vertices.set(length - i, s);
            }
        }
    }

    public void Changetoclockweise() {
        if (!IsClockweise()) {
            int length = vertices.size() - 1;

            for (int i = 0; i < (length + 1) / 2; i++) {
                Point2D s = vertices.get(i);
                vertices.set(i, vertices.get(length - i));
                vertices.set(length - i, s);
            }
        }
    }

    public boolean IsClockweise() {
        double min = this.vertices.get(0).getY();
        int minIndex = 0;
        for (int i = 1; i < vertices.size(); i++) {
            if (vertices.get(i).getY() < min) {
                min = vertices.get(i).getY();
                minIndex = i;
            }
        }
        Point2D prev = vertices.get((minIndex - 1 + vertices.size()) % vertices.size());
        Point2D next = vertices.get((minIndex + 1) % vertices.size());

        return !(turnTest(prev, vertices.get(minIndex), next));
    }

    public static boolean turnTest(Point2D p1, Point2D p2, Point2D p3) {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        return ((x2 - x1) * (y3 - y2) - (x3 - x2) * (y2 - y1) > 0);
    }

    public void Print() {
        for (int i = 0; i < this.vertices.size(); i++) {
            System.out.println(i + "   " + this.vertices.get(i));
        }
    }
}
