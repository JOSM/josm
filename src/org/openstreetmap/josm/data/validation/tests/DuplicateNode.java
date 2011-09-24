// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
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
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Tests if there are duplicate nodes
 *
 * @author frsantos
 */
public class DuplicateNode extends Test {

    private class NodeHash implements Hash<Object, Object> {

        double precision = Main.pref.getDouble("validator.duplicatenodes.precision", 0.);

        private LatLon RoundCoord(Node o) {
            return new LatLon(
                    Math.round(o.getCoor().lat() / precision) * precision,
                    Math.round(o.getCoor().lon() / precision) * precision
            );
        }

        @SuppressWarnings("unchecked")
        private LatLon getLatLon(Object o) {
            if (o instanceof Node) {
                if (precision==0)
                    return ((Node) o).getCoor().getRoundedToOsmPrecision();
                return RoundCoord((Node) o);
            } else if (o instanceof List<?>) {
                if (precision==0)
                    return ((List<Node>) o).get(0).getCoor().getRoundedToOsmPrecision();
                return RoundCoord(((List<Node>) o).get(0));
            } else
                throw new AssertionError();
        }

        @Override
        public boolean equals(Object k, Object t) {
            return getLatLon(k).equals(getLatLon(t));
        }

        @Override
        public int getHashCode(Object k) {
            return getLatLon(k).hashCode();
        }
    }

    protected static int DUPLICATE_NODE = 1;
    protected static int DUPLICATE_NODE_MIXED = 2;
    protected static int DUPLICATE_NODE_OTHER = 3;
    protected static int DUPLICATE_NODE_UNCLOSED = 4;
    protected static int DUPLICATE_NODE_BUILDING = 10;
    protected static int DUPLICATE_NODE_BOUNDARY = 11;
    protected static int DUPLICATE_NODE_HIGHWAY = 12;
    protected static int DUPLICATE_NODE_LANDUSE = 13;
    protected static int DUPLICATE_NODE_NATURAL = 14;
    protected static int DUPLICATE_NODE_POWER = 15;
    protected static int DUPLICATE_NODE_RAILWAY = 16;
    protected static int DUPLICATE_NODE_WATERWAY = 17;

    /** The map of potential duplicates.
     *
     * If there is exactly one node for a given pos, the map includes a pair <pos, Node>.
     * If there are multiple nodes for a given pos, the map includes a pair
     * <pos, NodesByEqualTagsMap>
     */
    Storage<Object> potentialDuplicates;

    /**
     * Constructor
     */
    public DuplicateNode() {
        super(tr("Duplicated nodes."),
                tr("This test checks that there are no nodes at the very same location."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        potentialDuplicates = new Storage<Object>(new NodeHash());
    }


    @SuppressWarnings("unchecked")
    @Override
    public void endTest() {
        for (Object v: potentialDuplicates) {
            if (v instanceof Node) {
                // just one node at this position. Nothing to report as
                // error
                continue;
            }

            // multiple nodes at the same position -> report errors
            //
            List<Node> nodes = (List<Node>) v;
            errors.addAll(buildTestErrors(this, nodes));
        }
        super.endTest();
        potentialDuplicates = null;
    }

    public List<TestError> buildTestErrors(Test parentTest, List<Node> nodes) {
        List<TestError> errors = new ArrayList<TestError>();

        MultiMap<Map<String,String>, OsmPrimitive> mm = new MultiMap<Map<String,String>, OsmPrimitive>();
        for (Node n: nodes) {
            mm.put(n.getKeys(), n);
        }

        Map<String,Boolean> typeMap=new HashMap<String,Boolean>();
        String[] types = {"none", "highway", "railway", "waterway", "boundary", "power", "natural", "landuse", "building"};


        // check whether we have multiple nodes at the same position with
        // the same tag set
        //
        for (Iterator<Map<String,String>> it = mm.keySet().iterator(); it.hasNext();) {
            Map<String,String> tagSet = it.next();
            if (mm.get(tagSet).size() > 1) {

                boolean oneWayClosed = false;

                for (String type: types) {
                    typeMap.put(type, false);
                }

                for (OsmPrimitive p : mm.get(tagSet)) {
                    if (p.getType()==OsmPrimitiveType.NODE) {
                        Node n = (Node) p;
                        List<OsmPrimitive> lp=n.getReferrers();
                        for (OsmPrimitive sp: lp) {
                            if (sp.getType()==OsmPrimitiveType.WAY) {
                                boolean typed = false;
                                Way w=(Way) sp;
                                oneWayClosed = oneWayClosed || w.isClosed();
                                Map<String, String> keys = w.getKeys();
                                for (String type: typeMap.keySet()) {
                                    if (keys.containsKey(type)) {
                                        typeMap.put(type, true);
                                        typed=true;
                                    }
                                }
                                if (!typed) {
                                    typeMap.put("none", true);
                                }
                            }
                        }

                    }
                }

                int nbType=0;
                for (String type: typeMap.keySet()) {
                    if (typeMap.get(type)) {
                        nbType++;
                    }
                }

                if (!oneWayClosed) {
                    String msg = marktr("Duplicate nodes in two un-closed ways");
                    errors.add(new TestError(
                            parentTest,
                            Severity.WARNING,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_UNCLOSED,
                            mm.get(tagSet)
                    ));
                } else if (nbType>1) {
                    String msg = marktr("Mixed type duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.WARNING,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_MIXED,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("highway")) {
                    String msg = marktr("Highway duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_HIGHWAY,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("railway")) {
                    String msg = marktr("Railway duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_RAILWAY,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("waterway")) {
                    String msg = marktr("Waterway duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_WATERWAY,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("boundary")) {
                    String msg = marktr("Boundary duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_BOUNDARY,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("power")) {
                    String msg = marktr("Power duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_POWER,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("natural")) {
                    String msg = marktr("Natural duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_NATURAL,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("building")) {
                    String msg = marktr("Building duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_BUILDING,
                            mm.get(tagSet)
                    ));
                } else if (typeMap.get("landuse")) {
                    String msg = marktr("Landuse duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.ERROR,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_LANDUSE,
                            mm.get(tagSet)
                    ));
                } else {
                    String msg = marktr("Other duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.WARNING,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_OTHER,
                            mm.get(tagSet)
                    ));

                }
                it.remove();
            }
        }

        // check whether we have multiple nodes at the same position with
        // differing tag sets
        //
        if (!mm.isEmpty()) {
            List<OsmPrimitive> duplicates = new ArrayList<OsmPrimitive>();
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
                Node n1 = (Node)potentialDuplicates.get(n);
                List<Node> nodes = new ArrayList<Node>(2);
                nodes.add(n1);
                nodes.add(n);
                potentialDuplicates.put(nodes);
            } else if (potentialDuplicates.get(n) instanceof List<?>) {
                // we have multiple nodes at the same position.
                //
                List<Node> nodes = (List<Node>)potentialDuplicates.get(n);
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
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>(testError.getPrimitives());
        LinkedHashSet<Node> nodes = new LinkedHashSet<Node>(OsmPrimitive.getFilteredList(sel, Node.class));

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

        if (DeleteCommand.checkAndConfirmOutlyingDelete(Main.main.getCurrentDataSet().getDataSourceArea(), nodes, target))
            return MergeNodesAction.mergeNodes(Main.main.getEditLayer(), nodes, target);

        return null;// undoRedo handling done in mergeNodes
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (!(testError.getTester() instanceof DuplicateNode)) return false;
        // never merge nodes with different tags.
        if (testError.getCode() == DUPLICATE_NODE) return false;
        // never merge nodes from two different non-closed geometries
        if (testError.getCode() == DUPLICATE_NODE_UNCLOSED) return false;
        // everything else is ok to merge
        return true;
    }
}
