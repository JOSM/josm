// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * @author Alain Delplanque
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
        private double radius;
        private double angle;
        private EastNorth origin = new EastNorth(0, 0);
        private double azimuth = 0;

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


    /**
     * Perform AlignInCircle action.
     *
     * A fixed node is a node for which it is forbidden to change the angle relative to center of the circle.
     * All other nodes are uniformly distributed.
     *
     * Case 1: One unclosed way.
     * --> allow action, and align selected way nodes
     * If nodes contained by this way are selected, there are fix.
     * If nodes outside from the way are selected there are ignored.
     *
     * Case 2: One or more ways are selected and can be joined into a polygon
     * --> allow action, and align selected ways nodes
     * If 1 node outside of way is selected, it became center
     * If 1 node outside and 1 node inside are selected there define center and radius
     * If no outside node and 2 inside nodes are selected those 2 nodes define diameter
     * In all other cases outside nodes are ignored
     * In all cases, selected nodes are fix, nodes with more than one referrers are fix
     * (first referrer is the selected way)
     *
     * Case 3: Only nodes are selected
     * --> Align these nodes, all are fix
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        List<Node> nodes = new LinkedList<>();
        // fixNodes: All nodes for which the angle relative to center should not be modified
        Set<Node> fixNodes = new HashSet<>();
        List<Way> ways = new LinkedList<>();
        EastNorth center = null;
        double radius = 0;

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Node) {
                nodes.add((Node) osm);
            } else if (osm instanceof Way) {
                ways.add((Way) osm);
            }
        }

        if (ways.size() == 1 && ways.get(0).firstNode() != ways.get(0).lastNode()) {
            // Case 1
            Way w = ways.get(0);
            fixNodes.add(w.firstNode());
            fixNodes.add(w.lastNode());
            fixNodes.addAll(nodes);
            fixNodes.addAll(collectNodesWithExternReferers(ways));
            // Temporary closed way used to reorder nodes
            Way closedWay = new Way(w);
            closedWay.addNode(w.firstNode());
            List<Way> usedWays = new ArrayList<>(1);
            usedWays.add(closedWay);
            nodes = collectNodesAnticlockwise(usedWays);
        } else if (!ways.isEmpty() && checkWaysArePolygon(ways)) {
            // Case 2
            List<Node> inside = new ArrayList<>();
            List<Node> outside = new ArrayList<>();

            for(Node n: nodes) {
                boolean isInside = false;
                for(Way w: ways) {
                    if(w.getNodes().contains(n)) {
                        isInside = true;
                        break;
                    }
                }
                if(isInside)
                    inside.add(n);
                else
                    outside.add(n);
            }

            if(outside.size() == 1 && inside.isEmpty()) {
                center = outside.get(0).getEastNorth();
            } else if(outside.size() == 1 && inside.size() == 1) {
                center = outside.get(0).getEastNorth();
                radius = distance(center, inside.get(0).getEastNorth());
            } else if(inside.size() == 2 && outside.isEmpty()) {
                // 2 nodes inside, define diameter
                EastNorth en0 = inside.get(0).getEastNorth();
                EastNorth en1 = inside.get(1).getEastNorth();
                center = new EastNorth((en0.east() + en1.east()) / 2, (en0.north() + en1.north()) / 2);
                radius = distance(en0, en1) / 2;
            }

            fixNodes.addAll(inside);
            fixNodes.addAll(collectNodesWithExternReferers(ways));
            nodes = collectNodesAnticlockwise(ways);
            if (nodes.size() < 4) {
                new Notification(
                        tr("Not enough nodes in selected ways."))
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_SHORT)
                .show();
                return;
            }
        } else if (ways.isEmpty() && nodes.size() > 3) {
            // Case 3
            fixNodes.addAll(nodes);
            // No need to reorder nodes since all are fix
        } else {
            // Invalid action
            new Notification(
                    tr("Please select at least four nodes."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        if (center == null) {
            // Compute the center of nodes
            center = Geometry.getCenter(nodes);
            if (center == null) {
                new Notification(tr("Cannot determine center of selected nodes."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
                return;
            }
        }

        // Now calculate the average distance to each node from the
        // center. This method is ok as long as distances are short
        // relative to the distance from the N or S poles.
        if (Double.doubleToRawLongBits(radius) == 0) {
            for (Node n : nodes) {
                radius += distance(center, n.getEastNorth());
            }
            radius = radius / nodes.size();
        }

        if(!actionAllowed(nodes)) return;

        Collection<Command> cmds = new LinkedList<>();

        // Move each node to that distance from the center.
        // Nodes that are not "fix" will be adjust making regular arcs.
        int nodeCount = nodes.size();
        // Search first fixed node
        int startPosition = 0;
        for(startPosition = 0; startPosition < nodeCount; startPosition++)
            if(fixNodes.contains(nodes.get(startPosition % nodeCount))) break;
        int i = startPosition; // Start position for current arc
        int j; // End position for current arc
        while(i < startPosition + nodeCount) {
            for(j = i + 1; j < startPosition + nodeCount; j++)
                if(fixNodes.contains(nodes.get(j % nodeCount))) break;
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

        Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Circle"), cmds));
        Main.map.repaint();
    }

    /**
     * Collect all nodes with more than one referrer.
     * @param ways Ways from witch nodes are selected
     * @return List of nodes with more than one referrer
     */
    private List<Node> collectNodesWithExternReferers(List<Way> ways) {
        List<Node> withReferrers = new ArrayList<>();
        for(Way w: ways)
            for(Node n: w.getNodes())
                if(n.getReferrers().size() > 1)
                    withReferrers.add(n);
        return withReferrers;
    }

    /**
     * Assuming all ways can be joined into polygon, create an ordered list of node.
     * @param ways List of ways to be joined
     * @return Nodes anticlockwise ordered
     */
    private List<Node> collectNodesAnticlockwise(List<Way> ways) {
        List<Node> nodes = new ArrayList<>();
        Node firstNode = ways.get(0).firstNode();
        Node lastNode = null;
        Way lastWay = null;
        while(firstNode != lastNode) {
            if(lastNode == null) lastNode = firstNode;
            for(Way way: ways) {
                if(way == lastWay) continue;
                if(way.firstNode() == lastNode) {
                    List<Node> wayNodes = way.getNodes();
                    for(int i = 0; i < wayNodes.size() - 1; i++)
                        nodes.add(wayNodes.get(i));
                    lastNode = way.lastNode();
                    lastWay = way;
                    break;
                }
                if(way.lastNode() == lastNode) {
                    List<Node> wayNodes = way.getNodes();
                    for(int i = wayNodes.size() - 1; i > 0; i--)
                        nodes.add(wayNodes.get(i));
                    lastNode = way.firstNode();
                    lastWay = way;
                    break;
                }
            }
        }
        // Check if nodes are in anticlockwise order
        int nc = nodes.size();
        double area = 0;
        for(int i = 0; i < nc; i++) {
            EastNorth p1 = nodes.get(i).getEastNorth();
            EastNorth p2 = nodes.get((i+1) % nc).getEastNorth();
            area += p1.east()*p2.north() - p2.east()*p1.north();
        }
        if(area < 0)
            Collections.reverse(nodes);
        return nodes;
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

    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null && !getCurrentDataSet().getSelected().isEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
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
