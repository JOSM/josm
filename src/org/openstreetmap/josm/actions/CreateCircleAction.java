// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Geometry;
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
 * @since 996
 *
 * @author Henry Loenwind
 * @author Sebastian Masch
 * @author Alain Delplanque
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

    /**
     * Distributes nodes according to the algorithm of election with largest remainder.
     * @param angles Array of PolarNode ordered by increasing angles
     * @param nodesCount Number of nodes to be distributed
     * @return Array of number of nodes to put in each arc
     */
    private int[] distributeNodes(PolarNode[] angles, int nodesCount) {
        int[] count = new int[angles.length];
        double[] width = new double[angles.length];
        double[] remainder = new double[angles.length];
        for(int i = 0; i < angles.length; i++) {
            width[i] = angles[(i+1) % angles.length].a - angles[i].a;
            if(width[i] < 0)
                width[i] += 2*Math.PI;
        }
        int assign = 0;
        for(int i = 0; i < angles.length; i++) {
            double part = width[i] / 2.0 / Math.PI * nodesCount;
            count[i] = (int) Math.floor(part);
            remainder[i] = part - count[i];
            assign += count[i];
        }
        while(assign < nodesCount) {
            int imax = 0;
            for(int i = 1; i < angles.length; i++)
                if(remainder[i] > remainder[imax])
                    imax = i;
            count[imax]++;
            remainder[imax] = 0;
            assign++;
        }
        return count;
    }

    /**
     * Class designed to create a couple between a node and its angle relative to the center of the circle.
     */
    private static class PolarNode {
        double a;
        Node node;
        
        PolarNode(EastNorth center, Node n) {
            EastNorth pt = n.getEastNorth();
            this.a = Math.atan2(pt.north() - center.north(), pt.east() - center.east());
            this.node = n;
        }
    }

    /**
     * Comparator used to order PolarNode relative to their angle.
     */
    private static class PolarNodeComparator implements Comparator<PolarNode> {

        @Override
        public int compare(PolarNode pc1, PolarNode pc2) {
            if(pc1.a < pc2.a)
                return -1;
            else if(pc1.a == pc2.a)
                return 0;
            else
                return 1;
        }
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

        if (nodes.size() < 2 || nodes.size() > 3) {
            new Notification(
                    tr("Please select exactly two or three nodes or one way with exactly two or three nodes."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_LONG)
                    .show();
            return;
        }

        // now we can start doing things to OSM data
        Collection<Command> cmds = new LinkedList<Command>();
        EastNorth center = null;
        
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
            center = new EastNorth(xc, yc);
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
        double r = Math.sqrt(Math.pow(center.east()-n1.east(),2) +
                Math.pow(center.north()-n1.north(),2));

        // Order nodes by angle
        PolarNode[] angles = new PolarNode[nodes.size()];
        for(int i = 0; i < nodes.size(); i++) {
            angles[i] = new PolarNode(center, nodes.get(i));
        }
        Arrays.sort(angles, new PolarNodeComparator());
        int[] count = distributeNodes(angles,
                numberOfNodesInCircle >= nodes.size() ? numberOfNodesInCircle - nodes.size() : 0);

        // build a way for the circle
        List<Node> wayToAdd = new ArrayList<Node>();
        for(int i = 0; i < nodes.size(); i++) {
            wayToAdd.add(angles[i].node);
            double delta = angles[(i+1) % nodes.size()].a - angles[i].a;
            if(delta < 0)
                delta += 2*Math.PI;
            for(int j = 0; j < count[i]; j++) {
                double alpha = angles[i].a + (j+1)*delta/(count[i]+1);
                double x = center.east() + r*Math.cos(alpha);
                double y = center.north() + r*Math.sin(alpha);
                LatLon ll = Main.getProjection().eastNorth2latlon(new EastNorth(x,y));
                if (ll.isOutSideWorld()) {
                    notifyNodesNotOnCircle();
                    return;
                }
                Node n = new Node(ll);
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
