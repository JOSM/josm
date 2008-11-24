//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
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

    public double determinant(double[][] mat) {
        double result = 0;

        if (mat.length == 1) {
            result = mat[0][0];
            return result;
        }

        if (mat.length == 2) {
            result = mat[0][0] * mat[1][1] - mat[0][1] * mat[1][0];
            return result;
        }

        for (int i = 0; i < mat[0].length; i++) {
            double temp[][] = new double[mat.length - 1][mat[0].length - 1];
            for (int j = 1; j < mat.length; j++) {
                for (int k = 0; k < mat[0].length; k++) {
                    if (k < i) {
                        temp[j - 1][k] = mat[j][k];
                    } else if (k > i) {
                        temp[j - 1][k - 1] = mat[j][k];
                    }
                }
            }
            result += mat[0][i] * Math.pow(-1, (double) i) * determinant(temp);
        }
        return result;
    }

    public double distance(EastNorth n, EastNorth m) {
        double easd, nord;
        easd = n.east() - m.east();
        nord = n.north() - m.north();
        return Math.sqrt(easd * easd + nord * nord);
    }

    public EastNorth circumcenter(EastNorth i, EastNorth j, EastNorth k) {
        // move to 0,0, to eliminate numeric errors
        double ie = i.east() - i.east();
        double in = i.north() - i.north();
        double je = j.east() - i.east();
        double jn = j.north() - i.north();
        double ke = k.east() - i.east();
        double kn = k.north() - i.north();
        double[][] ma = { { ie, in, 1 }, { je, jn, 1 }, { ke, kn, 1 } };
        double[][] mbx = { { (ie * ie + in * in), in, 1 }, { (je * je + jn * jn), jn, 1 },
                { (ke * ke + kn * kn), kn, 1 } };
        double[][] mby = { { ie * ie + in * in, ie, 1 }, { je * je + jn * jn, je, 1 }, { ke * ke + kn * kn, ke, 1 } };
        double a = determinant(ma);
        double bx = determinant(mbx);
        double by = determinant(mby);
        EastNorth result = new EastNorth(bx / (2 * a) + i.east(), -by / (2 * a) + i.north());

        Node n = new Node(Main.proj.eastNorth2latlon(result));
        if (n.coor.isOutSideWorld()) {
            JOptionPane.showMessageDialog(Main.parent, tr("Some of the nodes are (almost) in the line"));
            return null;
        }
        return result;
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
        Collection<OsmPrimitive> sel = Main.ds.getSelected();
        Collection<Node> nodes = new LinkedList<Node>();
        Collection<Way> ways = new LinkedList<Way>();
        Node center = null;
        Node node = null;
        double radius = 0;
        boolean regular = false;

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Node)
                nodes.add((Node) osm);
            else if (osm instanceof Way)
                ways.add((Way) osm);
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
                if (nodes.size() == 1 && way.nodes.contains((Node) nodes.toArray()[0])) {
                    node = (Node) nodes.toArray()[0];
                    regular = true;
                } else {

                    center = (Node) nodes.toArray()[way.nodes.contains((Node) nodes.toArray()[0]) ? 1 : 0];
                    if (nodes.size() == 2)
                        radius = distance(((Node) nodes.toArray()[0]).eastNorth, ((Node) nodes.toArray()[1]).eastNorth);
                }
                nodes = new LinkedList<Node>();
            }

            for (Node n : way.nodes) {
                if (!nodes.contains(n))
                    nodes.add(n);
            }
        }

        if (nodes.size() < 4) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select at least four nodes."));
            return;
        }

        // Get average position of circumcircles of the triangles of all triplets of neighbour nodes
        if (center == null) {
            center = new Node(new LatLon(0, 0));
            Node n0, n1, n2, prvni, druhy;
            n0 = (Node) nodes.toArray()[nodes.size() - 1];
            n1 = (Node) nodes.toArray()[nodes.size() - 2];
            for (Node n : nodes) {
                n2 = n1;
                n1 = n0;
                n0 = n;
                EastNorth cc = circumcenter(n0.eastNorth, n1.eastNorth, n2.eastNorth);
                if (cc == null)
                    return;
                center.eastNorth = new EastNorth(center.eastNorth.east() + cc.east(), center.eastNorth.north()
                        + cc.north());
            }

            center.eastNorth = new EastNorth(center.eastNorth.east() / nodes.size(), center.eastNorth.north()
                    / nodes.size());
            center.coor = Main.proj.eastNorth2latlon(center.eastNorth);
        }

        // Node "center" now is central to all selected nodes.

        // Now calculate the average distance to each node from the
        // centre. This method is ok as long as distances are short
        // relative to the distance from the N or S poles.
        if (radius == 0) {
            for (Node n : nodes) {
                radius += distance(center.eastNorth, n.eastNorth);
            }
            radius = radius / nodes.size();
        }

        Collection<Command> cmds = new LinkedList<Command>();

        PolarCoor pc;

        if (regular) { // Make a regular polygon
            double angle = Math.PI * 2 / nodes.size();
            pc = new PolarCoor(((Node) nodes.toArray()[0]).eastNorth, center.eastNorth, 0);

            if (pc.angle > (new PolarCoor(((Node) nodes.toArray()[1]).eastNorth, center.eastNorth, 0).angle))
                angle *= -1;

            pc.radius = radius;
            for (Node n : nodes) {
                EastNorth no = pc.toEastNorth();
                cmds.add(new MoveCommand(n, no.east() - n.eastNorth.east(), no.north() - n.eastNorth.north()));
                pc.angle += angle;
            }
        } else { // Move each node to that distance from the centre.
            for (Node n : nodes) {
                pc = new PolarCoor(n.eastNorth, center.eastNorth, 0);
                pc.radius = radius;
                EastNorth no = pc.toEastNorth();
                cmds.add(new MoveCommand(n, no.east() - n.eastNorth.east(), no.north() - n.eastNorth.north()));
            }
        }

        Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Circle"), cmds));
        Main.map.repaint();
    }
}
