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
        if ((nodes.size() <= 2) && (ways.size() == 1)) {
            Way way = ways.get(0);

            // some more special combinations:
            // When is selected node that is part of the way, then make a regular polygon, selected
            // node doesn't move.
            // I haven't got better idea, how to activate that function.
            //
            // When one way and one node is selected, set center to position of that node.
            // When one more node, part of the way, is selected, set the radius equal to the
            // distance between two nodes.
            if (nodes.size() > 0) {
                if (nodes.size() == 1 && way.containsNode(nodes.get(0)) && allowRegularPolygon(way.getNodes())) {
                    regular = true;
                } else if (nodes.size() >= 2) {
                    center = nodes.get(way.containsNode(nodes.get(0)) ? 1 : 0).getEastNorth();
                    if (nodes.size() == 2) {
                        radius = distance(nodes.get(0).getEastNorth(), nodes.get(1).getEastNorth());
                    }
                }
                nodes.clear();
            }

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
        } else { // Move each node to that distance from the centre.
            for (Node n : nodes) {
                pc = new PolarCoor(n.getEastNorth(), center, 0);
                pc.radius = radius;
                EastNorth no = pc.toEastNorth();
                cmds.add(new MoveCommand(n, no.east() - n.getEastNorth().east(), no.north() - n.getEastNorth().north()));
            }
        }

        Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Circle"), cmds));
        Main.map.repaint();
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
}
