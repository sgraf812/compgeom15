package visibility.types;

import javafx.geometry.Point2D;

public class NewSegment {
    Point2D[] endpoint = new Point2D[2];

    public NewSegment(Point2D A, Point2D B) {
        endpoint[0] = A;
        endpoint[1] = B;
    }

    public boolean IsIntersectioPointhorizontal(Point2D P) {
        if ((endpoint[0].getY() > P.getY() && endpoint[1].getY() > P.getY()) || (endpoint[0].getY() < P.getY() && endpoint[1].getY() < P.getY()))
            return false;
        else
            return true;
    }

    public Point2D HorizontalIntersection(Point2D P) {
        if (IsIntersectioPointhorizontal(P)) {
            Point2D a;
            if ((endpoint[0].getX() - endpoint[1].getX()) == 0) {
                a = new Point2D(endpoint[0].getX(), P.getY());
            } else {
                double t = (P.getY() - (endpoint[0].getX() * endpoint[1].getY() - endpoint[1].getX() * endpoint[0].getY()) / (endpoint[0].getX() - endpoint[1].getX()))
                        * (endpoint[1].getX() - endpoint[0].getX()) / (endpoint[1].getY() - endpoint[0].getY());
                a = new Point2D(t, P.getY());
            }
            return a;
        } else {
            System.out.print(" no intersection ");
            return null;
        }
    }

    public static void main(String args[]) {
        Point2D a = new Point2D(0, 0);
        Point2D b = new Point2D(2, 2);
        Point2D v = new Point2D(1, 0.5);
        NewSegment s = new NewSegment(a, b);
        System.out.print(s.HorizontalIntersection(v).getX() + "  " + s.HorizontalIntersection(v).getY());
    }
}
