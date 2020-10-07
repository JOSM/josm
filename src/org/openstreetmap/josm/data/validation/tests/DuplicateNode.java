// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.RAILWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.WATERWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Hash;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Tests if there are duplicate nodes
 *
 * @author frsantos
 */
public class DuplicateNode extends Test {

    private static class NodeHash implements Hash<Object, Object> {

        private final double precision = Config.getPref().getDouble("validator.duplicatenodes.precision", 0.);

        private LatLon roundCoord(LatLon coor) {
            return new LatLon(
                    Math.round(coor.lat() / precision) * precision,
                    Math.round(coor.lon() / precision) * precision
                    );
        }

        @SuppressWarnings("unchecked")
        private LatLon getLatLon(Object o) {
            if (o instanceof Node) {
                LatLon coor = ((Node) o).getCoor();
                if (coor == null)
                    return null;
                if (precision == 0)
                    return coor.getRoundedToOsmPrecision();
                return roundCoord(coor);
            } else if (o instanceof List<?>) {
                LatLon coor = ((List<Node>) o).get(0).getCoor();
                if (coor == null)
                    return null;
                if (precision == 0)
                    return coor.getRoundedToOsmPrecision();
                return roundCoord(coor);
            } else
                throw new AssertionError();
        }

        @Override
        public boolean equals(Object k, Object t) {
            LatLon coorK = getLatLon(k);
            LatLon coorT = getLatLon(t);
            return coorK == coorT || (coorK != null && coorT != null && coorK.equals(coorT));
        }

        @Override
        public int getHashCode(Object k) {
            LatLon coorK = getLatLon(k);
            return coorK == null ? 0 : coorK.hashCode();
        }
    }

    protected static final int DUPLICATE_NODE = 1;
    protected static final int DUPLICATE_NODE_MIXED = 2;
    protected static final int DUPLICATE_NODE_OTHER = 3;
    protected static final int DUPLICATE_NODE_BUILDING = 10;
    protected static final int DUPLICATE_NODE_BOUNDARY = 11;
    protected static final int DUPLICATE_NODE_HIGHWAY = 12;
    protected static final int DUPLICATE_NODE_LANDUSE = 13;
    protected static final int DUPLICATE_NODE_NATURAL = 14;
    protected static final int DUPLICATE_NODE_POWER = 15;
    protected static final int DUPLICATE_NODE_RAILWAY = 16;
    protected static final int DUPLICATE_NODE_WATERWAY = 17;

    private static final String[] TYPES = {
            "none", HIGHWAY, RAILWAY, WATERWAY, "boundary", "power", "natural", "landuse", "building"};

    /** The map of potential duplicates.
     *
     * If there is exactly one node for a given pos, the map includes a pair &lt;pos, Node&gt;.
     * If there are multiple nodes for a given pos, the map includes a pair
     * &lt;pos, NodesByEqualTagsMap&gt;
     */
    private Storage<Object> potentialDuplicates;

    /**
     * Constructor
     */
    public DuplicateNode() {
        super(tr("Duplicated nodes"),
                tr("This test checks that there are no nodes at the very same location."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        potentialDuplicates = new Storage<>(new NodeHash());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void endTest() {
        for (Object v: potentialDuplicates) {
            if (v instanceof Node) {
                // just one node at this position. Nothing to report as error
                continue;
            }

            // multiple nodes at the same position -> check if all nodes have a distinct elevation
            List<Node> nodes = (List<Node>) v;
            Set<String> eles = nodes.stream()
                    .map(n -> n.get("ele"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (eles.size() == nodes.size()) {
                // All nodes at this position have a distinct elevation.
                // This is normal in some particular cases (for example, geodesic points in France)
                // Do not report this as an error
                continue;
            }

            // report errors
            errors.addAll(buildTestErrors(this, nodes));
        }
        super.endTest();
        potentialDuplicates = null;
    }

    /**
     * Returns the list of "duplicate nodes" errors for the given selection of node and parent test
     * @param parentTest The parent test of returned errors
     * @param nodes The nodes selction to look into
     * @return the list of "duplicate nodes" errors
     */
    public List<TestError> buildTestErrors(Test parentTest, List<Node> nodes) {
        List<TestError> errors = new ArrayList<>();

        MultiMap<Map<String, String>, OsmPrimitive> mm = new MultiMap<>();
        for (Node n: nodes) {
            mm.put(n.getKeys(), n);
        }

        Map<String, Boolean> typeMap = new HashMap<>();

        // check whether we have multiple nodes at the same position with the same tag set
        for (Iterator<Map<String, String>> it = mm.keySet().iterator(); it.hasNext();) {
            Set<OsmPrimitive> primitives = mm.get(it.next());
            if (primitives.size() > 1) {

                for (String type: TYPES) {
                    typeMap.put(type, Boolean.FALSE);
                }

                for (OsmPrimitive p : primitives) {
                    if (p.getType() == OsmPrimitiveType.NODE) {
                        Node n = (Node) p;
                        List<OsmPrimitive> lp = n.getReferrers();
                        for (OsmPrimitive sp: lp) {
                            if (sp.getType() == OsmPrimitiveType.WAY) {
                                boolean typed = false;
                                Way w = (Way) sp;
                                Map<String, String> keys = w.getKeys();
                                for (Iterator<Entry<String, Boolean>> itt = typeMap.entrySet().iterator(); itt.hasNext();) {
                                    Entry<String, Boolean> e = itt.next();
                                    if (keys.containsKey(e.getKey())) {
                                        e.setValue(Boolean.TRUE);
                                        typed = true;
                                    }
                                }
                                if (!typed) {
                                    typeMap.put("none", Boolean.TRUE);
                                }
                            }
                        }
                    }
                }

                long nbType = typeMap.entrySet().stream().filter(Entry::getValue).count();

                if (nbType > 1) {
                    errors.add(TestError.builder(parentTest, Severity.WARNING, DUPLICATE_NODE_MIXED)
                            .message(tr("Mixed type duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get(HIGHWAY)) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_HIGHWAY)
                            .message(tr("Highway duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get(RAILWAY)) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_RAILWAY)
                            .message(tr("Railway duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get(WATERWAY)) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_WATERWAY)
                            .message(tr("Waterway duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get("boundary")) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_BOUNDARY)
                            .message(tr("Boundary duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get("power")) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_POWER)
                            .message(tr("Power duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get("natural")) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_NATURAL)
                            .message(tr("Natural duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get("building")) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_BUILDING)
                            .message(tr("Building duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else if (typeMap.get("landuse")) {
                    errors.add(TestError.builder(parentTest, Severity.ERROR, DUPLICATE_NODE_LANDUSE)
                            .message(tr("Landuse duplicated nodes"))
                            .primitives(primitives)
                            .build());
                } else {
                    errors.add(TestError.builder(parentTest, Severity.WARNING, DUPLICATE_NODE_OTHER)
                            .message(tr("Other duplicated nodes"))
                            .primitives(primitives)
                            .build());
                }
                it.remove();
            }
        }

        // check whether we have multiple nodes at the same position with differing tag sets
        if (!mm.isEmpty()) {
            List<OsmPrimitive> duplicates = new ArrayList<>();
            for (Set<OsmPrimitive> l: mm.values()) {
                duplicates.addAll(l);
            }
            if (duplicates.size() > 1) {
                errors.add(TestError.builder(parentTest, Severity.WARNING, DUPLICATE_NODE)
                        .message(tr("Nodes at same position"))
                        .primitives(duplicates)
                        .build());
            }
        }
        return errors;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visit(Node n) {
        if (n.isUsable()) {
            if (potentialDuplicates.get(n) == null) {
                // in most cases there is just one node at a given position. We
                // avoid to create an extra object and add remember the node
                // itself at this position
                potentialDuplicates.put(n);
            } else if (potentialDuplicates.get(n) instanceof Node) {
                // we have an additional node at the same position. Create an extra
                // object to keep track of the nodes at this position.
                //
                Node n1 = (Node) potentialDuplicates.get(n);
                List<Node> nodes = new ArrayList<>(2);
                nodes.add(n1);
                nodes.add(n);
                potentialDuplicates.put(nodes);
            } else if (potentialDuplicates.get(n) instanceof List<?>) {
                // we have multiple nodes at the same position.
                //
                List<Node> nodes = (List<Node>) potentialDuplicates.get(n);
                nodes.add(n);
            }
        }
    }

    /**
     * Merge the nodes into one.
     * Copied from UtilsPlugin.MergePointsAction
     */
    @Override
    public Command fixError(TestError testError) {
        final Set<Node> nodes = testError.primitives(Node.class)
                // Filter nodes that have already been deleted (see #5764 and #5773)
                .filter(n -> !n.isDeleted())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Merge only if at least 2 nodes remain
        if (nodes.size() >= 2) {
            // Use first existing node or first node if all nodes are new
            Node target = nodes.stream()
                    .filter(n -> !n.isNew())
                    .findFirst()
                    .orElseGet(() -> nodes.iterator().next());

            if (Command.checkOutlyingOrIncompleteOperation(nodes, Collections.singleton(target)) == Command.IS_OK)
                return MergeNodesAction.mergeNodes(nodes, target);
        }

        return null; // undoRedo handling done in mergeNodes
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (!(testError.getTester() instanceof DuplicateNode)) return false;
        // never merge nodes with different tags.
        if (testError.getCode() == DUPLICATE_NODE) return false;
        // cannot merge nodes outside download area
        return testError.getPrimitives().stream().filter(p -> !p.isDeleted()).count() > 1
                && Command.checkOutlyingOrIncompleteOperation(testError.getPrimitives(), null) == Command.IS_OK;
        // everything else is ok to merge
    }
}
