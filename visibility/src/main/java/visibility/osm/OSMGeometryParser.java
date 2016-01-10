package visibility.osm;

import com.ximpleware.*;
import javafx.geometry.Point2D;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import visibility.types.GeometryParser;
import visibility.types.Polygon;

import java.util.*;
import java.util.function.Function;

public class OSMGeometryParser implements GeometryParser {
    private final AutoPilot BUILDING_MULTIPOLYGON_PATH = new AutoPilot();
    private final AutoPilot BUILDING_WAY_PATH = new AutoPilot();
    private final AutoPilot MEMBER_WAY_PATH = new AutoPilot();
    private final AutoPilot NODE_REF_PATH = new AutoPilot();
    private final AutoPilot WAY_PATH = new AutoPilot();
    private final AutoPilot NODE_PATH = new AutoPilot();

    public OSMGeometryParser() {
        try {
            NODE_PATH.selectXPath("/osm/node");
            NODE_REF_PATH.selectXPath("nd/@ref");
            BUILDING_WAY_PATH.selectXPath("/osm/way[./tag[@k='building']]");
            BUILDING_MULTIPOLYGON_PATH.selectXPath("/osm/relation[./tag[@k='type' and @v='multipolygon'] and ./tag/@k='building']");
            MEMBER_WAY_PATH.selectXPath("member[@type='way']");
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
            MEMBER_WAY_PATH.bind(vn);
            WAY_PATH.bind(vn);

            // A hash from node ids to actual node positions
            Hashtable<Long, Point2D> nodes = new Hashtable<>();
            // A hash from way refs referenced from multipolygons to their actual list of node refs
            Hashtable<Long, List<Long>> ways = new Hashtable<>();

            // The following call initializes accessed node refs in nodes to a dummy value.
            List<List<Long>> buildingWays = extractWaysOfBuildings(vn, nodes);
            // The following call initializes accessed way refs from multipolygons to a dummy value.
            List<List<Tuple2<WayRole, Long>>> multipolygons = extractWayRefsOfMultipolygons(vn, ways);
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
            polygons.add(new Polygon(outerFace, new ArrayList<>()));
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
            ways.add(extractNodeRefs(vn, NODE_REF_PATH, nodes));
        }

        vn.pop();

        BUILDING_WAY_PATH.resetXPath();

        return ways;
    }

    /**
     * Requires vn to point to a way element. From there it iterates over all matches of the ap.
     * Puts a 0 point into nodes for every node it finds.
     */
    private List<Long> extractNodeRefs(VTDNav vn, AutoPilot ap, Hashtable<Long, Point2D> nodes) throws NavException, XPathEvalException {
        vn.push();

        List<Long> refs = new ArrayList<>();
        for (int j = ap.evalXPath(); j != -1; j = ap.evalXPath()) {
            long ref = Long.parseLong(vn.toString(j + 1));
            refs.add(ref);
            nodes.put(ref, Point2D.ZERO);
        }
        ap.resetXPath();

        vn.pop();

        return refs;
    }

    private List<List<Tuple2<WayRole, Long>>> extractWayRefsOfMultipolygons(VTDNav vn, Hashtable<Long, List<Long>> ways) throws NavException, XPathEvalException {
        vn.push();

        List<List<Tuple2<WayRole, Long>>> multipolygons = new ArrayList<>();
        for (int i = BUILDING_MULTIPOLYGON_PATH.evalXPath(); i != -1 ; i = BUILDING_MULTIPOLYGON_PATH.evalXPath()) {
            // For an explanation for the lambda see extractWaysOfBuildings
            multipolygons.add(extractWayRefs(vn, MEMBER_WAY_PATH, ways));
        }

        vn.pop();

        BUILDING_WAY_PATH.resetXPath();

        return multipolygons;
    }

    /**
     * Requires vn to point to a relation element. From there it iterates over all matches of the ap.
     * Puts an empty list for each referenced way.
     */
    private List<Tuple2<WayRole, Long>> extractWayRefs(VTDNav vn, AutoPilot ap, Hashtable<Long, List<Long>> ways) throws NavException, XPathEvalException {
        vn.push();

        List<Tuple2<WayRole, Long>> refs = new ArrayList<>();
        for (int j = ap.evalXPath(); j != -1; j = ap.evalXPath()) {
            long ref = Long.parseLong(vn.toString(vn.getAttrVal("ref")));
            String roleAsString = vn.toString(vn.getAttrVal("role"));
            WayRole role;
            switch (roleAsString) {
                case "inner":
                    role = WayRole.INNER;
                    break;
                case "outer":
                    role = WayRole.OUTER;
                    break;
                default:
                    throw new RuntimeException("Parsing the role blew up");
            }
            refs.add(Tuple.tuple(role, ref));
            ways.put(ref, Collections.emptyList());
        }
        ap.resetXPath();

        vn.pop();

        return refs;
    }

    /**
     * Covers the 'role' attribute of a way within a multipolygon relation.
     * OUTER would mean that the inner of the circumfered area belongs to the polygon,
     * while INNER cuts holes into existing polygon areas.
     */
    private enum WayRole {
        INNER,
        OUTER
    }
}
