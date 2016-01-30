package visibility.types;

import java.util.ArrayList;

import javafx.geometry.Point2D;

public class EarClipping {
    Polygon polygon;
    ArrayList<Triangle> TriangleArray;
    ArrayList<Integer> EarArray;

    public EarClipping(Polygon p) {
        this.polygon = p;
        this.TriangleArray = new ArrayList<Triangle>();
        this.EarArray = new ArrayList<Integer>();
        this.polygon.ChangetoUnclockweise();
    }

    public ArrayList<Triangle> Triangulation() {
        InitiateTheEar();
//		this.polygon.Print();
//		EararayPrint();

        if (Polygonsize() == 3) {
            Triangle t = new Triangle(polygon.GetPoint(0), polygon.GetPoint(1), polygon.GetPoint(2));
            TriangleArray.add(t);
            return TriangleArray;
        }

        if (EarArray.size() == 0 || Polygonsize() < 3) {
//			System.out.println((EarArray.size()==0)+"user's data do something wrong"+(Polygonsize()<3));
            Triangle t = new Triangle(polygon.GetPoint(0), polygon.GetPoint(1), polygon.GetPoint(2));
            TriangleArray.add(t);
            return TriangleArray;

        }
        while (IsPolygonexsit() && (EarArray.size() != 0)) {
            if (IsEar(EarArray.get(0))) {
                int index = EarArray.get(0);
                Point2D point = GetpointinPolygon(index);
                Point2D Prepoint = GetpointinPolygon(index - 1);
                Point2D Nextpoint = GetpointinPolygon(index + 1);
                Triangle t = new Triangle(Prepoint, point, Nextpoint);
                TriangleArray.add(t);

                cutEarFromPolygon(index);

                RefreshEarArray(index);
                removeEarinEarArray();


            } else {
                removeEarinEarArray();
            }
        }
        this.TriangleArray.add(new Triangle(this.polygon.GetPoint(0), this.polygon.GetPoint(1),
                this.polygon.GetPoint(2)));
        return this.TriangleArray;
    }

    //	private void EararayPrint()
//	{
//		for(int i=0;i<EarArray.size();i++)
//		{
//			System.out.print(EarArray.get(i)+"  "+GetpointinPolygon(EarArray.get(i))+"    ");
//		}
//		System.out.println();
//		System.out.println("-----------------------------");
//	}
    private void removeEarinEarArray() {
        // TODO Auto-generated method stub
        EarArray.remove(0);

    }

    private void RefreshEarArray(int index) {
        // TODO Auto-generated method stub


        if (IsEar(PointIndexinPolygon(index - 1)) && !IsinEarArray(PointIndexinPolygon(index - 1)))
            EarArray.add(PointIndexinPolygon(index - 1));
        if (IsEar(PointIndexinPolygon(index)) && !IsinEarArray(PointIndexinPolygon(index)))
            EarArray.add(1, PointIndexinPolygon(index));

    }

    private boolean IsinEarArray(int index) {
        // TODO Auto-generated method stub
        for (int i = 1; i < EarArray.size(); i++) {
            if (index == EarArray.get(i))
                return true;
        }
        return false;
    }

    private void cutEarFromPolygon(int index) {
        // TODO Auto-generated method stub
        for (int i = 1; i < EarArray.size(); i++) {
            if (EarArray.get(i) > index)
                EarArray.set(i, EarArray.get(i) - 1);
        }
        this.polygon.removePoint(index);
    }

    private Point2D GetpointinPolygon(int i) {
        // TODO Auto-generated method stub
        if (i < -1 || i > Polygonsize()) {
            System.out.println(Polygonsize() + "this index over size" + i);

            return null;
        }
        if (i == -1)
            return this.polygon.GetPoint(Polygonsize() - 1);
        else if (i == Polygonsize())
            return this.polygon.GetPoint(0);
        else
            return this.polygon.GetPoint(i);
    }

    private int PointIndexinPolygon(int index) {
        // TODO Auto-generated method stub
        if (index == -1) {
            return Polygonsize() - 1;
        } else if (index == Polygonsize())
            return 0;
        else
            return index;
    }

    private boolean IsEar(int index) {
        // TODO Auto-generated method stub
        Point2D point = GetpointinPolygon(index);
        Point2D Prepoint = GetpointinPolygon(index - 1);
        Point2D Nextpoint = GetpointinPolygon(index + 1);

        Triangle t = new Triangle(Prepoint, point, Nextpoint);
        double dx0 = point.getX() - Prepoint.getX();
        double dy0 = point.getY() - Prepoint.getY();
        double dx1 = Nextpoint.getX() - point.getX();
        double dy1 = Nextpoint.getY() - point.getY();
        double cross = dx0 * dy1 - dx1 * dy0;
        if (cross <= 0) return false;
        for (int j = 0; j < Polygonsize(); ++j) {
            Point2D Testpoint = this.polygon.GetPoint(j);
            if (t.isEndPointinTriangle(Testpoint))
                continue;
            if (t.isInside(polygon.GetPoint(j)))
                return false;
        }
        return true;
    }

    private boolean IsPolygonexsit() {
        // TODO Auto-generated method stub
        if (Polygonsize() > 3)
            return true;
        else
            return false;
    }

    private void InitiateTheEar() {
        // TODO Auto-generated method stub
        for (int i = 0; i < Polygonsize(); i++) {
            if (IsEar(i))
                EarArray.add(i);
        }
    }

    private int Polygonsize() {
        // TODO Auto-generated method stub
        return this.polygon.GetPointsNumber();
    }
}
