// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * - Create a new circle from two selected nodes or a way with 2 nodes which represent the diameter of the circle.
 * - Create a new circle from three selected nodes--or a way with 3 nodes.
 * - Useful for roundabouts
 *
 * Note: If a way is selected, it is changed. If nodes are selected a new way is created.
 *       So if you've got a way with nodes it makes a difference between running this on the way or the nodes!
 *
 * BTW: Someone might want to implement projection corrections for this...
 *
 * @since 996
 *
 * @author Henry Loenwind
 * @author Sebastian Masch
 */
public final class CreateCircleAction extends JosmAction {

    /**
     * Constructs a new {@code CreateCircleAction}.
     */
    public CreateCircleAction() {
        super(tr("Create Circle"), "aligncircle", tr("Create a circle from three selected nodes."),
            Shortcut.registerShortcut("tools:createcircle", tr("Tool: {0}", tr("Create Circle")),
            KeyEvent.VK_O, Shortcut.SHIFT), true, "createcircle", true);
        putValue("help", ht("/Action/CreateCircle"));
    }

    private double calcang(double xc, double yc, double x, double y) {
        // calculate the angle from xc|yc to x|y
        if (xc == x && yc == y)
            return 0; // actually invalid, but we won't have this case in this context
        double yd = Math.abs(y - yc);
        if (yd == 0 && xc < x)
            return 0;
        if (yd == 0 && xc > x)
            return Math.PI;
        double xd = Math.abs(x - xc);
        double a = Math.atan2(xd, yd);
        if (y > yc) {
            a = Math.PI - a;
        }
        if (x < xc) {
            a = -a;
        }
        a = 1.5*Math.PI + a;
        if (a < 0) {
            a += 2*Math.PI;
        }
        if (a >= 2*Math.PI) {
            a -= 2*Math.PI;
        }
        return a;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        int numberOfNodesInCircle = Main.pref.getInteger("createcircle.nodecount", 8);
        if (numberOfNodesInCircle < 1) {
            numberOfNodesInCircle = 1;
        } else if (numberOfNodesInCircle > 100) {
            numberOfNodesInCircle = 100;
        }

        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        List<Node> nodes = new LinkedList<Node>();
        Way existingWay = null;

        for (OsmPrimitive osm : sel)
            if (osm instanceof Node) {
                nodes.add((Node)osm);
            }

        // special case if no single nodes are selected and exactly one way is:
        // then use the way's nodes
        if (nodes.isEmpty() && (sel.size() == 1)) {
            for (OsmPrimitive osm : sel)
                if (osm instanceof Way) {
                    existingWay = ((Way)osm);
                    for (Node n : ((Way)osm).getNodes())
                    {
                        if(!nodes.contains(n)) {
                            nodes.add(n);
                        }
                    }
                }
        }

        // now we can start doing things to OSM data
        Collection<Command> cmds = new LinkedList<Command>();

        if (nodes.size() == 2) {
            // diameter: two single nodes needed or a way with two nodes

            Node   n1 = nodes.get(0);
            double x1 = n1.getEastNorth().east();
            double y1 = n1.getEastNorth().north();
            Node   n2 = nodes.get(1);
            double x2 = n2.getEastNorth().east();
            double y2 = n2.getEastNorth().north();

            // calculate the center (xc/yc)
            double xc = 0.5 * (x1 + x2);
            double yc = 0.5 * (y1 + y2);

            // calculate the radius (r)
            double r = Math.sqrt(Math.pow(xc-x1,2) + Math.pow(yc-y1,2));

            // find where to put the existing nodes
            double a1 = calcang(xc, yc, x1, y1);
            double a2 = calcang(xc, yc, x2, y2);
            if (a1 < a2) { double at = a1; Node nt = n1; a1 = a2; n1 = n2; a2 = at; n2 = nt; }

            // build a way for the circle
            List<Node> wayToAdd = new ArrayList<Node>(numberOfNodesInCircle + 1);

            for (int i = 1; i <= numberOfNodesInCircle; i++) {
                double a = a2 + 2*Math.PI*(1.0 - i/(double)numberOfNodesInCircle); // "1-" to get it clock-wise

                // insert existing nodes if they fit before this new node (999 means "already added this node")
                if ((a1 < 999) && (a1 > a - 1E-9) && (a1 < a + 1E-9)) {
                    wayToAdd.add(n1);
                    a1 = 999;
                }
                else if ((a2 < 999) && (a2 > a - 1E-9) && (a2 < a + 1E-9)) {
                    wayToAdd.add(n2);
                    a2 = 999;
                }
                else {
                    // get the position of the new node and insert it
                    double x = xc + r*Math.cos(a);
                    double y = yc + r*Math.sin(a);
                    Node n = new Node(Main.getProjection().eastNorth2latlon(new EastNorth(x,y)));
                    wayToAdd.add(n);
                    cmds.add(new AddCommand(n));
                }
            }
            wayToAdd.add(wayToAdd.get(0)); // close the circle
            if (existingWay == null) {
                Way newWay = new Way();
                newWay.setNodes(wayToAdd);
                cmds.add(new AddCommand(newWay));
            } else {
                Way newWay = new Way(existingWay);
                newWay.setNodes(wayToAdd);
                cmds.add(new ChangeCommand(existingWay, newWay));
            }

            // the first node may be unused/abandoned if createcircle.nodecount is odd
            if (a1 < 999) {
                // if it is, delete it
                List<OsmPrimitive> parents = n1.getReferrers();
                if (parents.isEmpty() || ((parents.size() == 1) && (parents.contains(existingWay)))) {
                    cmds.add(new DeleteCommand(n1));
                }
            }

        } else if (nodes.size() == 3) {
            // triangle: three single nodes needed or a way with three nodes

            // let's get some shorter names
            Node   n1 = nodes.get(0);
            double x1 = n1.getEastNorth().east();
            double y1 = n1.getEastNorth().north();
            Node   n2 = nodes.get(1);
            double x2 = n2.getEastNorth().east();
            double y2 = n2.getEastNorth().north();
            Node   n3 = nodes.get(2);
            double x3 = n3.getEastNorth().east();
            double y3 = n3.getEastNorth().north();

            // calculate the center (xc/yc)
            double s = 0.5*((x2 - x3)*(x1 - x3) - (y2 - y3)*(y3 - y1));
            double sUnder = (x1 - x2)*(y3 - y1) - (y2 - y1)*(x1 - x3);

            if (sUnder == 0) {
                notifyNodesNotOnCircle();
                return;
            }

            s /= sUnder;

            double xc = 0.5*(x1 + x2) + s*(y2 - y1);
            double yc = 0.5*(y1 + y2) + s*(x1 - x2);

            // calculate the radius (r)
            double r = Math.sqrt(Math.pow(xc-x1,2) + Math.pow(yc-y1,2));

            // find where to put the existing nodes
            double a1 = calcang(xc, yc, x1, y1);
            double a2 = calcang(xc, yc, x2, y2);
            double a3 = calcang(xc, yc, x3, y3);
            if (a1 < a2) { double at = a1; Node nt = n1; a1 = a2; n1 = n2; a2 = at; n2 = nt; }
            if (a2 < a3) { double at = a2; Node nt = n2; a2 = a3; n2 = n3; a3 = at; n3 = nt; }
            if (a1 < a2) { double at = a1; Node nt = n1; a1 = a2; n1 = n2; a2 = at; n2 = nt; }

            // build a way for the circle
            List<Node> wayToAdd = new ArrayList<Node>();
            for (int i = 1; i <= numberOfNodesInCircle; i++) {
                double a = 2*Math.PI*(1.0 - i/(double)numberOfNodesInCircle); // "1-" to get it clock-wise
                // insert existing nodes if they fit before this new node (999 means "already added this node")
                if (a1 < 999 && a1 > a) {
                    wayToAdd.add(n1);
                    a1 = 999;
                }
                if (a2 < 999 && a2 > a) {
                    wayToAdd.add(n2);
                    a2 = 999;
                }
                if (a3 < 999 && a3 > a) {
                    wayToAdd.add(n3);
                    a3 = 999;
                }
                // get the position of the new node and insert it
                double x = xc + r*Math.cos(a);
                double y = yc + r*Math.sin(a);
                LatLon ll = Main.getProjection().eastNorth2latlon(new EastNorth(x,y));
                if (ll.isOutSideWorld()) {
                    notifyNodesNotOnCircle();
                    return;
                }
                Node n = new Node(ll);
                wayToAdd.add(n);
                cmds.add(new AddCommand(n));
            }
            wayToAdd.add(wayToAdd.get(0)); // close the circle
            if (existingWay == null) {
                Way newWay = new Way();
                newWay.setNodes(wayToAdd);
                cmds.add(new AddCommand(newWay));
            } else {
                Way newWay = new Way(existingWay);
                newWay.setNodes(wayToAdd);
                cmds.add(new ChangeCommand(existingWay, newWay));
            }

        } else {
            new Notification(
                    tr("Please select exactly two or three nodes or one way with exactly two or three nodes."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_LONG)
                    .show();
            return;
        }

        Main.main.undoRedo.add(new SequenceCommand(tr("Create Circle"), cmds));
        Main.map.repaint();
    }

    private static void notifyNodesNotOnCircle() {
        new Notification(
                tr("Those nodes are not in a circle. Aborting."))
                .setIcon(JOptionPane.WARNING_MESSAGE)
                .show();
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
