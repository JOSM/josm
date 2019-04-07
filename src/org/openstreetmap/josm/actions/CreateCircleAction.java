// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.PolarCoor;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * - Create a new circle from two selected nodes or a way with 2 nodes which represent the diameter of the circle.
 * - Create a new circle from three selected nodes--or a way with 3 nodes.
 * - Useful for roundabouts
 *
 * Notes:
 *   * If a way is selected, it is changed. If nodes are selected a new way is created.
 *     So if you've got a way with nodes it makes a difference between running this on the way or the nodes!
 *   * The existing nodes are retained, and additional nodes are inserted regularly
 *     to achieve the desired number of nodes (`createcircle.nodecount`).
 * BTW: Someone might want to implement projection corrections for this...
 *
 * @author Henry Loenwind
 * @author Sebastian Masch
 * @author Alain Delplanque
 *
 * @since 996
 */
public final class CreateCircleAction extends JosmAction {

    /**
     * Constructs a new {@code CreateCircleAction}.
     */
    public CreateCircleAction() {
        super(tr("Create Circle"), "aligncircle", tr("Create a circle from three selected nodes."),
            Shortcut.registerShortcut("tools:createcircle", tr("Tool: {0}", tr("Create Circle")),
            KeyEvent.VK_O, Shortcut.SHIFT), true, "createcircle", true);
        setHelpId(ht("/Action/CreateCircle"));
    }

    /**
     * Distributes nodes according to the algorithm of election with largest remainder.
     * @param angles Array of PolarNode ordered by increasing angles
     * @param nodesCount Number of nodes to be distributed
     * @return Array of number of nodes to put in each arc
     */
    private static int[] distributeNodes(PolarNode[] angles, int nodesCount) {
        int[] count = new int[angles.length];
        double[] width = new double[angles.length];
        double[] remainder = new double[angles.length];
        for (int i = 0; i < angles.length; i++) {
            width[i] = angles[(i+1) % angles.length].a - angles[i].a;
            if (width[i] < 0)
                width[i] += 2*Math.PI;
        }
        int assign = 0;
        for (int i = 0; i < angles.length; i++) {
            double part = width[i] / 2.0 / Math.PI * nodesCount;
            count[i] = (int) Math.floor(part);
            remainder[i] = part - count[i];
            assign += count[i];
        }
        while (assign < nodesCount) {
            int imax = 0;
            for (int i = 1; i < angles.length; i++) {
                if (remainder[i] > remainder[imax])
                    imax = i;
            }
            count[imax]++;
            remainder[imax] = 0;
            assign++;
        }
        return count;
    }

    /**
     * Class designed to create a couple between a node and its angle relative to the center of the circle.
     */
    private static class PolarNode implements Comparable<PolarNode> {
        private final double a;
        private final Node node;

        PolarNode(EastNorth center, Node n) {
            this.a = PolarCoor.computeAngle(n.getEastNorth(), center);
            this.node = n;
        }

        @Override
        public int compareTo(PolarNode o) {
            return Double.compare(a, o.a);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        runOn(getLayerManager().getEditDataSet());
    }

    /**
     * Run the action on the given dataset.
     * @param ds dataset
     * @since 14542
     */
    public static void runOn(DataSet ds) {
        List<Node> nodes = new ArrayList<>(ds.getSelectedNodes());
        Collection<Way> ways = ds.getSelectedWays();

        Way existingWay = null;

        // special case if no single nodes are selected and exactly one way is:
        // then use the way's nodes
        if (nodes.isEmpty() && (ways.size() == 1)) {
            existingWay = ways.iterator().next();
            for (Node n : existingWay.getNodes()) {
                if (!nodes.contains(n)) {
                    nodes.add(n);
                }
            }
        }

        if (nodes.size() < 2 || nodes.size() > 3) {
            new Notification(
                    tr("Please select exactly two or three nodes or one way with exactly two or three nodes."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_LONG)
                    .show();
            return;
        }

        EastNorth center;

        if (nodes.size() == 2) {
            // diameter: two single nodes needed or a way with two nodes
            EastNorth n1 = nodes.get(0).getEastNorth();
            EastNorth n2 = nodes.get(1).getEastNorth();

            center = n1.getCenter(n2);
        } else {
            // triangle: three single nodes needed or a way with three nodes
            center = Geometry.getCenter(nodes);
            if (center == null) {
                notifyNodesNotOnCircle();
                return;
            }
        }

        // calculate the radius (r)
        EastNorth n1 = nodes.get(0).getEastNorth();
        double r = n1.distance(center);

        // see #10777
        LatLon ll1 = ProjectionRegistry.getProjection().eastNorth2latlon(n1);
        LatLon ll2 = ProjectionRegistry.getProjection().eastNorth2latlon(center);

        double radiusInMeters = ll1.greatCircleDistance(ll2);

        int numberOfNodesInCircle = (int) Math.ceil(6.0 * Math.pow(radiusInMeters, 0.5));
        // an odd number of nodes makes the distribution uneven
        if ((numberOfNodesInCircle % 2) != 0) {
            // add 1 to make it even
            numberOfNodesInCircle += 1;
        }
        if (numberOfNodesInCircle < 6) {
            numberOfNodesInCircle = 6;
        }

        // Order nodes by angle
        final PolarNode[] angles = nodes.stream()
                .map(n -> new PolarNode(center, n))
                .sorted()
                .toArray(PolarNode[]::new);
        int[] count = distributeNodes(angles,
                numberOfNodesInCircle >= nodes.size() ? (numberOfNodesInCircle - nodes.size()) : 0);

        // now we can start doing things to OSM data
        Collection<Command> cmds = new LinkedList<>();

        // build a way for the circle
        List<Node> nodesToAdd = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            nodesToAdd.add(angles[i].node);
            double delta = angles[(i+1) % nodes.size()].a - angles[i].a;
            if (delta < 0)
                delta += 2*Math.PI;
            for (int j = 0; j < count[i]; j++) {
                double alpha = angles[i].a + (j+1)*delta/(count[i]+1);
                double x = center.east() + r*Math.cos(alpha);
                double y = center.north() + r*Math.sin(alpha);
                LatLon ll = ProjectionRegistry.getProjection().eastNorth2latlon(new EastNorth(x, y));
                if (new Node(new EastNorth(x, y)).isOutSideWorld()) {
                    notifyNodesOutsideWorld();
                    return;
                }
                Node n = new Node(ll);
                nodesToAdd.add(n);
                cmds.add(new AddCommand(ds, n));
            }
        }
        nodesToAdd.add(nodesToAdd.get(0)); // close the circle
        if (existingWay != null && existingWay.getNodesCount() >= 3) {
            nodesToAdd = orderNodesByWay(nodesToAdd, existingWay);
        } else {
            nodesToAdd = orderNodesByTrafficHand(nodesToAdd);
        }
        if (existingWay == null) {
            Way newWay = new Way();
            newWay.setNodes(nodesToAdd);
            cmds.add(new AddCommand(ds, newWay));
        } else {
            Way newWay = new Way(existingWay);
            newWay.setNodes(nodesToAdd);
            cmds.add(new ChangeCommand(ds, existingWay, newWay));
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Create Circle"), cmds));
    }

    /**
     * Order nodes according to left/right hand traffic.
     * @param nodes Nodes list to be ordered.
     * @return Modified nodes list ordered according hand traffic.
     */
    private static List<Node> orderNodesByTrafficHand(List<Node> nodes) {
        boolean rightHandTraffic = true;
        for (Node n: nodes) {
            if (!RightAndLefthandTraffic.isRightHandTraffic(n.getCoor())) {
                rightHandTraffic = false;
                break;
            }
        }
        if (rightHandTraffic == Geometry.isClockwise(nodes)) {
            Collections.reverse(nodes);
        }
        return nodes;
    }

    /**
     * Order nodes according to way direction.
     * @param nodes Nodes list to be ordered.
     * @param way Way used to determine direction.
     * @return Modified nodes list with same direction as way.
     */
    private static List<Node> orderNodesByWay(List<Node> nodes, Way way) {
        List<Node> wayNodes = way.getNodes();
        if (!way.isClosed()) {
            wayNodes.add(wayNodes.get(0));
        }
        if (Geometry.isClockwise(wayNodes) != Geometry.isClockwise(nodes)) {
            Collections.reverse(nodes);
        }
        return nodes;
    }

    private static void notifyNodesNotOnCircle() {
        new Notification(
                tr("Those nodes are not in a circle. Aborting."))
                .setIcon(JOptionPane.WARNING_MESSAGE)
                .show();
    }

    private static void notifyNodesOutsideWorld() {
        new Notification(tr("Cannot add a node outside of the world."))
        .setIcon(JOptionPane.WARNING_MESSAGE)
        .show();
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }
}
