package visibility.types;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;

public class PolygonwithHoles {
    public Polygon Outer;//clockwise
    public List<Polygon> Inter;//unclockwise
    public Polygon SimplePolygon;

    public PolygonwithHoles() {
        this.Outer = new Polygon();
        this.Inter = new ArrayList<Polygon>();
        SimplePolygon = new Polygon();
    }

    public PolygonwithHoles(Polygon Outer) {
        this.Outer = Outer;
        this.Outer.Changetoclockweise();
        this.Inter = new ArrayList<Polygon>();
        SimplePolygon = new Polygon();
    }

    public PolygonwithHoles(Polygon Outer, List<Polygon> Inter) {
        this.Outer = Outer;
        this.Inter = Inter;
        SimplePolygon = new Polygon();
    }

    public Polygon ComplexToSimplePolygon() {
        if (this.Outer.GetPoint(0).equals(this.Outer.GetPoint(this.Outer.GetPointsNumber() - 1))) {

            this.Outer.removePoint(this.Outer.GetPointsNumber() - 1);
        }
        for (int i = 0; i < this.Inter.size(); i++) {
            if (this.Inter.get(i).GetPointsNumber() != 0)
                if (this.Inter.get(i).GetPoint(0).equals(this.Inter.get(i).GetPoint(this.Inter.get(i).GetPointsNumber() - 1))) {

                    this.Inter.get(i).removePoint(this.Inter.get(i).GetPointsNumber() - 1);
                }
        }
        if (!this.Outer.IsClockweise()) {

            this.Outer.Changetoclockweise();
        }
        for (int i = 0; i < this.Inter.size(); i++) {
            if (this.Inter.get(i).GetPointsNumber() != 0) {
                if (this.Inter.get(i).IsClockweise()) {
                    this.Inter.get(i).ChangetoUnclockweise();
                }
            }
        }
        SortInterPolygonbyMostRight_X();
        for (int i = 0; i < this.Inter.size(); i++) {
            AddInterlineToOutline(this.Inter.get(i));
        }
        this.SimplePolygon = this.Outer;
        this.SimplePolygon.ChangetoUnclockweise();

        return this.SimplePolygon;
    }

    private void SortInterPolygonbyMostRight_X() {
        // TODO Auto-generated method stub
        ArrayList<Polygon> intersort = new ArrayList<Polygon>();
        ArrayList<Double> X = new ArrayList<Double>();

        for (int i = 0; i < this.Inter.size(); i++) {
            Double V_x = (double) -1111;
            for (int j = 0; j < this.Inter.get(i).GetPointsNumber(); j++)
                if (this.Inter.get(i).GetPoint(j).getX() > V_x)
                    V_x = this.Inter.get(i).GetPoint(j).getX();
            X.add(V_x);
        }
        while ((this.Inter.size() - intersort.size()) != 0) {
            double V_x = -1111;
            int index = 0;
            for (int i = 0; i < X.size(); i++) {
                if (X.get(i) > V_x) {
                    V_x = X.get(i);
                    index = i;
                }
            }
            intersort.add(Inter.get(index));
            X.set(index, (double) -1111);
        }

        this.Inter = intersort;
    }

    private void AddInterlineToOutline(Polygon polygon) {
        // TODO Auto-generated method stub
        int Interindex = FindMostRight(polygon);
        int Outerindex = FindOuterConnectionPoint(polygon, Interindex);
        ConnectOuterwithInter(polygon, Interindex, Outerindex);
    }

    private int FindMostRight(Polygon polygon) {
        // TODO Auto-generated method stub
        double V_x = -1111;
        int index = 0;
        for (int j = 0; j < polygon.GetPointsNumber(); j++)
            if (polygon.GetPoint(j).getX() > V_x) {
                V_x = polygon.GetPoint(j).getX();
                index = j;
            }
        return index;
    }

    private int FindOuterConnectionPoint(Polygon polygon, int interindex) {
        // TODO Auto-generated method stub
        int index = 0;
        Point2D WantedPoint = new Point2D(111111, -111111);
        Point2D Intersection = null;
        Point2D InterPoint = polygon.GetPoint(interindex);
        for (int i = 0; i < this.Outer.GetPointsNumber(); i++) {
            Point2D endPoint1;
            Point2D endPoint2;
            if (i == (this.Outer.GetPointsNumber() - 1)) {
                endPoint1 = new Point2D(this.Outer.GetPoint(i).getX(), this.Outer.GetPoint(i).getY());
                endPoint2 = new Point2D(this.Outer.GetPoint(0).getX(), this.Outer.GetPoint(0).getY());
            } else {
                endPoint1 = new Point2D(this.Outer.GetPoint(i).getX(), this.Outer.GetPoint(i).getY());
                endPoint2 = new Point2D(this.Outer.GetPoint(i + 1).getX(), this.Outer.GetPoint(i + 1).getY());
            }
            NewSegment s = new NewSegment(endPoint1, endPoint2);
            if (s.IsIntersectioPointhorizontal(InterPoint)) {
                Point2D IntersectionNow = s.HorizontalIntersection(InterPoint);
                if ((IntersectionNow.getX() >= InterPoint.getX()) && (IntersectionNow.getX() < WantedPoint.getX())) {
                    WantedPoint = IntersectionNow;
                    Intersection = IntersectionNow;

                    if (endPoint2.getX() >= endPoint1.getX())
                        if (i == (this.Outer.GetPointsNumber() - 1))
                            index = 0;
                        else
                            index = i + 1;
                    else
                        index = i;
                }
            }
        }
        Triangle t = new Triangle(InterPoint, Intersection, this.Outer.GetPoint(index));
        double mintangle = Math.abs(this.Outer.GetPoint(index).getY() - InterPoint.getY()) / Math.abs(this.Outer.GetPoint(index).getX() - InterPoint.getX());
        for (int i = 0; i < this.Outer.GetPointsNumber(); i++) {

            if (t.isInside(this.Outer.GetPoint(i))) {
                double tangle1 = Math.abs(this.Outer.GetPoint(i).getY() - InterPoint.getY()) / Math.abs(this.Outer.GetPoint(i).getX() - InterPoint.getX());
                if (tangle1 < mintangle) {
                    index = i;
                    mintangle = tangle1;
                }
            }

        }
        return index;
    }

    private void ConnectOuterwithInter(Polygon polygon, int interindex, int outerindex) {
        // TODO Auto-generated method stub
        Point2D outerpoint = this.Outer.GetPoint(outerindex);
        Point2D innnerpoint = polygon.GetPoint(interindex);
        int endindex = 0;
        int times = 0;
        for (int index = 0; index < this.Outer.GetPointsNumber(); index++) {
            if (this.Outer.GetPoint(index).equals(outerpoint)) {
                endindex = index;
                times++;
//				System.out.println("index"+index+outerpoint+times);
                if (index == (this.Outer.GetPointsNumber() - 1)) {
                    if (this.Outer.GetPoint(0).getY() < innnerpoint.getY()) {
                        outerindex = index;
                        break;
                    }
                } else if (this.Outer.GetPoint(index + 1).getY() < innnerpoint.getY()) {
                    outerindex = index;
                    break;
                }
            }
        }
        outerindex = endindex;
        int i = 0;
        while ((polygon.GetPointsNumber() + 1) != i) {
            Point2D mid = copy(polygon.GetPoint((interindex + i) % polygon.GetPointsNumber()));
            this.Outer.AddPointwithIndex(outerindex + i + 1, mid);
            i++;
        }
        Point2D p = copy(this.Outer.GetPoint(outerindex));
        this.Outer.AddPointwithIndex(outerindex + i + 1, p);
    }

    private Point2D copy(Point2D point) {
        // TODO Auto-generated method stub
        Point2D p = new Point2D(point.getX(), point.getY());
        return p;
    }

}
