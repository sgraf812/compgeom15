package visibility.osm;

import com.ximpleware.*;
import javafx.geometry.Point2D;
import org.apache.commons.io.IOUtils;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import visibility.types.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

public class OSMGeometryParser implements GeometryParser {
    private static final Logger LOG = LoggerFactory.getLogger(OSMGeometryParser.class);
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

    public List<Triangle> parseFile(InputStream is) {
        try {
            VTDGen vg = new VTDGen();
            vg.setDoc(IOUtils.toByteArray(is));
            vg.parse(false);
            VTDNav vn = vg.getNav();
            NODE_PATH.resetXPath();
            NODE_PATH.bind(vn); // This is important state for the later method calls!
            NODE_REF_PATH.resetXPath();
            NODE_REF_PATH.bind(vn);
            BUILDING_WAY_PATH.resetXPath();
            BUILDING_WAY_PATH.bind(vn);
            BUILDING_MULTIPOLYGON_PATH.resetXPath();
            BUILDING_MULTIPOLYGON_PATH.bind(vn);
            MEMBER_WAY_PATH.resetXPath();
            MEMBER_WAY_PATH.bind(vn);
            WAY_PATH.resetXPath();
            WAY_PATH.bind(vn);

            // A hash from node ids to actual node positions
            Hashtable<Long, Point2D> nodes = new Hashtable<>();
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
            // Triangulate each polygon and return the flattened list of triangles.
            // This way, poly2tri's types will not leak out of this class and we operate
            // on triangles anyway.
            return buildPolygons(nodes, buildingWays, multipolygonWays, multipolygonWayRefs).flatMap(p -> {
                        try {
                            p.ComplexToSimplePolygon();
                            EarClipping ec = new EarClipping(p.SimplePolygon);
                            return ec.Triangulation().stream();
                        } catch (RuntimeException ignored) {
                        }
                        return Stream.empty();
                    }
            ).toList();
        } catch (XPathEvalException | NavException | IOException | ParseException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private Seq<PolygonwithHoles> buildPolygons(Hashtable<Long, Point2D> nodes, List<List<Long>> buildingWays, Hashtable<Long, List<Long>> multipolygonWays, List<List<Tuple2<WayRole, Long>>> multipolygonWayRefs) {
        // first add the building ways
        Seq<PolygonwithHoles> buildings = buildBuildingPolygonsWithoutHoles(nodes, buildingWays);
        // now add the multipolygon ways, that's more involved
        Seq<PolygonwithHoles> multis = buildMultipolygons(nodes, multipolygonWays, multipolygonWayRefs);

        return buildings.concat(multis);
    }

    private Seq<PolygonwithHoles> buildBuildingPolygonsWithoutHoles(Hashtable<Long, Point2D> nodes, List<List<Long>> buildingWays) {
        return Seq.seq(buildingWays).map(way -> new PolygonwithHoles(buildPolygonFromWayRefs(way, nodes)));
    }

    private static Polygon buildPolygonFromWayRefs(List<Long> way, Hashtable<Long, Point2D> nodes) {
        return new Polygon(Seq.seq(way).map(nodes::get).toList());
    }

    private void extractReferencedNodes(VTDNav vn, Hashtable<Long, Point2D> nodes) throws XPathEvalException, NavException {
        vn.push();

        for (int i = NODE_PATH.evalXPath(); i != -1; i = NODE_PATH.evalXPath()) {
            long id = Long.parseLong(vn.toString(vn.getAttrVal("id")));
            // By checking that we already referenced the id we can reduce memory pressure
            if (nodes.containsKey(id)) {
                nodes.put(id, new Point2D(
                        Double.parseDouble(vn.toString(vn.getAttrVal("lon"))) * (1 << 10),
                        Double.parseDouble(vn.toString(vn.getAttrVal("lat"))) * (1 << 10)
                ));
            }
        }

        // Make sure we have all referenced nodes extracted
        //assert !nodes.containsValue(DUMMY_POINT);

        vn.pop();
    }

    private void extractReferencedWays(VTDNav vn, Hashtable<Long, List<Long>> ways, Hashtable<Long, Point2D> nodes) throws XPathEvalException, NavException {
        vn.push();

        for (int i = WAY_PATH.evalXPath(); i != -1; i = WAY_PATH.evalXPath()) {
            long id = Long.parseLong(vn.toString(vn.getAttrVal("id")));
            // By checking that we already referenced the id we can reduce memory pressure
            if (ways.containsKey(id)) {
                ways.put(id, extractNodeRefs(vn, nodes));
            }
        }

        vn.pop();
    }

    private List<List<Long>> extractWaysOfBuildings(VTDNav vn, Hashtable<Long, Point2D> nodes)
            throws XPathEvalException, NavException {
        vn.push();

        List<List<Long>> ways = new ArrayList<>();
        for (int i = BUILDING_WAY_PATH.evalXPath(); i != -1; i = BUILDING_WAY_PATH.evalXPath()) {
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
    private List<Long> extractNodeRefs(VTDNav vn, Hashtable<Long, Point2D> nodes) throws NavException, XPathEvalException {
        vn.push();

        List<Long> refs = new ArrayList<>();
        for (int j = NODE_REF_PATH.evalXPath(); j != -1; j = NODE_REF_PATH.evalXPath()) {
            long ref = Long.parseLong(vn.toString(j + 1));
            refs.add(ref);
            nodes.put(ref, new Point2D(0, 0));
        }
        NODE_REF_PATH.resetXPath();

        vn.pop();

        return refs;
    }

    private List<List<Tuple2<WayRole, Long>>> extractWayRefsOfMultipolygons(VTDNav vn, Hashtable<Long, List<Long>> ways) throws NavException, XPathEvalException {
        vn.push();

        List<List<Tuple2<WayRole, Long>>> multipolygons = new ArrayList<>();
        for (int i = BUILDING_MULTIPOLYGON_PATH.evalXPath(); i != -1; i = BUILDING_MULTIPOLYGON_PATH.evalXPath()) {
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
            String roleAsString = vn.toString(vn.getAttrVal("role")).toLowerCase();
            WayRole role;
            switch (roleAsString) {
                case "inner":
                    role = WayRole.INNER;
                    break;
                case "outer":
                    role = WayRole.OUTER;
                    break;
                default:
                    continue;
            }
            refs.add(Tuple.tuple(role, ref));
            ways.put(ref, Collections.emptyList());
        }
        MEMBER_WAY_PATH.resetXPath();

        vn.pop();

        return refs;
    }

    private Seq<PolygonwithHoles> buildMultipolygons(Hashtable<Long, Point2D> nodes, Hashtable<Long, List<Long>> multipolygonWays, List<List<Tuple2<WayRole, Long>>> multipolygonWayRefs) {
        return Seq.seq(multipolygonWayRefs).map(waysOfPoly -> {
            try {
                // I know this not how should assemble them, but parsing OSM wasn't our objective.
                List<Polygon> holes = new ArrayList<>();
                Polygon outer = null;
                for (Tuple2<WayRole, Long> way : waysOfPoly) {
                    WayRole role = way.v1;
                    long wayRef = way.v2;
                    List<Long> nodeRefs = multipolygonWays.get(wayRef);
                    Polygon p = buildPolygonFromWayRefs(nodeRefs, nodes);
                    if (role == WayRole.OUTER && outer == null && p.GetPointsNumber() > 0) {
                        outer = p;
                    } else if (role == WayRole.INNER && p.GetPointsNumber() > 0) {
                        holes.add(p);
                    } else {
                        LOG.warn("Could not process a way");
                    }
                }
                return new PolygonwithHoles(outer, holes);
            } catch (Exception ex) {
                //System.out.println("Failed to build a certain multipolygon");
            }
            return null;
        }).filter(p -> p != null);
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
