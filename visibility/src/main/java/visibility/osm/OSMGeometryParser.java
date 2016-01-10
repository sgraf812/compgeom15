package visibility.osm;

import com.ximpleware.*;
import javafx.geometry.Point2D;
import visibility.types.GeometryParser;
import visibility.types.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Function;

public class OSMGeometryParser implements GeometryParser {
    private final AutoPilot BUILDING_MULTIPOLYGON_PATH = new AutoPilot();
    private final AutoPilot BUILDING_WAY_PATH = new AutoPilot();
    private final AutoPilot MEMBER_WAY_REF_PATH = new AutoPilot();
    private final AutoPilot NODE_REF_PATH = new AutoPilot();
    private final AutoPilot WAY_PATH = new AutoPilot();
    private final AutoPilot NODE_PATH = new AutoPilot();

    public OSMGeometryParser() {
        try {
            NODE_PATH.selectXPath("/osm/node");
            NODE_REF_PATH.selectXPath("nd/@ref");
            BUILDING_WAY_PATH.selectXPath("/osm/way[./tag[@k='building']]");
            BUILDING_MULTIPOLYGON_PATH.selectXPath("/osm/relation[@type='multipolygon' and ./tag[@k='building']]");
            MEMBER_WAY_REF_PATH.selectXPath("member[@type='way']/@ref");
            WAY_PATH.selectXPath("/osm/way");
        } catch (XPathParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Iterable<Polygon> parseFile(String osmfile) {
        try {
            VTDGen vg = new VTDGen();
            vg.parseFile(osmfile, false);
            VTDNav vn = vg.getNav();
            NODE_PATH.bind(vn); // This is important state for the later method calls!
            NODE_REF_PATH.bind(vn);
            BUILDING_WAY_PATH.bind(vn);
            BUILDING_MULTIPOLYGON_PATH.bind(vn);
            MEMBER_WAY_REF_PATH.bind(vn);
            WAY_PATH.bind(vn);

            // A hash from node ids to actual node positions
            Hashtable<Long, Point2D> nodes = new Hashtable<>();

            // The following call initializes accessed node refs in nodes to a dummy value.
            List<List<Long>> buildingWays = extractWaysOfBuildings(vn, nodes);
            // This will extract all referenced nodes, but no more.
            extractReferencedNodes(vn, nodes);
            // Finally build the polygon list by following the node refs in wayRefs.
            return buildPolygonList(nodes, buildingWays);
        } catch (XPathEvalException | NavException e) {
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

    private void extractReferencedNodes(VTDNav vn, Hashtable<Long, Point2D> nodes) throws XPathEvalException, NavException {
        vn.push();

        for (int i = NODE_PATH.evalXPath(); i != -1 ; i = NODE_PATH.evalXPath()) {
            long id = Long.parseLong(vn.toString(vn.getAttrVal("id")));
            // By checking that we already referenced the id we can reduce memory pressure
            if (nodes.containsKey(id)) {
                nodes.put(id, new Point2D(
                        Double.parseDouble(vn.toString(vn.getAttrVal("lon"))),
                        Double.parseDouble(vn.toString(vn.getAttrVal("lat")))
                ));
            }
        }

        vn.pop();
    }

    private List<List<Long>> extractWaysOfBuildings(VTDNav vn, Hashtable<Long, Point2D> nodes)
            throws XPathEvalException, NavException {
        vn.push();

        List<List<Long>> ways = new ArrayList<>();
        for (int i = BUILDING_WAY_PATH.evalXPath(); i != -1 ; i = BUILDING_WAY_PATH.evalXPath()) {
            // The lambda will put in a dummy value for each encountered node,
            // so that we know later which nodes we need to parse.
            ways.add(extractRefs(vn, NODE_REF_PATH, (Long ref) -> nodes.put(ref, Point2D.ZERO)));
        }

        vn.pop();

        BUILDING_WAY_PATH.resetXPath();

        return ways;
    }

    /**
     * Requires vn to point to a sub element. From there it iterates over all matches of the ap.
     * Calls onRef for visited nodes.
     */
    private List<Long> extractRefs(VTDNav vn, AutoPilot ap, Function<Long, Object> onRef) throws NavException, XPathEvalException {
        vn.push();

        List<Long> refs = new ArrayList<>();
        for (int j = ap.evalXPath(); j != -1; j = ap.evalXPath()) {
            long ref = Long.parseLong(vn.toString(j + 1));
            refs.add(ref);
            onRef.apply(ref);
        }
        ap.resetXPath();

        vn.pop();

        return refs;
    }

    private List<List<Long>> extractWayRefsOfMultipolygons(VTDNav vn, Hashtable<Long, List<Long>> ways) throws NavException, XPathEvalException {
        vn.push();

        List<List<Long>> multipolygons = new ArrayList<>();
        for (int i = BUILDING_MULTIPOLYGON_PATH.evalXPath(); i != -1 ; i = BUILDING_MULTIPOLYGON_PATH.evalXPath()) {
            // For an explanation for the lambda see extractWaysOfBuildings
            multipolygons.add(extractRefs(vn, MEMBER_WAY_REF_PATH, (Long ref) -> ways.put(ref, Collections.emptyList())));
        }

        vn.pop();

        BUILDING_WAY_PATH.resetXPath();

        return multipolygons;
    }
}
