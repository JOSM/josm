//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Aligns all selected nodes within a circle. (Useful for roundabouts)
 *
 * @author Matthew Newton
 * @author Petr Dlouhý
 */
public final class AlignInCircleAction extends JosmAction {

    public AlignInCircleAction() {
        super(tr("Align Nodes in Circle"), "aligncircle", tr("Move the selected nodes into a circle."),
                Shortcut.registerShortcut("tools:aligncircle", tr("Tool: {0}", tr("Align Nodes in Circle")),
                        KeyEvent.VK_O, Shortcut.GROUP_EDIT), true);
    }

    public double distance(EastNorth n, EastNorth m) {
        double easd, nord;
        easd = n.east() - m.east();
        nord = n.north() - m.north();
        return Math.sqrt(easd * easd + nord * nord);
    }

    public class PolarCoor {
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

    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        Collection<Node> nodes = new LinkedList<Node>();
        Collection<Way> ways = new LinkedList<Way>();
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
            Way way = (Way) ways.toArray()[0];

            // some more special combinations:
            // When is selected node that is part of the way, then make a regular polygon, selected
            // node doesn't move.
            // I haven't got better idea, how to activate that function.
            //
            // When one way and one node is selected, set center to position of that node.
            // When one more node, part of the way, is selected, set the radius equal to the
            // distance between two nodes.
            if (nodes.size() > 0) {
                if (nodes.size() == 1 && way.nodes.contains(nodes.toArray()[0])) {
                    regular = true;
                } else {

                    center = ((Node) nodes.toArray()[way.nodes.contains(nodes.toArray()[0]) ? 1 : 0]).getEastNorth();
                    if (nodes.size() == 2) {
                        radius = distance(((Node) nodes.toArray()[0]).getEastNorth(), ((Node) nodes.toArray()[1]).getEastNorth());
                    }
                }
                nodes = new LinkedList<Node>();
            }

            for (Node n : way.nodes) {
                if (!nodes.contains(n)) {
                    nodes.add(n);
                }
            }
        }

        if (nodes.size() < 4) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select at least four nodes."));
            return;
        }

        if (center == null) {
            // Compute the centroid of nodes

            BigDecimal area = new BigDecimal("0");
            BigDecimal north = new BigDecimal("0");
            BigDecimal east = new BigDecimal("0");

            // See http://en.wikipedia.org/w/index.php?title=Centroid&oldid=294224857#Centroid_of_polygon for the equation used here
            for (int i = 0; i < nodes.size(); i++) {
                EastNorth n0 = ((Node) nodes.toArray()[i]).getEastNorth();
                EastNorth n1 = ((Node) nodes.toArray()[(i+1) % nodes.size()]).getEastNorth();

                BigDecimal x0 = new BigDecimal(n0.east());
                BigDecimal y0 = new BigDecimal(n0.north());
                BigDecimal x1 = new BigDecimal(n1.east());
                BigDecimal y1 = new BigDecimal(n1.north());

                BigDecimal k = x0.multiply(y1, MathContext.DECIMAL128).subtract(y0.multiply(x1, MathContext.DECIMAL128));

                area = area.add(k, MathContext.DECIMAL128);
                east = east.add(k.multiply(x0.add(x1, MathContext.DECIMAL128), MathContext.DECIMAL128));
                north = north.add(k.multiply(y0.add(y1, MathContext.DECIMAL128), MathContext.DECIMAL128));

            }

            BigDecimal d = new BigDecimal("2");
            area  = area.divide(d, MathContext.DECIMAL128);
            d = new BigDecimal("6");
            north = north.divide(d.multiply(area, MathContext.DECIMAL128), MathContext.DECIMAL128);
            east = east.divide(d.multiply(area, MathContext.DECIMAL128), MathContext.DECIMAL128);

            center = new EastNorth(east.doubleValue(), north.doubleValue());

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
            pc = new PolarCoor(((Node) nodes.toArray()[0]).getEastNorth(), center, 0);

            if (pc.angle > (new PolarCoor(((Node) nodes.toArray()[1]).getEastNorth(), center, 0).angle)) {
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
}
