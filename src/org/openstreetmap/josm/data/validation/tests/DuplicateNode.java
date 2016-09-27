// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.Main;
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
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Tests if there are duplicate nodes
 *
 * @author frsantos
 */
public class DuplicateNode extends Test {

    private static class NodeHash implements Hash<Object, Object> {

        private final double precision = Main.pref.getDouble("validator.duplicatenodes.precision", 0.);

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

    private static class DuplicateNodeTestError extends TestError {
        DuplicateNodeTestError(Test parentTest, Severity severity, String msg, int code, Set<OsmPrimitive> primitives) {
            super(parentTest, severity, tr("Duplicated nodes"), tr(msg), msg, code, primitives);
            CheckParameterUtil.ensureThat(!primitives.isEmpty(), "Empty primitives: " + msg);
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
            "none", "highway", "railway", "waterway", "boundary", "power", "natural", "landuse", "building"};

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
            Set<String> eles = new HashSet<>();
            for (Node n : nodes) {
                String ele = n.get("ele");
                if (ele != null) {
                    eles.add(ele);
                }
            }
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
                                for (String type: typeMap.keySet()) {
                                    if (keys.containsKey(type)) {
                                        typeMap.put(type, Boolean.TRUE);
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
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.WARNING,
                            marktr("Mixed type duplicated nodes"),
                            DUPLICATE_NODE_MIXED,
                            primitives
                            ));
                } else if (typeMap.get("highway")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Highway duplicated nodes"),
                            DUPLICATE_NODE_HIGHWAY,
                            primitives
                            ));
                } else if (typeMap.get("railway")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Railway duplicated nodes"),
                            DUPLICATE_NODE_RAILWAY,
                            primitives
                            ));
                } else if (typeMap.get("waterway")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Waterway duplicated nodes"),
                            DUPLICATE_NODE_WATERWAY,
                            primitives
                            ));
                } else if (typeMap.get("boundary")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Boundary duplicated nodes"),
                            DUPLICATE_NODE_BOUNDARY,
                            primitives
                            ));
                } else if (typeMap.get("power")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Power duplicated nodes"),
                            DUPLICATE_NODE_POWER,
                            primitives
                            ));
                } else if (typeMap.get("natural")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Natural duplicated nodes"),
                            DUPLICATE_NODE_NATURAL,
                            primitives
                            ));
                } else if (typeMap.get("building")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Building duplicated nodes"),
                            DUPLICATE_NODE_BUILDING,
                            primitives
                            ));
                } else if (typeMap.get("landuse")) {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.ERROR,
                            marktr("Landuse duplicated nodes"),
                            DUPLICATE_NODE_LANDUSE,
                            primitives
                            ));
                } else {
                    errors.add(new DuplicateNodeTestError(
                            parentTest,
                            Severity.WARNING,
                            marktr("Other duplicated nodes"),
                            DUPLICATE_NODE_OTHER,
                            primitives
                            ));
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
                errors.add(new TestError(
                        parentTest,
                        Severity.WARNING,
                        tr("Nodes at same position"),
                        DUPLICATE_NODE,
                        duplicates
                        ));
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
        if (!isFixable(testError)) return null;
        // Diamond operator does not work with Java 9 here
        @SuppressWarnings("unused")
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>(testError.getPrimitives());
        Set<Node> nodes = new LinkedHashSet<>(OsmPrimitive.getFilteredList(sel, Node.class));

        // Filter nodes that have already been deleted (see #5764 and #5773)
        for (Iterator<Node> it = nodes.iterator(); it.hasNext();) {
            if (it.next().isDeleted()) {
                it.remove();
            }
        }

        // Merge only if at least 2 nodes remain
        if (nodes.size() >= 2) {
            // Use first existing node or first node if all nodes are new
            Node target = null;
            for (Node n: nodes) {
                if (!n.isNew()) {
                    target = n;
                    break;
                }
            }
            if (target == null) {
                target = nodes.iterator().next();
            }

            if (Command.checkOutlyingOrIncompleteOperation(nodes, Collections.singleton(target)) == Command.IS_OK)
                return MergeNodesAction.mergeNodes(Main.getLayerManager().getEditLayer(), nodes, target);
        }

        return null; // undoRedo handling done in mergeNodes
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (!(testError.getTester() instanceof DuplicateNode)) return false;
        // never merge nodes with different tags.
        if (testError.getCode() == DUPLICATE_NODE) return false;
        // cannot merge nodes outside download area
        final Iterator<? extends OsmPrimitive> it = testError.getPrimitives().iterator();
        if (!it.hasNext() || it.next().isOutsideDownloadArea()) return false;
        // everything else is ok to merge
        return true;
    }
}
