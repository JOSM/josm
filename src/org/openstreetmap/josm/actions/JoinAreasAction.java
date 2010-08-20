// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SplitWayAction.SplitWayResult;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TigerUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

public class JoinAreasAction extends JosmAction {
    // This will be used to commit commands and unite them into one large command sequence at the end
    private LinkedList<Command> cmds = new LinkedList<Command>();
    private int cmdsCount = 0;

    /**
     * This helper class describes join ares action result.
     * @author viesturs
     *
     */
    public static class JoinAreasResult {

        public Way outerWay;
        public List<Way> innerWays;

        public boolean mergeSuccessful;
        public boolean hasChanges;
        public boolean hasRelationProblems;
    }

    // HelperClass
    // Saves a node and two positions where to insert the node into the ways
    private static class NodeToSegs implements Comparable<NodeToSegs> {
        public int pos;
        public Node n;
        public double dis;
        public NodeToSegs(int pos, Node n, LatLon dis) {
            this.pos = pos;
            this.n = n;
            this.dis = n.getCoor().greatCircleDistance(dis);
        }

        public int compareTo(NodeToSegs o) {
            if(this.pos == o.pos)
                return (this.dis - o.dis) > 0 ? 1 : -1;
                return this.pos - o.pos;
        }

        @Override
        public int hashCode() {
            return pos;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NodeToSegs)
                return compareTo((NodeToSegs) o) == 0;
            else
                return false;
        }
    }

    // HelperClass
    // Saves a relation and a role an OsmPrimitve was part of until it was stripped from all relations
    private static class RelationRole {
        public final Relation rel;
        public final String role;
        public RelationRole(Relation rel, String role) {
            this.rel = rel;
            this.role = role;
        }

        @Override
        public int hashCode() {
            return rel.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RelationRole)) return false;
            RelationRole otherMember = (RelationRole) other;
            return otherMember.role.equals(role) && otherMember.rel.equals(rel);
        }
    }

    /**
     * HelperClass
     * saves a way and the "inside" side
     * insideToTheLeft: if true left side is "in", false -right side is "in".
     * Left and right are determined along the orientation of way.
     */
    private static class WayInPath {
        public final Way way;
        public boolean insideToTheLeft;

        public WayInPath(Way way, boolean insideLeft) {
            this.way = way;
            this.insideToTheLeft = insideLeft;
        }

        @Override
        public int hashCode() {
            return way.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof WayInPath))
                return false;
            WayInPath otherMember = (WayInPath) other;
            return otherMember.way.equals(this.way) && otherMember.insideToTheLeft == this.insideToTheLeft;
        }
    }

    // Adds the menu entry, Shortcuts, etc.
    public JoinAreasAction() {
        super(tr("Join overlapping Areas"), "joinareas", tr("Joins areas that overlap each other"), Shortcut.registerShortcut("tools:joinareas", tr("Tool: {0}", tr("Join overlapping Areas")),
                KeyEvent.VK_J, Shortcut.GROUP_EDIT, Shortcut.SHIFT_DEFAULT), true);
    }

    /**
     * Gets called whenever the shortcut is pressed or the menu entry is selected
     * Checks whether the selected objects are suitable to join and joins them if so
     */
    public void actionPerformed(ActionEvent e) {
        LinkedList<Way> ways = new LinkedList<Way>(Main.main.getCurrentDataSet().getSelectedWays());

        if (ways.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one closed way that should be joined."));
            return;
        }

        // Too many ways
        if(ways.size() > 2) {
            JOptionPane.showMessageDialog(Main.parent, tr("Only up to two areas can be joined at the moment."));
            return;
        }

        List<Node> allNodes = new ArrayList<Node>();
        for (Way way: ways) {
            if(!way.isClosed()) {
                JOptionPane.showMessageDialog(Main.parent, tr("\"{0}\" is not closed and therefore cannot be joined.", way.getName()));
                return;
            }

            allNodes.addAll(way.getNodes());
        }

        // TODO: Only display this warning when nodes outside dataSourceArea are deleted
        Area dataSourceArea = Main.main.getCurrentDataSet().getDataSourceArea();
        if (dataSourceArea != null) {
            for (Node node: allNodes) {
                if (!dataSourceArea.contains(node.getCoor())) {
                    int option = JOptionPane.showConfirmDialog(Main.parent,
                            trn("The selected way has nodes outside of the downloaded data region.",
                                    "The selected ways have nodes outside of the downloaded data region.",
                                    ways.size()) + "\n"
                                    + tr("This can lead to nodes being deleted accidentally.") + "\n"
                                    + tr("Are you really sure to continue?"),
                                    tr("Please abort if you are not sure"), JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE);

                    if (option != JOptionPane.YES_OPTION) return;
                    break;
                }
            }
        }

        if (checkForTagConflicts(ways.getFirst(), ways.getLast()))
            //there was conflicts and user canceled abort the action.
            return;


        JoinAreasResult result = joinAreas(ways.getFirst(), ways.getLast());

        if (result.hasChanges) {
            Main.map.mapView.repaint();
            DataSet ds = Main.main.getCurrentDataSet();
            ds.fireSelectionChanged();
        } else {
            JOptionPane.showMessageDialog(Main.parent, tr("No intersection found. Nothing was changed."));
        }
    }

    /**
     * Will join two overlapping areas
     * @param Way First way/area
     * @param Way Second way/area
     */
    private JoinAreasResult joinAreas(Way a, Way b) {

        JoinAreasResult result = new JoinAreasResult();
        result.hasChanges = false;

        // Fix self-overlapping first or other errors
        boolean same = a.equals(b);
        if(!same) {
            int i = 0;

            //join each area with itself, fixing self-crossings.
            JoinAreasResult resultA = joinAreas(a, a);
            JoinAreasResult resultB = joinAreas(b, b);

            if (resultA.mergeSuccessful) {
                a = resultA.outerWay;
                ++i;
            }
            if(resultB.mergeSuccessful) {
                b = resultB.outerWay;
                ++i;
            }

            result.hasChanges = i > 0;
            cmdsCount = i;
        }

        ArrayList<Node> nodes = addIntersections(a, b);

        //no intersections, return.
        if(nodes.size() == 0) return result;
        commitCommands(marktr("Added node on all intersections"));

        // Remove ways from all relations so ways can be combined/split quietly
        ArrayList<RelationRole> relations = removeFromRelations(a);
        if(!same) {
            relations.addAll(removeFromRelations(b));
        }

        // Don't warn now, because it will really look corrupted
        boolean warnAboutRelations = relations.size() > 0;

        ArrayList<Way> allWays = splitWaysOnNodes(a, b, nodes);

        // Find inner ways save them to a list
        ArrayList<WayInPath> outerWays = findOuterWays(allWays);
        ArrayList<Way> innerWays = findInnerWays(allWays, outerWays);

        // Join outer ways
        Way outerWay = joinOuterWays(outerWays);

        // Fix Multipolygons if there are any
        List<Way> newInnerWays = fixMultipolygons(innerWays, outerWay, same);

        // Delete the remaining inner ways
        if(innerWays != null && innerWays.size() > 0) {
            cmds.add(DeleteCommand.delete(Main.map.mapView.getEditLayer(), innerWays, true));
        }
        commitCommands(marktr("Delete Ways that are not part of an inner multipolygon"));

        // We can attach our new multipolygon relation and pretend it has always been there
        addOwnMultigonRelation(newInnerWays, outerWay, relations);
        fixRelations(relations, outerWay);
        commitCommands(marktr("Fix relations"));

        stripTags(newInnerWays);

        makeCommitsOneAction(
                same
                ? marktr("Joined self-overlapping area")
                        : marktr("Joined overlapping areas")
        );

        if(warnAboutRelations) {
            JOptionPane.showMessageDialog(Main.parent, tr("Some of the ways were part of relations that have been modified. Please verify no errors have been introduced."));
        }

        result.hasChanges = true;
        result.mergeSuccessful = true;
        result.outerWay = outerWay;
        result.innerWays = newInnerWays;

        return result;
    }

    /**
     * Checks if tags of two given ways differ, and presents the user a dialog to solve conflicts
     * @param Way First way to check
     * @param Way Second Way to check
     * @return boolean True if not all conflicts could be resolved, False if everything's fine
     */
    private boolean checkForTagConflicts(Way a, Way b) {
        ArrayList<Way> ways = new ArrayList<Way>();
        ways.add(a);
        ways.add(b);

        // FIXME: This is mostly copied and pasted from CombineWayAction.java and one day should be moved into tools
        // We have TagCollection handling for that now - use it here as well
        Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
        for (Way w : ways) {
            for (String key: w.keySet()) {
                if (!props.containsKey(key)) {
                    props.put(key, new TreeSet<String>());
                }
                props.get(key).add(w.get(key));
            }
        }

        Way ax = new Way(a);
        Way bx = new Way(b);

        Map<String, JComboBox> components = new HashMap<String, JComboBox>();
        JPanel p = new JPanel(new GridBagLayout());
        for (Entry<String, Set<String>> e : props.entrySet()) {
            if (TigerUtils.isTigerTag(e.getKey())) {
                String combined = TigerUtils.combineTags(e.getKey(), e.getValue());
                ax.put(e.getKey(), combined);
                bx.put(e.getKey(), combined);
            } else if (e.getValue().size() > 1) {
                if("created_by".equals(e.getKey()))
                {
                    ax.remove("created_by");
                    bx.remove("created_by");
                } else {
                    JComboBox c = new JComboBox(e.getValue().toArray());
                    c.setEditable(true);
                    p.add(new JLabel(e.getKey()), GBC.std());
                    p.add(Box.createHorizontalStrut(10), GBC.std());
                    p.add(c, GBC.eol());
                    components.put(e.getKey(), c);
                }
            } else {
                String val = e.getValue().iterator().next();
                ax.put(e.getKey(), val);
                bx.put(e.getKey(), val);
            }
        }

        if (components.isEmpty())
            return false; // No conflicts found

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Enter values for all conflicts."),
                new String[] {tr("Solve Conflicts"), tr("Cancel")});
        ed.setButtonIcons(new String[] {"dialogs/conflict.png", "cancel.png"});
        ed.setContent(p);
        ed.showDialog();

        if (ed.getValue() != 1) return true; // user cancel, unresolvable conflicts

        for (Entry<String, JComboBox> e : components.entrySet()) {
            String val = e.getValue().getEditor().getItem().toString();
            ax.put(e.getKey(), val);
            bx.put(e.getKey(), val);
        }

        cmds.add(new ChangeCommand(a, ax));
        cmds.add(new ChangeCommand(b, bx));
        commitCommands(marktr("Fix tag conflicts"));
        return false;
    }

    /**
     * Will find all intersection and add nodes there for two given ways
     * @param Way First way
     * @param Way Second way
     * @return ArrayList<OsmPrimitive> List of new nodes
     */
    private ArrayList<Node> addIntersections(Way a, Way b) {
        boolean same = a.equals(b);
        int nodesSizeA = a.getNodesCount();
        int nodesSizeB = b.getNodesCount();

        ArrayList<Node> nodes = new ArrayList<Node>();
        ArrayList<NodeToSegs> nodesA = new ArrayList<NodeToSegs>();
        ArrayList<NodeToSegs> nodesB = new ArrayList<NodeToSegs>();

        for (int i = (same ? 1 : 0); i < nodesSizeA - 1; i++) {
            for (int j = (same ? i + 2 : 0); j < nodesSizeB - 1; j++) {
                // Avoid re-adding nodes that already exist on (some) intersections
                if(a.getNode(i).equals(b.getNode(j)) || a.getNode(i+1).equals(b.getNode(j)))   {
                    nodes.add(b.getNode(j));
                    continue;
                } else
                    if(a.getNode(i).equals(b.getNode(j+1)) || a.getNode(i+1).equals(b.getNode(j+1))) {
                        nodes.add(b.getNode(j+1));
                        continue;
                    }
                LatLon intersection = getLineLineIntersection(
                        a.getNode(i)  .getEastNorth().east(), a.getNode(i)  .getEastNorth().north(),
                        a.getNode(i+1).getEastNorth().east(), a.getNode(i+1).getEastNorth().north(),
                        b.getNode(j)  .getEastNorth().east(), b.getNode(j)  .getEastNorth().north(),
                        b.getNode(j+1).getEastNorth().east(), b.getNode(j+1).getEastNorth().north());
                if(intersection == null) {
                    continue;
                }

                // Create the node. Adding them to the ways must be delayed because we still loop over them
                Node n = new Node(intersection);
                cmds.add(new AddCommand(n));
                nodes.add(n);
                // The distance is needed to sort and add the nodes in direction of the way
                nodesA.add(new NodeToSegs(i,  n, a.getNode(i).getCoor()));
                if(same) {
                    nodesA.add(new NodeToSegs(j,  n, a.getNode(j).getCoor()));
                } else {
                    nodesB.add(new NodeToSegs(j,  n, b.getNode(j).getCoor()));
                }
            }
        }

        addNodesToWay(a, nodesA);
        if(!same) {
            addNodesToWay(b, nodesB);
        }

        return nodes;
    }

    /**
     * Finds the intersection of two lines
     * @return LatLon null if no intersection was found, the LatLon coordinates of the intersection otherwise
     */
    static private LatLon getLineLineIntersection(
            double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4) {

        if (!Line2D.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) return null;

        // Convert line from (point, point) form to ax+by=c
        double a1 = y2 - y1;
        double b1 = x1 - x2;
        double c1 = x2*y1 - x1*y2;

        double a2 = y4 - y3;
        double b2 = x3 - x4;
        double c2 = x4*y3 - x3*y4;

        // Solve the equations
        double det = a1*b2 - a2*b1;
        if(det == 0) return null; // Lines are parallel

        return Main.proj.eastNorth2latlon(new EastNorth(
                (b1*c2 - b2*c1)/det,
                (a2*c1 -a1*c2)/det
        ));
    }

    /**
     * Inserts given nodes with positions into the given ways
     * @param Way The way to insert the nodes into
     * @param Collection<NodeToSegs> The list of nodes with positions to insert
     */
    private void addNodesToWay(Way a, ArrayList<NodeToSegs> nodes) {
        if(nodes.size() == 0)
            return;
        Way ax=new Way(a);
        Collections.sort(nodes);

        int numOfAdds = 1;
        for(NodeToSegs n : nodes) {
            ax.addNode(n.pos + numOfAdds, n.n);
            numOfAdds++;
        }

        cmds.add(new ChangeCommand(a, ax));
    }

    /**
     * Commits the command list with a description
     * @param String The description of what the commands do
     */
    private void commitCommands(String description) {
        switch(cmds.size()) {
        case 0:
            return;
        case 1:
            Main.main.undoRedo.add(cmds.getFirst());
            break;
        default:
            Command c = new SequenceCommand(tr(description), cmds);
            Main.main.undoRedo.add(c);
            break;
        }

        cmds.clear();
        cmdsCount++;
    }

    /**
     * Removes a given OsmPrimitive from all relations
     * @param OsmPrimitive Element to remove from all relations
     * @return ArrayList<RelationRole> List of relations with roles the primitives was part of
     */
    private ArrayList<RelationRole> removeFromRelations(OsmPrimitive osm) {
        ArrayList<RelationRole> result = new ArrayList<RelationRole>();
        for (Relation r : Main.main.getCurrentDataSet().getRelations()) {
            if (r.isDeleted()) {
                continue;
            }
            for (RelationMember rm : r.getMembers()) {
                if (rm.getMember() != osm) {
                    continue;
                }

                Relation newRel = new Relation(r);
                List<RelationMember> members = newRel.getMembers();
                members.remove(rm);
                newRel.setMembers(members);

                cmds.add(new ChangeCommand(r, newRel));
                RelationRole saverel =  new RelationRole(r, rm.getRole());
                if(!result.contains(saverel)) {
                    result.add(saverel);
                }
                break;
            }
        }

        commitCommands(marktr("Removed Element from Relations"));
        return result;
    }

    /**
     * This method splits ways into smaller parts, using the prepared nodes list as split points.
     * Uses SplitWayAction.splitWay for the heavy lifting.
     * @return list of split ways (or original ways if no splitting is done).
     */
    private ArrayList<Way> splitWaysOnNodes(Way a, Way b, Collection<Node> nodes) {

        ArrayList<Way> result = new ArrayList<Way>();
        List<Way> ways = new ArrayList<Way>();
        ways.add(a);
        ways.add(b);

        for (Way way: ways) {
            List<List<Node>> chunks = buildNodeChunks(way, nodes);
            SplitWayResult split = SplitWayAction.splitWay(Main.map.mapView.getEditLayer(), way, chunks, Collections.<OsmPrimitive>emptyList());

            //execute the command, we need the results
            Main.main.undoRedo.add(split.getCommand());
            cmdsCount ++;

            result.add(split.getOriginalWay());
            result.addAll(split.getNewWays());
        }

        return result;
    }

    /**
     * Simple chunking version. Does not care about circular ways and result being proper, we will glue it all back together later on.
     * @param way the way to chunk
     * @param splitNodes the places where to cut.
     * @return list of node segments to produce.
     */
    private List<List<Node>> buildNodeChunks(Way way, Collection<Node> splitNodes)
    {
        List<List<Node>> result = new ArrayList<List<Node>>();
        List<Node> curList = new ArrayList<Node>();

        for(Node node: way.getNodes()){
            curList.add(node);
            if (curList.size() > 1 && splitNodes.contains(node)){
                result.add(curList);
                curList = new ArrayList<Node>();
                curList.add(node);
            }
        }

        if (curList.size() > 1)
        {
            result.add(curList);
        }

        return result;
    }

    /**
     * Returns all nodes for given ways
     * @param Collection<Way> The list of ways which nodes are to be returned
     * @return Collection<Node> The list of nodes the ways contain
     */
    private Collection<Node> getNodesFromWays(Collection<Way> ways) {
        Collection<Node> allNodes = new ArrayList<Node>();
        for(Way w: ways) {
            allNodes.addAll(w.getNodes());
        }
        return allNodes;
    }

    /**
     * Gets all inner ways given all ways and outer ways.
     * @param multigonWays
     * @param outerWays
     * @return list of inner ways.
     */
    private ArrayList<Way> findInnerWays(Collection<Way> multigonWays, Collection<WayInPath> outerWays) {
        ArrayList<Way> innerWays = new ArrayList<Way>();
        Set<Way> outerSet = new HashSet<Way>();

        for(WayInPath w: outerWays) {
            outerSet.add(w.way);
        }

        for(Way way: multigonWays) {
            if (!outerSet.contains(way)) {
                innerWays.add(way);
            }
        }

        return innerWays;
    }


    /**
     * Finds all ways for a given list of Ways that form the outer hull.
     * This works by starting with one node and traversing the multigon clockwise, always picking the leftmost path.
     * Prerequisites - the ways must not intersect and have common end nodes where they meet.
     * @param Collection<Way> A list of (splitted) ways that form a multigon
     * @return Collection<Way> A list of ways that form the outer boundary of the multigon.
     */
    private static ArrayList<WayInPath> findOuterWays(Collection<Way> multigonWays) {

        //find the node with minimum lat - it's guaranteed to be outer. (What about the south pole?)
        Way bestWay = null;
        Node topNode = null;
        int topIndex = 0;
        double minLat = Double.POSITIVE_INFINITY;

        for(Way way: multigonWays) {
            for (int pos = 0; pos < way.getNodesCount(); pos ++) {
                Node node = way.getNode(pos);

                if (node.getCoor().lat() < minLat) {
                    minLat = node.getCoor().lat();
                    bestWay = way;
                    topNode = node;
                    topIndex = pos;
                }
            }
        }

        //get two final nodes from best way to mark as starting point and orientation.
        Node headNode = null;
        Node prevNode = null;

        if (topNode.equals(bestWay.firstNode()) || topNode.equals(bestWay.lastNode())) {
            //node is in split point
            headNode = topNode;
            //make a fake node that is downwards from head node (smaller latitude). It will be a division point between paths.
            prevNode = new Node(new LatLon(headNode.getCoor().lat() - 1000, headNode.getCoor().lon()));
        } else {
            //node is inside way - pick the clockwise going end.
            Node prev = bestWay.getNode(topIndex - 1);
            Node next = bestWay.getNode(topIndex + 1);

            if (angleIsClockwise(prev, topNode, next)) {
                headNode = bestWay.lastNode();
                prevNode = bestWay.getNode(bestWay.getNodesCount() - 2);
            }
            else {
                headNode = bestWay.firstNode();
                prevNode = bestWay.getNode(1);
            }
        }

        Set<Way> outerWays = new HashSet<Way>();
        ArrayList<WayInPath> result = new ArrayList<WayInPath>();

        //iterate till full circle is reached
        while (true) {

            bestWay = null;
            Node bestWayNextNode = null;
            boolean bestWayReverse = false;

            for (Way way: multigonWays) {
                boolean wayReverse;
                Node nextNode;

                if (way.firstNode().equals(headNode)) {
                    //start adjacent to headNode
                    nextNode = way.getNode(1);
                    wayReverse = false;

                    if (nextNode.equals(prevNode)) {
                        //this is the path we came from - ignore it.
                    } else if (bestWay == null || !isToTheRightSideOfLine(prevNode, headNode, bestWayNextNode, nextNode)) {
                        //the new way is better
                        bestWay = way;
                        bestWayReverse = wayReverse;
                        bestWayNextNode = nextNode;
                    }
                }

                if (way.lastNode().equals(headNode)) {
                    //end adjacent to headNode
                    nextNode = way.getNode(way.getNodesCount() - 2);
                    wayReverse = true;

                    if (nextNode.equals(prevNode)) {
                        //this is the path we came from - ignore it.
                    } else if (bestWay == null || !isToTheRightSideOfLine(prevNode, headNode, bestWayNextNode, nextNode)) {
                        //the new way is better
                        bestWay = way;
                        bestWayReverse = wayReverse;
                        bestWayNextNode = nextNode;
                    }
                }
            }

            if (bestWay == null)
                throw new RuntimeException();
            else if (outerWays.contains(bestWay)) {
                break; //full circle reached, terminate.
            } else {
                //add to outer ways, repeat.
                outerWays.add(bestWay);
                result.add(new WayInPath(bestWay, bestWayReverse));
                headNode = bestWayReverse ? bestWay.firstNode() : bestWay.lastNode();
                prevNode = bestWayReverse ? bestWay.getNode(1) : bestWay.getNode(bestWay.getNodesCount() - 2);
            }
        }

        return result;
    }

    /**
     * Tests if given point is to the right side of path consisting of 3 points.
     * @param lineP1 first point in path
     * @param lineP2 second point in path
     * @param lineP3 third point in path
     * @param testPoint
     * @return true if to the right side, false otherwise
     */
    public static boolean isToTheRightSideOfLine(Node lineP1, Node lineP2, Node lineP3, Node testPoint)
    {
        boolean pathBendToRight = angleIsClockwise(lineP1, lineP2, lineP3);
        boolean rightOfSeg1 = angleIsClockwise(lineP1, lineP2, testPoint);
        boolean rightOfSeg2 = angleIsClockwise(lineP2, lineP3, testPoint);

        if (pathBendToRight)
            return rightOfSeg1 && rightOfSeg2;
        else
            return !(!rightOfSeg1 && !rightOfSeg2);
    }

    /**
     * This method tests if secondNode is clockwise to first node.
     * @param commonNode starting point for both vectors
     * @param firstNode first vector end node
     * @param secondNode second vector end node
     * @return true if first vector is clockwise before second vector.
     */
    public static boolean angleIsClockwise(Node commonNode, Node firstNode, Node secondNode)
    {
        double dla1 = (firstNode.getCoor().lat() - commonNode.getCoor().lat());
        double dla2 = (secondNode.getCoor().lat() - commonNode.getCoor().lat());
        double dlo1 = (firstNode.getCoor().lon() - commonNode.getCoor().lon());
        double dlo2 = (secondNode.getCoor().lon() - commonNode.getCoor().lon());

        return dla1 * dlo2 - dlo1 * dla2 > 0;
    }

    /**
     * Tests if point is inside a polygon. The polygon can be self-intersecting. In such case the contains function works in xor-like manner.
     * @param polygonNodes list of nodes from polygon path.
     * @param point the point to test
     * @return true if the point is inside polygon.
     * FIXME: this should probably be moved to tools..
     */
    public static boolean nodeInsidePolygon(ArrayList<Node> polygonNodes, Node point)
    {
        if (polygonNodes.size() < 3)
            return false;

        boolean inside = false;
        Node p1, p2;

        //iterate each side of the polygon, start with the last segment
        Node oldPoint = polygonNodes.get(polygonNodes.size() - 1);

        for(Node newPoint: polygonNodes)
        {
            //skip duplicate points
            if (newPoint.equals(oldPoint)) {
                continue;
            }

            //order points so p1.lat <= p2.lat;
            if (newPoint.getCoor().lat() > oldPoint.getCoor().lat())
            {
                p1 = oldPoint;
                p2 = newPoint;
            }
            else
            {
                p1 = newPoint;
                p2 = oldPoint;
            }

            //test if the line is crossed and if so invert the inside flag.
            if ((newPoint.getCoor().lat() < point.getCoor().lat()) == (point.getCoor().lat() <= oldPoint.getCoor().lat())
                    && (point.getCoor().lon() - p1.getCoor().lon()) * (p2.getCoor().lat() - p1.getCoor().lat())
                    < (p2.getCoor().lon() - p1.getCoor().lon()) * (point.getCoor().lat() - p1.getCoor().lat()))
            {
                inside = !inside;
            }

            oldPoint = newPoint;
        }

        return inside;
    }

    /**
     * Joins the outer ways and deletes all short ways that can't be part of a multipolygon anyway.
     * @param Collection<Way> The list of outer ways that belong to that multigon.
     * @return Way The newly created outer way
     */
    private Way joinOuterWays(ArrayList<WayInPath> outerWays) {

        //leave original orientation, if all paths are reverse.
        boolean allReverse = true;
        for(WayInPath way: outerWays) {
            allReverse &= way.insideToTheLeft;
        }

        if (allReverse) {
            for(WayInPath way: outerWays){
                way.insideToTheLeft = !way.insideToTheLeft;
            }
        }

        commitCommands(marktr("Join Areas: Remove Short Ways"));
        Way joinedWay = joinOrientedWays(outerWays);
        if (joinedWay != null)
            return closeWay(joinedWay);
        else
            return null;
    }

    /**
     * Ensures a way is closed. If it isn't, last and first node are connected.
     * @param Way the way to ensure it's closed
     * @return Way The joined way.
     */
    private Way closeWay(Way w) {
        if(w.isClosed())
            return w;
        Main.main.getCurrentDataSet().setSelected(w);
        Way wnew = new Way(w);
        wnew.addNode(wnew.firstNode());
        cmds.add(new ChangeCommand(w, wnew));
        commitCommands(marktr("Closed Way"));
        return (Way)(Main.main.getCurrentDataSet().getSelectedWays().toArray())[0];
    }

    /**
     * Joins a list of ways (using CombineWayAction and ReverseWayAction as specified in WayInPath)
     * @param ArrayList<Way> The list of ways to join and reverse
     * @return Way The newly created way
     */
    private Way joinOrientedWays(ArrayList<WayInPath> ways) {
        if(ways.size() < 2)
            return ways.get(0).way;

        // This will turn ways so all of them point in the same direction and CombineAction won't bug
        // the user about this.

        List<Way> actionWays = new ArrayList<Way>(ways.size());

        for(WayInPath way : ways) {
            actionWays.add(way.way);

            if (way.insideToTheLeft) {
                Main.main.getCurrentDataSet().setSelected(way.way);
                new ReverseWayAction().actionPerformed(null);
                cmdsCount++;
            }
        }

        Way result = new CombineWayAction().combineWays(actionWays);

        if(result != null) {
            cmdsCount++;
        }
        return result;
    }

    /**
     * Joins a list of ways (using CombineWayAction and ReverseWayAction if necessary to quiet the former)
     * @param ArrayList<Way> The list of ways to join
     * @return Way The newly created way
     */
    private Way joinWays(ArrayList<Way> ways) {
        if(ways.size() < 2)
            return ways.get(0);

        // This will turn ways so all of them point in the same direction and CombineAction won't bug
        // the user about this.
        Way a = null;
        for (Way b : ways) {
            if(a == null) {
                a = b;
                continue;
            }
            if(a.getNode(0).equals(b.getNode(0)) ||
                    a.getNode(a.getNodesCount()-1).equals(b.getNode(b.getNodesCount()-1))) {
                Main.main.getCurrentDataSet().setSelected(b);
                new ReverseWayAction().actionPerformed(null);
                cmdsCount++;
            }
            a = b;
        }
        if ((a = new CombineWayAction().combineWays(ways)) != null) {
            cmdsCount++;
        }
        return a;
    }

    /**
     * Finds all ways that may be part of a multipolygon relation and removes them from the given list.
     * It will automatically combine "good" ways
     * @param Collection<Way> The list of inner ways to check
     * @param Way The newly created outer way
     * @return ArrayList<Way> The List of newly created inner ways
     */
    private ArrayList<Way> fixMultipolygons(Collection<Way> uninterestingWays, Way outerWay, boolean selfintersect) {
        Collection<Node> innerNodes = getNodesFromWays(uninterestingWays);
        Collection<Node> outerNodes = outerWay.getNodes();

        // The newly created inner ways. uninterestingWays is passed by reference and therefore modified in-place
        ArrayList<Way> newInnerWays = new ArrayList<Way>();

        // Now we need to find all inner ways that contain a remaining node, but no outer nodes
        // Remaining nodes are those that contain to more than one way. All nodes that belong to an
        // inner multigon part will have at least two ways, so we can use this to find which ways do
        // belong to the multigon.
        ArrayList<Way> possibleWays = new ArrayList<Way>();
        wayIterator: for(Way w : uninterestingWays) {
            boolean hasInnerNodes = false;
            for(Node n : w.getNodes()) {
                if(outerNodes.contains(n)) {
                    if(!selfintersect) { // allow outer point for self intersection
                        continue wayIterator;
                    }
                }
                else if(!hasInnerNodes && innerNodes.contains(n)) {
                    hasInnerNodes = true;
                }
            }
            if(!hasInnerNodes || w.getNodesCount() < 2) {
                continue;
            }
            possibleWays.add(w);
        }

        // This removes unnecessary ways that might have been added.
        removeAlmostAlikeWays(possibleWays);

        // loop twice
        // in k == 0 prefer ways which allow no Y-joining (i.e. which have only 1 solution)
        for(int k = 0; k < 2; ++k)
        {
            // Join all ways that have one start/ending node in common
            Way joined = null;
            outerIterator: do {
                removePartlyUnconnectedWays(possibleWays);
                joined = null;
                for(Way w1 : possibleWays) {
                    if(w1.isClosed()) {
                        if(!wayIsCollapsed(w1)) {
                            uninterestingWays.remove(w1);
                            newInnerWays.add(w1);
                        }
                        joined = w1;
                        possibleWays.remove(w1);
                        continue outerIterator;
                    }
                    ArrayList<Way> secondary = new ArrayList<Way>();
                    for(Way w2 : possibleWays) {
                        int i = 0;
                        // w2 cannot be closed, otherwise it would have been removed above
                        if(w1.equals(w2)) {
                            continue;
                        }
                        if(w2.isFirstLastNode(w1.firstNode())) {
                            ++i;
                        }
                        if(w2.isFirstLastNode(w1.lastNode())) {
                            ++i;
                        }
                        if(i == 2) // this way closes w1 - take it!
                        {
                            if(secondary.size() > 0) {
                                secondary.clear();
                            }
                            secondary.add(w2);
                            break;
                        }
                        else if(i > 0) {
                            secondary.add(w2);
                        }
                    }
                    if(k == 0 ? secondary.size() == 1 : secondary.size() > 0)
                    {
                        ArrayList<Way> joinThem = new ArrayList<Way>();
                        joinThem.add(w1);
                        joinThem.add(secondary.get(0));
                        // Although we joined the ways, we cannot simply assume that they are closed
                        if((joined = joinWays(joinThem)) != null)
                        {
                            uninterestingWays.removeAll(joinThem);
                            possibleWays.removeAll(joinThem);

                            //List<Node> nodes = joined.getNodes();
                            // check if we added too much
                            /*for(int i = 1; i < nodes.size()-2; ++i)
                            {
                                if(nodes.get(i) == nodes.get(nodes.size()-1))
                                    System.out.println("Joining of ways produced unexpecteded result\n");
                            }*/
                            uninterestingWays.add(joined);
                            possibleWays.add(joined);
                            continue outerIterator;
                        }
                    }
                }
            } while(joined != null);
        }
        return newInnerWays;
    }

    /**
     * Removes almost alike ways (= ways that are on top of each other for all nodes)
     * @param ArrayList<Way> the ways to remove almost-duplicates from
     */
    private void removeAlmostAlikeWays(ArrayList<Way> ways) {
        Collection<Way> removables = new ArrayList<Way>();
        outer: for(int i=0; i < ways.size(); i++) {
            Way a = ways.get(i);
            for(int j=i+1; j < ways.size(); j++) {
                Way b = ways.get(j);
                List<Node> revNodes = new ArrayList<Node>(b.getNodes());
                Collections.reverse(revNodes);
                if(a.getNodes().equals(b.getNodes()) || a.getNodes().equals(revNodes)) {
                    removables.add(a);
                    continue outer;
                }
            }
        }
        ways.removeAll(removables);
    }

    /**
     * Removes ways from the given list whose starting or ending node doesn't
     * connect to other ways from the same list (it's like removing spikes).
     * @param ArrayList<Way> The list of ways to remove "spikes" from
     */
    private void removePartlyUnconnectedWays(ArrayList<Way> ways) {
        List<Way> removables = new ArrayList<Way>();
        for(Way a : ways) {
            if(a.isClosed()) {
                continue;
            }
            boolean connectedStart = false;
            boolean connectedEnd = false;
            for(Way b : ways) {
                if(a.equals(b)) {
                    continue;
                }
                if(b.isFirstLastNode(a.firstNode())) {
                    connectedStart = true;
                }
                if(b.isFirstLastNode(a.lastNode())) {
                    connectedEnd = true;
                }
            }
            if(!connectedStart || !connectedEnd) {
                removables.add(a);
            }
        }
        ways.removeAll(removables);
    }

    /**
     * Checks if a way is collapsed (i.e. looks like <---->)
     * @param Way A *closed* way to check if it is collapsed
     * @return boolean If the closed way is collapsed or not
     */
    private boolean wayIsCollapsed(Way w) {
        if(w.getNodesCount() <= 3) return true;

        // If a way contains more than one node twice, it must be collapsed (only start/end node may be the same)
        Way x = new Way(w);
        int count = 0;
        for(Node n : w.getNodes()) {
            x.removeNode(n);
            if(x.containsNode(n)) {
                count++;
            }
            if(count == 2) return true;
        }
        return false;
    }

    /**
     * Will add own multipolygon relation to the "previously existing" relations. Fixup is done by fixRelations
     * @param Collection<Way> List of already closed inner ways
     * @param Way The outer way
     * @param ArrayList<RelationRole> The list of relation with roles to add own relation to
     */
    private void addOwnMultigonRelation(Collection<Way> inner, Way outer, ArrayList<RelationRole> rels) {
        if(inner.size() == 0) return;
        // Create new multipolygon relation and add all inner ways to it
        Relation newRel = new Relation();
        newRel.put("type", "multipolygon");
        for(Way w : inner) {
            newRel.addMember(new RelationMember("inner", w));
        }
        cmds.add(new AddCommand(newRel));

        // We don't add outer to the relation because it will be handed to fixRelations()
        // which will then do the remaining work. Collections are passed by reference, so no
        // need to return it
        rels.add(new RelationRole(newRel, "outer"));
        //return rels;
    }

    /**
     * Adds the previously removed relations again to the outer way. If there are multiple multipolygon
     * relations where the joined areas were in "outer" role a new relation is created instead with all
     * members of both. This function depends on multigon relations to be valid already, it won't fix them.
     * @param ArrayList<RelationRole> List of relations with roles the (original) ways were part of
     * @param Way The newly created outer area/way
     */
    private void fixRelations(ArrayList<RelationRole> rels, Way outer) {
        ArrayList<RelationRole> multiouters = new ArrayList<RelationRole>();
        for(RelationRole r : rels) {
            if( r.rel.get("type") != null &&
                    r.rel.get("type").equalsIgnoreCase("multipolygon") &&
                    r.role.equalsIgnoreCase("outer")
            ) {
                multiouters.add(r);
                continue;
            }
            // Add it back!
            Relation newRel = new Relation(r.rel);
            newRel.addMember(new RelationMember(r.role, outer));
            cmds.add(new ChangeCommand(r.rel, newRel));
        }

        Relation newRel = null;
        switch(multiouters.size()) {
        case 0:
            return;
        case 1:
            // Found only one to be part of a multipolygon relation, so just add it back as well
            newRel = new Relation(multiouters.get(0).rel);
            newRel.addMember(new RelationMember(multiouters.get(0).role, outer));
            cmds.add(new ChangeCommand(multiouters.get(0).rel, newRel));
            return;
        default:
            // Create a new relation with all previous members and (Way)outer as outer.
            newRel = new Relation();
            for(RelationRole r : multiouters) {
                // Add members
                for(RelationMember rm : r.rel.getMembers())
                    if(!newRel.getMembers().contains(rm)) {
                        newRel.addMember(rm);
                    }
                // Add tags
                for (String key : r.rel.keySet()) {
                    newRel.put(key, r.rel.get(key));
                }
                // Delete old relation
                cmds.add(new DeleteCommand(r.rel));
            }
            newRel.addMember(new RelationMember("outer", outer));
            cmds.add(new AddCommand(newRel));
        }
    }

    /**
     * @param Collection<Way> The List of Ways to remove all tags from
     */
    private void stripTags(Collection<Way> ways) {
        for(Way w: ways) {
            stripTags(w);
        }
        commitCommands(marktr("Remove tags from inner ways"));
    }

    /**
     * @param Way The Way to remove all tags from
     */
    private void stripTags(Way x) {
        if(x.getKeys() == null) return;
        Way y = new Way(x);
        for (String key : x.keySet()) {
            y.remove(key);
        }
        cmds.add(new ChangeCommand(x, y));
    }

    /**
     * Takes the last cmdsCount actions back and combines them into a single action
     * (for when the user wants to undo the join action)
     * @param String The commit message to display
     */
    private void makeCommitsOneAction(String message) {
        UndoRedoHandler ur = Main.main.undoRedo;
        cmds.clear();
        int i = Math.max(ur.commands.size() - cmdsCount, 0);
        for(; i < ur.commands.size(); i++) {
            cmds.add(ur.commands.get(i));
        }

        for(i = 0; i < cmds.size(); i++) {
            ur.undo();
        }

        commitCommands(message == null ? marktr("Join Areas Function") : message);
        cmdsCount = 0;
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
