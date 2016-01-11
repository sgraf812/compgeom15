package visibility.osm;

import com.ximpleware.*;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.poly2tri.geometry.polygon.PolygonPoint;
import visibility.types.GeometryParser;
import org.poly2tri.geometry.polygon.Polygon;

import java.util.*;

public class OSMGeometryParser implements GeometryParser {
    private static final PolygonPoint DUMMY_POINT = new PolygonPoint(0, 0);
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
            Hashtable<Long, PolygonPoint> nodes = new Hashtable<>();
            // A hash from way refs referenced from multipolygons to their actual list of node refs
            Hashtable<Long, List<Long>> multipolygonWays = new Hashtable<>();

            // The following call initializes accessed node refs in nodes to a dummy value.
            List<List<Long>> buildingWays = extractWaysOfBuildings(vn, nodes);
            // The following call initializes accessed way refs from multipolygons to a dummy value.
            List<List<Tuple2<WayRole, Long>>> multipolygonWayRefs = extractWayRefsOfMultipolygons(vn, multipolygonWays);
            // This will extract all referenced multipolygon multipolygonWays, excluding the building multipolygonWays
            // Also adds referenced nodes to nodes
            extractReferencedWays(vn, multipolygonWays, nodes);
            // This will extract all referenced nodes, but no more.
            extractReferencedNodes(vn, nodes);
            // Finally build the polygon list by following the node refs in wayRefs.
            return buildPolygonList(nodes, buildingWays, multipolygonWays, multipolygonWayRefs);
        } catch (XPathEvalException | NavException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private Iterable<Polygon> buildPolygonList(Hashtable<Long, PolygonPoint> nodes, List<List<Long>> buildingWays, Hashtable<Long, List<Long>> multipolygonWays, List<List<Tuple2<WayRole, Long>>> multipolygonWayRefs) {
        ArrayList<Polygon> polygons = new ArrayList<>();

        // first add the building ways
        buildBuildingPolygonsWithoutHoles(nodes, buildingWays, polygons);

        // now add the multipolygon ways, that's more involved
        buildMultipolygons(nodes, multipolygonWays, multipolygonWayRefs, polygons);

        return polygons;
    }

    private void buildBuildingPolygonsWithoutHoles(Hashtable<Long, PolygonPoint> nodes, List<List<Long>> buildingWays, ArrayList<Polygon> polygons) {
        for (List<Long> way : buildingWays) {
            List<PolygonPoint> outerFace = new ArrayList<>(way.size());
            for (Long ref : way) {
                outerFace.add(nodes.get(ref));
            }
            polygons.add(new Polygon(outerFace));
        }
    }

    private void extractReferencedNodes(VTDNav vn, Hashtable<Long, PolygonPoint> nodes) throws XPathEvalException, NavException {
        vn.push();

        for (int i = NODE_PATH.evalXPath(); i != -1 ; i = NODE_PATH.evalXPath()) {
            long id = Long.parseLong(vn.toString(vn.getAttrVal("id")));
            // By checking that we already referenced the id we can reduce memory pressure
            if (nodes.containsKey(id)) {
                nodes.put(id, new PolygonPoint(
                        Double.parseDouble(vn.toString(vn.getAttrVal("lon"))),
                        Double.parseDouble(vn.toString(vn.getAttrVal("lat")))
                ));
            }
        }

        vn.pop();
    }

    private void extractReferencedWays(VTDNav vn, Hashtable<Long, List<Long>> ways, Hashtable<Long, PolygonPoint> nodes) throws XPathEvalException, NavException {
        vn.push();

        for (int i = WAY_PATH.evalXPath(); i != -1 ; i = WAY_PATH.evalXPath()) {
            long id = Long.parseLong(vn.toString(vn.getAttrVal("id")));
            // By checking that we already referenced the id we can reduce memory pressure
            if (ways.containsKey(id)) {
                ways.put(id, extractNodeRefs(vn, nodes));
            }
        }

        vn.pop();
    }

    private List<List<Long>> extractWaysOfBuildings(VTDNav vn, Hashtable<Long, PolygonPoint> nodes)
            throws XPathEvalException, NavException {
        vn.push();

        List<List<Long>> ways = new ArrayList<>();
        for (int i = BUILDING_WAY_PATH.evalXPath(); i != -1 ; i = BUILDING_WAY_PATH.evalXPath()) {
            // The lambda will put in a dummy value for each encountered node,
            // so that we know later which nodes we need to parse.
            ways.add(extractNodeRefs(vn, nodes));
        }

        vn.pop();

        BUILDING_WAY_PATH.resetXPath();
        return ways;
    }

    /**
     * Requires vn to point to a way element. From there it iterates over all matches of the ap.
     * Puts a dummy point into nodes for every node it finds.
     */
    private List<Long> extractNodeRefs(VTDNav vn, Hashtable<Long, PolygonPoint> nodes) throws NavException, XPathEvalException {
        vn.push();

        List<Long> refs = new ArrayList<>();
        for (int j = NODE_REF_PATH.evalXPath(); j != -1; j = NODE_REF_PATH.evalXPath()) {
            long ref = Long.parseLong(vn.toString(j + 1));
            refs.add(ref);
            nodes.put(ref, DUMMY_POINT);
        }
        NODE_REF_PATH.resetXPath();

        vn.pop();

        return refs;
    }

    private List<List<Tuple2<WayRole, Long>>> extractWayRefsOfMultipolygons(VTDNav vn, Hashtable<Long, List<Long>> ways) throws NavException, XPathEvalException {
        vn.push();

        List<List<Tuple2<WayRole, Long>>> multipolygons = new ArrayList<>();
        for (int i = BUILDING_MULTIPOLYGON_PATH.evalXPath(); i != -1 ; i = BUILDING_MULTIPOLYGON_PATH.evalXPath()) {
            // For an explanation for the lambda see extractWaysOfBuildings
            multipolygons.add(extractWayRefs(vn, ways));
        }

        vn.pop();

        BUILDING_WAY_PATH.resetXPath();

        return multipolygons;
    }

    /**
     * Requires vn to point to a relation element. From there it iterates over all matches of the ap.
     * Puts an empty list for each referenced way.
     */
    private List<Tuple2<WayRole, Long>> extractWayRefs(VTDNav vn, Hashtable<Long, List<Long>> ways) throws NavException, XPathEvalException {
        vn.push();

        List<Tuple2<WayRole, Long>> refs = new ArrayList<>();
        for (int j = MEMBER_WAY_PATH.evalXPath(); j != -1; j = MEMBER_WAY_PATH.evalXPath()) {
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
        MEMBER_WAY_PATH.resetXPath();

        vn.pop();

        return refs;
    }

    private void buildMultipolygons(Hashtable<Long, PolygonPoint> nodes, Hashtable<Long, List<Long>> multipolygonWays, List<List<Tuple2<WayRole, Long>>> multipolygonWayRefs, List<Polygon> polygons) {
        for (List<Tuple2<WayRole, Long>> waysOfPoly: multipolygonWayRefs) {
            List<PolygonPoint> outer = new ArrayList<>();
        }
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
