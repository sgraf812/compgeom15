package visibility.osm;

import com.ximpleware.*;
import javafx.geometry.Point2D;
import visibility.types.GeometryParser;
import visibility.types.Polygon;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class OSMGeometryParser implements GeometryParser {
    @Override
    public Iterable<Polygon> parseFile(String osmfile) {
        try {
            VTDGen vg = new VTDGen();
            vg.parseFile(osmfile, false);
            VTDNav vn = vg.getNav();

            // A hash from node ids to actual node positions
            Hashtable<Long, Point2D> nodes = new Hashtable<>();

            // The following call initializes accessed node refs in nodes to a dummy value.
            List<List<Long>> wayRefs = extractWaysOfBuildings(vn, nodes);
            // This will extract all referenced node refs, but no more.
            extractAccessedNodes(vn, nodes);
            // Finally build the polygon list by following the node refs in wayRefs.
            return buildPolygonList(nodes, wayRefs);
        } catch ( XPathParseException | XPathEvalException | NavException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private Iterable<Polygon> buildPolygonList(Hashtable<Long, Point2D> nodes, List<List<Long>> wayRefs) {
        ArrayList<Polygon> polygons = new ArrayList<>();
        for (List<Long> way : wayRefs) {
            List<Point2D> outerFace = new ArrayList<>(way.size());
            for (Long ref : way) {
                outerFace.add(nodes.get(ref));
            }
            polygons.add(new Polygon(outerFace, new ArrayList<List<Point2D>>()));
        }
        return polygons;
    }

    private void extractAccessedNodes(VTDNav vn, Hashtable<Long, Point2D> nodes) throws XPathParseException, XPathEvalException, NavException {
        AutoPilot nodePath = new AutoPilot(vn);
        nodePath.selectXPath("/osm/node");

        for (int i = nodePath.evalXPath(); i != -1 ; i = nodePath.evalXPath()) {
            long id = Long.parseLong(vn.toString(vn.getAttrVal("id")));
            // By checking that we already referenced the id we can reduce memory pressure
            if (nodes.containsKey(id)) {
                nodes.put(id, new Point2D(
                        Double.parseDouble(vn.toString(vn.getAttrVal("lon"))),
                        Double.parseDouble(vn.toString(vn.getAttrVal("lat")))
                ));
            }
        }
    }

    private List<List<Long>> extractWaysOfBuildings(VTDNav vn, Hashtable<Long, Point2D> nodes)
            throws XPathParseException, XPathEvalException, NavException {
        vn.push();

        final AutoPilot wayPath =  new AutoPilot(vn);
        wayPath.selectXPath("/osm/way[./tag[@k='building']]");
        final AutoPilot nodeRefPath = new AutoPilot(vn);
        nodeRefPath.selectXPath("nd/@ref");

        List<List<Long>> wayRefs = new ArrayList<>();
        for (int i = wayPath.evalXPath(); i != -1 ; i = wayPath.evalXPath()) {
            vn.push();

            List<Long> refs = new ArrayList<>();
            for (int j = nodeRefPath.evalXPath(); j != -1; j = nodeRefPath.evalXPath()) {
                long ref = Long.parseLong(vn.toString(j + 1));
                refs.add(ref);
                nodes.put(ref, Point2D.ZERO); // a dummy value for now
            }
            wayRefs.add(refs);
            nodeRefPath.resetXPath();

            vn.pop();
        }

        vn.pop();

        return wayRefs;
    }
}
