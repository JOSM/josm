// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TigerUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

public class JoinAreasAction extends JosmAction {
    // This will be used to commit commands and unite them into one large command sequence at the end
    private LinkedList<Command> cmds = new LinkedList<Command>();
    private int cmdsCount = 0;

    // HelperClass
    // Saves a node and two positions where to insert the node into the ways
    private class NodeToSegs implements Comparable<NodeToSegs> {
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
    }

    // HelperClass
    // Saves a relation and a role an OsmPrimitve was part of until it was stripped from all relations
    private class RelationRole {
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
        Collection<OsmPrimitive> selection = Main.main.getCurrentDataSet().getSelectedWays();

        int ways = 0;
        Way[] selWays = new Way[2];

        LinkedList<Bounds> bounds = new LinkedList<Bounds>();
        OsmDataLayer dataLayer = Main.map.mapView.getEditLayer();
        for (DataSource ds : dataLayer.data.dataSources) {
            if (ds.bounds != null) {
                bounds.add(ds.bounds);
            }
        }

        boolean askedAlready = false;
        for (OsmPrimitive prim : selection) {
            Way way = (Way) prim;

            // Too many ways
            if(ways == 2) {
                JOptionPane.showMessageDialog(Main.parent, tr("Only up to two areas can be joined at the moment."));
                return;
            }

            if(!way.isClosed()) {
                JOptionPane.showMessageDialog(Main.parent, tr("\"{0}\" is not closed and therefore can't be joined.", way.getName()));
                return;
            }

            // This is copied from SimplifyAction and should be probably ported to tools
            for (Node node : way.getNodes()) {
                if(askedAlready) {
                    break;
                }
                boolean isInsideOneBoundingBox = false;
                for (Bounds b : bounds) {
                    if (b.contains(node.getCoor())) {
                        isInsideOneBoundingBox = true;
                        break;
                    }
                }

                if (!isInsideOneBoundingBox) {
                    int option = JOptionPane.showConfirmDialog(Main.parent,
                            tr("The selected way(s) have nodes outside of the downloaded data region.\n"
                                    + "This can lead to nodes being deleted accidentally.\n"
                                    + "Are you really sure to continue?"),
                                    tr("Please abort if you are not sure"), JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE);

                    if (option != JOptionPane.YES_OPTION) return;
                    askedAlready = true;
                    break;
                }
            }

            selWays[ways] = way;
            ways++;
        }

        if (ways < 1) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one closed way the should be joined."));
            return;
        }

        if(joinAreas(selWays[0], selWays[ways == 2 ? 1 : 0])) {
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
     * @return boolean Whether to display the "no operation" message
     */
    private boolean joinAreas(Way a, Way b) {
        // Fix self-overlapping first or other errors
        boolean same = a.equals(b);
        boolean hadChanges = false;
        if(!same) {
            if(checkForTagConflicts(a, b)) return true; // User aborted, so don't warn again
            hadChanges = joinAreas(a, a);
            hadChanges = joinAreas(b, b) || hadChanges;
        }

        ArrayList<OsmPrimitive> nodes = addIntersections(a, b);
        if(nodes.size() == 0) return hadChanges;
        commitCommands(marktr("Added node on all intersections"));

        // Remove ways from all relations so ways can be combined/split quietly
        ArrayList<RelationRole> relations = removeFromRelations(a);
        if(!same) {
            relations.addAll(removeFromRelations(b));
        }

        // Don't warn now, because it will really look corrupted
        boolean warnAboutRelations = relations.size() > 0;

        Collection<Way> allWays = splitWaysOnNodes(a, b, nodes);

        // Find all nodes and inner ways save them to a list
        Collection<Node> allNodes = getNodesFromWays(allWays);
        Collection<Way> innerWays = findInnerWays(allWays, allNodes);

        // Join outer ways
        Way outerWay = joinOuterWays(allWays, innerWays);

        // Fix Multipolygons if there are any
        Collection<Way> newInnerWays = fixMultigons(innerWays, outerWay);

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

        return true;
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

        // This is mostly copied and pasted from CombineWayAction.java and one day should be moved into tools
        Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
        for (Way w : ways) {
            for (Entry<String,String> e : w.entrySet()) {
                if (!props.containsKey(e.getKey())) {
                    props.put(e.getKey(), new TreeSet<String>());
                }
                props.get(e.getKey()).add(e.getValue());
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
                    ax.put("created_by", "JOSM");
                    bx.put("created_by", "JOSM");
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
    private ArrayList<OsmPrimitive> addIntersections(Way a, Way b) {
        boolean same = a.equals(b);
        int nodesSizeA = a.getNodesCount();
        int nodesSizeB = b.getNodesCount();

        // We use OsmPrimitive here instead of Node because we later need to split a way at these nodes.
        // With OsmPrimitve we can simply add the way and don't have to loop over the nodes
        ArrayList<OsmPrimitive> nodes = new ArrayList<OsmPrimitive>();
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
            if (r.isDeleted())
                continue;
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
     * This is a hacky implementation to make use of the splitWayAction code and
     * should be improved. SplitWayAction needs to expose its splitWay function though.
     */
    private Collection<Way> splitWaysOnNodes(Way a, Way b, Collection<OsmPrimitive> nodes) {
        ArrayList<Way> ways = new ArrayList<Way>();
        ways.add(a);
        if(!a.equals(b)) {
            ways.add(b);
        }

        List<OsmPrimitive> affected = new ArrayList<OsmPrimitive>();
        for (Way way : ways) {
            nodes.add(way);
            Main.main.getCurrentDataSet().setSelected(nodes);
            nodes.remove(way);
            new SplitWayAction().actionPerformed(null);
            cmdsCount++;
            affected.addAll(Main.main.getCurrentDataSet().getSelectedWays());
        }
        return osmprim2way(affected);
    }

    /**
     * Converts a list of OsmPrimitives to a list of Ways
     * @param Collection<OsmPrimitive> The OsmPrimitives list that's needed as a list of Ways
     * @return Collection<Way> The list as list of Ways
     */
    static private Collection<Way> osmprim2way(Collection<OsmPrimitive> ways) {
        Collection<Way> result = new ArrayList<Way>();
        for(OsmPrimitive w: ways) {
            if(w instanceof Way) {
                result.add((Way) w);
            }
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
     * Finds all inner ways for a given list of Ways and Nodes from a multigon by constructing a polygon
     * for each way, looking for inner nodes that are not part of this way. If a node is found, all ways
     * containing this node are added to the list
     * @param Collection<Way> A list of (splitted) ways that form a multigon
     * @param Collection<Node> A list of nodes that belong to the multigon
     * @return Collection<Way> A list of ways that are positioned inside the outer borders of the multigon
     */
    private Collection<Way> findInnerWays(Collection<Way> multigonWays, Collection<Node> multigonNodes) {
        Collection<Way> innerWays = new ArrayList<Way>();
        for(Way w: multigonWays) {
            Polygon poly = new Polygon();
            for(Node n: (w).getNodes()) {
                poly.addPoint(latlonToXY(n.getCoor().lat()), latlonToXY(n.getCoor().lon()));
            }

            for(Node n: multigonNodes) {
                if(!(w).containsNode(n) && poly.contains(latlonToXY(n.getCoor().lat()), latlonToXY(n.getCoor().lon()))) {
                    getWaysByNode(innerWays, multigonWays, n);
                }
            }
        }

        return innerWays;
    }

    // Polygon only supports int coordinates, so convert them
    private int latlonToXY(double val) {
        return (int)Math.round(val*1000000);
    }

    /**
     * Finds all ways that contain the given node.
     * @param Collection<Way> A list to which matching ways will be added
     * @param Collection<Way> A list of ways to check
     * @param Node The node the ways should be checked against
     */
    private void getWaysByNode(Collection<Way> innerWays, Collection<Way> w, Node n) {
        for(Way way : w) {
            if(!(way).containsNode(n)) {
                continue;
            }
            if(!innerWays.contains(way)) {
                innerWays.add(way); // Will need this later for multigons
            }
        }
    }

    /**
     * Joins the two outer ways and deletes all short ways that can't be part of a multipolygon anyway
     * @param Collection<OsmPrimitive> The list of all ways that belong to that multigon
     * @param Collection<Way> The list of inner ways that belong to that multigon
     * @return Way The newly created outer way
     */
    private Way joinOuterWays(Collection<Way> multigonWays, Collection<Way> innerWays) {
        ArrayList<Way> join = new ArrayList<Way>();
        for(Way w: multigonWays) {
            // Skip inner ways
            if(innerWays.contains(w)) {
                continue;
            }

            if(w.getNodesCount() <= 2) {
                cmds.add(new DeleteCommand(w));
            } else {
                join.add(w);
            }
        }

        commitCommands(marktr("Join Areas: Remove Short Ways"));
        return closeWay(joinWays(join));
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
     * Joins a list of ways (using CombineWayAction and ReverseWayAction if necessary to quiet the former)
     * @param ArrayList<Way> The list of ways to join
     * @return Way The newly created way
     */
    private Way joinWays(ArrayList<Way> ways) {
        if(ways.size() < 2) return ways.get(0);

        // This will turn ways so all of them point in the same direction and CombineAction won't bug
        // the user about this.
        Way a = null;
        for(Way b : ways) {
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
        Main.main.getCurrentDataSet().setSelected(ways);
        // TODO: It might be possible that a confirmation dialog is presented even after reversing (for
        // "strange" ways). If the user cancels this, makeCommitsOneAction will wrongly consume a previous
        // action. Make CombineWayAction either silent or expose its combining capabilities.
        new CombineWayAction().actionPerformed(null);
        cmdsCount++;
        return (Way)(Main.main.getCurrentDataSet().getSelectedWays().toArray())[0];
    }

    /**
     * Finds all ways that may be part of a multipolygon relation and removes them from the given list.
     * It will automatically combine "good" ways
     * @param Collection<Way> The list of inner ways to check
     * @param Way The newly created outer way
     * @return ArrayList<Way> The List of newly created inner ways
     */
    private ArrayList<Way> fixMultigons(Collection<Way> uninterestingWays, Way outerWay) {
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
                    continue wayIterator;
                }
                if(!hasInnerNodes && innerNodes.contains(n)) {
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
        removePartlyUnconnectedWays(possibleWays);

        // Join all ways that have one start/ending node in common
        Way joined = null;
        outerIterator: do {
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
                for(Way w2 : possibleWays) {
                    // w2 cannot be closed, otherwise it would have been removed above
                    if(!waysCanBeCombined(w1, w2)) {
                        continue;
                    }

                    ArrayList<Way> joinThem = new ArrayList<Way>();
                    joinThem.add(w1);
                    joinThem.add(w2);
                    uninterestingWays.removeAll(joinThem);
                    possibleWays.removeAll(joinThem);

                    // Although we joined the ways, we cannot simply assume that they are closed
                    joined = joinWays(joinThem);
                    uninterestingWays.add(joined);
                    possibleWays.add(joined);
                    continue outerIterator;
                }
            }
        } while(joined != null);
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
     * Checks if two ways share one starting/ending node
     * @param Way first way
     * @param Way second way
     * @return boolean Wheter the ways share a starting/ending node or not
     */
    private boolean waysCanBeCombined(Way w1, Way w2) {
        if(w1.equals(w2)) return false;

        if(w1.getNode(0).equals(w2.getNode(0))) return true;
        if(w1.getNode(0).equals(w2.getNode(w2.getNodesCount()-1))) return true;

        if(w1.getNode(w1.getNodesCount()-1).equals(w2.getNode(0))) return true;
        if(w1.getNode(w1.getNodesCount()-1).equals(w2.getNode(w2.getNodesCount()-1))) return true;

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
