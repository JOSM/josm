//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Aligns all selected nodes within a circle. (Useful for roundabouts)
 *
 * @since 146
 *
 * @author Matthew Newton
 * @author Petr Dlouhý
 * @author Teemu Koskinen
 */
public final class AlignInCircleAction extends JosmAction {

    /**
     * Constructs a new {@code AlignInCircleAction}.
     */
    public AlignInCircleAction() {
        super(tr("Align Nodes in Circle"), "aligncircle", tr("Move the selected nodes into a circle."),
                Shortcut.registerShortcut("tools:aligncircle", tr("Tool: {0}", tr("Align Nodes in Circle")),
                        KeyEvent.VK_O, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/AlignInCircle"));
    }

    private static double distance(EastNorth n, EastNorth m) {
        double easd, nord;
        easd = n.east() - m.east();
        nord = n.north() - m.north();
        return Math.sqrt(easd * easd + nord * nord);
    }

    public static class PolarCoor {
        double radius;
        double angle;
        EastNorth origin = new EastNorth(0, 0);
        double azimuth = 0;

        PolarCoor(double radius, double angle) {
            this(radius, angle, new EastNorth(0, 0), 0);
        }

        PolarCoor(double radius, double angle, EastNorth origin, double azimuth) {
            this.radius = radius;
            this.angle = angle;
            this.origin = origin;
            this.azimuth = azimuth;
        }

        PolarCoor(EastNorth en) {
            this(en, new EastNorth(0, 0), 0);
        }

        PolarCoor(EastNorth en, EastNorth origin, double azimuth) {
            radius = distance(en, origin);
            angle = Math.atan2(en.north() - origin.north(), en.east() - origin.east());
            this.origin = origin;
            this.azimuth = azimuth;
        }

        public EastNorth toEastNorth() {
            return new EastNorth(radius * Math.cos(angle - azimuth) + origin.east(), radius * Math.sin(angle - azimuth)
                    + origin.north());
        }

        /**
         * Create a MoveCommand to move a node to this PolarCoor.
         * @param n Node to move
         * @return new MoveCommand
         */
        public MoveCommand createMoveCommand(Node n) {
            EastNorth en = toEastNorth();
            return new MoveCommand(n, en.east() - n.getEastNorth().east(), en.north() - n.getEastNorth().north());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        List<Node> nodes = new LinkedList<Node>();
        List<Way> ways = new LinkedList<Way>();
        EastNorth center = null;
        double radius = 0;
        boolean regular = false;

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Node) {
                nodes.add((Node) osm);
            } else if (osm instanceof Way) {
                ways.add((Way) osm);
            }
        }

        // special case if no single nodes are selected and exactly one way is:
        // then use the way's nodes
        if ((nodes.size() <= 2) && checkWaysArePolygon(ways)) {
            // some more special combinations:
            // When is selected node that is part of the way, then make a regular polygon, selected
            // node doesn't move.
            // I haven't got better idea, how to activate that function.
            //
            // When one way and one node is selected, set center to position of that node.
            // When one more node, part of the way, is selected, set the radius equal to the
            // distance between two nodes.
            if (nodes.size() == 1 && ways.size() == 1) {
                // Regular polygons are allowed only if there is just one way
                // Should be remove regular are now default for all nodes with no more than 1 referrer.
                Way way = ways.get(0);
                if (nodes.size() == 1 && way.containsNode(nodes.get(0)) && allowRegularPolygon(way.getNodes()))
                    regular = true;
            }
            if (nodes.size() >= 1) {
                boolean[] isContained = new boolean[nodes.size()];
                for(int i = 0; i < nodes.size(); i++) {
                    Node n = nodes.get(i);
                    isContained[i] = false;
                    for(Way way: ways)
                        if(way.containsNode(n)) {
                            isContained[i] = true;
                            break;
                        }
                }
                if(nodes.size() == 1) {
                    if(!isContained[0])
                        center = nodes.get(0).getEastNorth();
                } else {
                    if(!isContained[0] && !isContained[1]) {
                        // 2 nodes outside of way, can't choose one as center
                        new Notification(
                                tr("Please select only one node as center."))
                                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                                .setDuration(Notification.TIME_SHORT)
                                .show();
                        return;
                    } else if (!isContained[0] || !isContained[1]) {
                        // 1 node inside and 1 outside, outside is center, inside node define radius
                        center = nodes.get(isContained[0] ? 1 : 0).getEastNorth();
                        radius = distance(nodes.get(0).getEastNorth(), nodes.get(1).getEastNorth());
                    } else {
                        // 2 nodes inside, define diameter
                        EastNorth en0 = nodes.get(0).getEastNorth();
                        EastNorth en1 = nodes.get(1).getEastNorth();
                        center = new EastNorth((en0.east() + en1.east()) / 2, (en0.north() + en1.north()) / 2);
                        radius = distance(en0, en1) / 2;
                    }
                }
            }
            nodes.clear();

            for(Way way: ways)
                for (Node n : way.getNodes()) {
                    if (!nodes.contains(n)) {
                        nodes.add(n);
                    }
                }
        }

        if (nodes.size() < 4) {
            new Notification(
                    tr("Please select at least four nodes."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        // Reorder the nodes if they didn't come from a single way
        if (ways.size() != 1) {
            // First calculate the average point

            BigDecimal east = BigDecimal.ZERO;
            BigDecimal north = BigDecimal.ZERO;

            for (Node n : nodes) {
                BigDecimal x = new BigDecimal(n.getEastNorth().east());
                BigDecimal y = new BigDecimal(n.getEastNorth().north());
                east = east.add(x, MathContext.DECIMAL128);
                north = north.add(y, MathContext.DECIMAL128);
            }
            BigDecimal nodesSize = new BigDecimal(nodes.size());
            east = east.divide(nodesSize, MathContext.DECIMAL128);
            north = north.divide(nodesSize, MathContext.DECIMAL128);

            EastNorth average = new EastNorth(east.doubleValue(), north.doubleValue());
            List<Node> newNodes = new LinkedList<Node>();

            // Then reorder them based on heading from the average point
            while (!nodes.isEmpty()) {
                double maxHeading = -1.0;
                Node maxNode = null;
                for (Node n : nodes) {
                    double heading = average.heading(n.getEastNorth());
                    if (heading > maxHeading) {
                        maxHeading = heading;
                        maxNode = n;
                    }
                }
                newNodes.add(maxNode);
                nodes.remove(maxNode);
            }

            nodes = newNodes;
        }

        if (center == null) {
            // Compute the centroid of nodes
            center = Geometry.getCentroid(nodes);
        }
        // Node "center" now is central to all selected nodes.

        // Now calculate the average distance to each node from the
        // centre. This method is ok as long as distances are short
        // relative to the distance from the N or S poles.
        if (radius == 0) {
            for (Node n : nodes) {
                radius += distance(center, n.getEastNorth());
            }
            radius = radius / nodes.size();
        }

        if(!actionAllowed(nodes)) return;

        Collection<Command> cmds = new LinkedList<Command>();

        PolarCoor pc;

        if (regular) { // Make a regular polygon
            double angle = Math.PI * 2 / nodes.size();
            pc = new PolarCoor(nodes.get(0).getEastNorth(), center, 0);

            if (pc.angle > (new PolarCoor(nodes.get(1).getEastNorth(), center, 0).angle)) {
                angle *= -1;
            }

            pc.radius = radius;
            for (Node n : nodes) {
                EastNorth no = pc.toEastNorth();
                cmds.add(new MoveCommand(n, no.east() - n.getEastNorth().east(), no.north() - n.getEastNorth().north()));
                pc.angle += angle;
            }
        } else { // Move each node to that distance from the center.
            int nodeCount = nodes.size();
            // Search first fixed node
            int startPosition = 0;
            for(startPosition = 0; startPosition < nodeCount; startPosition++)
                if(isFixNode(nodes.get(startPosition % nodeCount), sel)) break;
            int i = startPosition; // Start position for current arc
            int j; // End position for current arc
            while(i < startPosition + nodeCount) {
                for(j = i + 1; j < startPosition + nodeCount; j++)
                    if(isFixNode(nodes.get(j % nodeCount), sel)) break;
                Node first = nodes.get(i % nodeCount);
                PolarCoor pcFirst = new PolarCoor(first.getEastNorth(), center, 0);
                pcFirst.radius = radius;
                cmds.add(pcFirst.createMoveCommand(first));
                if(j > i + 1) {
                    double delta;
                    if(j == i + nodeCount) {
                        delta = 2 * Math.PI / nodeCount;
                    } else {
                        PolarCoor pcLast = new PolarCoor(nodes.get(j % nodeCount).getEastNorth(), center, 0);
                        delta = pcLast.angle - pcFirst.angle;
                        if(delta < 0) // Assume each PolarCoor.angle is in range ]-pi; pi]
                            delta +=  2*Math.PI;
                        delta /= j - i;
                    }
                    for(int k = i+1; k < j; k++) {
                        PolarCoor p = new PolarCoor(radius, pcFirst.angle + (k-i)*delta, center, 0);
                        cmds.add(p.createMoveCommand(nodes.get(k % nodeCount)));
                    }
                }
                i = j; // Update start point for next iteration
            }
        }
        
        Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Circle"), cmds));
        Main.map.repaint();
    }

    /**
     * Check if one or more nodes are outside of download area
     * @param nodes Nodes to check
     * @return true if action can be done
     */
    private boolean actionAllowed(Collection<Node> nodes) {
        boolean outside = false;
        for(Node n: nodes)
            if(n.isOutsideDownloadArea()) {
                outside = true;
                break;
            }
        if(outside)
            new Notification(
                    tr("One or more nodes involved in this action is outside of the downloaded area."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
        return true;
    }
    
    /**
     * Test if angle of a node can be change.
     * @param n Node
     * @param sel Selection which action is apply
     * @return true is this node does't have a fix angle 
     */
    private boolean isFixNode(Node n, Collection<OsmPrimitive> sel) {
        List<OsmPrimitive> referrers = n.getReferrers();
        if(referrers.isEmpty()) return false;
        if(sel.contains(n) || referrers.size() > 1 || !sel.contains(referrers.get(0))) return true;
        return false;
    }
    
    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null && !getCurrentDataSet().getSelected().isEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    /**
     * Determines if a regular polygon is allowed to be created with the given nodes collection.
     * @param nodes The nodes collection to check.
     * @return true if all nodes in the given collection are referred by the same object, and no other one (see #8431)
     */
    protected static boolean allowRegularPolygon(Collection<Node> nodes) {
        Set<OsmPrimitive> allReferrers = new HashSet<OsmPrimitive>();
        for (Node n : nodes) {
            List<OsmPrimitive> referrers = n.getReferrers();
            if (referrers.size() > 1 || (allReferrers.addAll(referrers) && allReferrers.size() > 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if ways can be joined into a polygon.
     * @param ways The ways collection to check
     * @return true if all ways can be joined into a polygon
     */
    protected static boolean checkWaysArePolygon(Collection<Way> ways) {
        // For each way, nodes strictly between first and last should't be reference by an other way
        for(Way way: ways) {
            for(Node node: way.getNodes()) {
                if(node == way.firstNode() || node == way.lastNode()) continue;
                for(Way wayOther: ways) {
                    if(way == wayOther) continue;
                    if(node.getReferrers().contains(wayOther)) return false;
                }
            }
        }
        // Test if ways can be joined
        Way currentWay = null;
        Node startNode = null, endNode = null;
        int used = 0;
        while(true) {
            Way nextWay = null;
            for(Way w: ways) {
                if(w.firstNode() == w.lastNode()) return ways.size() == 1;
                if(w == currentWay) continue;
                if(currentWay == null) {
                    nextWay = w;
                    startNode = w.firstNode();
                    endNode = w.lastNode();
                    break;
                }
                if(w.firstNode() == endNode) {
                    nextWay = w;
                    endNode = w.lastNode();
                    break;
                }
                if(w.lastNode() == endNode) {
                    nextWay = w;
                    endNode = w.firstNode();
                    break;
                }
            }
            if(nextWay == null) return false;
            used += 1;
            currentWay = nextWay;
            if(endNode == startNode) return used == ways.size();
        }
    }
}
