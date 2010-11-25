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

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
import org.openstreetmap.josm.data.validation.util.Bag;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

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
            } else {
                throw new AssertionError();
            }
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
        super(tr("Duplicated nodes")+".",
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

        Bag<Map<String,String>, OsmPrimitive> bag = new Bag<Map<String,String>, OsmPrimitive>();
        for (Node n: nodes) {
            bag.add(n.getKeys(), n);
        }

        Map<String,Boolean> typeMap=new HashMap<String,Boolean>();
        String[] types = {"none", "highway", "railway", "waterway", "boundary", "power", "natural", "landuse", "building"};


        // check whether we have multiple nodes at the same position with
        // the same tag set
        //
        for (Iterator<Map<String,String>> it = bag.keySet().iterator(); it.hasNext();) {
            Map<String,String> tagSet = it.next();
            if (bag.get(tagSet).size() > 1) {

                for (String type: types) {
                    typeMap.put(type, false);
                }

                for (OsmPrimitive p : bag.get(tagSet)) {
                    if (p.getType()==OsmPrimitiveType.NODE) {
                        Node n = (Node) p;
                        List<OsmPrimitive> lp=n.getReferrers();
                        for (OsmPrimitive sp: lp) {
                            if (sp.getType()==OsmPrimitiveType.WAY) {
                                boolean typed = false;
                                Way w=(Way) sp;
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

                if (nbType>1) {
                    String msg = marktr("Mixed type duplicated nodes");
                    errors.add(new TestError(
                            parentTest,
                            Severity.WARNING,
                            tr("Duplicated nodes"),
                            tr(msg),
                            msg,
                            DUPLICATE_NODE_MIXED,
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
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
                            bag.get(tagSet)
                    ));

                }
                it.remove();
            }
        }

        // check whether we have multiple nodes at the same position with
        // differing tag sets
        //
        if (!bag.isEmpty()) {
            List<OsmPrimitive> duplicates = new ArrayList<OsmPrimitive>();
            for (List<OsmPrimitive> l: bag.values()) {
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

        if(checkAndConfirmOutlyingDeletes(nodes))
            return MergeNodesAction.mergeNodes(Main.main.getEditLayer(), nodes, target);

        return null;// undoRedo handling done in mergeNodes
    }

    @Override
    public boolean isFixable(TestError testError) {
        return (testError.getTester() instanceof DuplicateNode);
    }

    /**
     * Check whether user is about to delete data outside of the download area.
     * Request confirmation if he is.
     */
    private static boolean checkAndConfirmOutlyingDeletes(LinkedHashSet<Node> del) {
        Area a = Main.main.getCurrentDataSet().getDataSourceArea();
        if (a != null) {
            for (OsmPrimitive osm : del) {
                if (osm instanceof Node && !osm.isNew()) {
                    Node n = (Node) osm;
                    if (!a.contains(n.getCoor())) {
                        JPanel msg = new JPanel(new GridBagLayout());
                        msg.add(new JLabel(
                                "<html>" +
                                // leave message in one tr() as there is a grammatical
                                // connection.
                                tr("You are about to delete nodes outside of the area you have downloaded."
                                        + "<br>"
                                        + "This can cause problems because other objects (that you do not see) might use them."
                                        + "<br>" + "Do you really want to delete?") + "</html>"));

                        return ConditionalOptionPaneUtil.showConfirmationDialog(
                                "delete_outside_nodes",
                                Main.parent,
                                msg,
                                tr("Delete confirmation"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.YES_OPTION);
                    }
                }
            }
        }
        return true;
    }
}
