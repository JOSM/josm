// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.combineTigerTags;
import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.completeTagCollectionForEditing;
import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.normalizeTagCollectionBeforeEditing;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ReverseWayAction.ReverseWayResult;
import org.openstreetmap.josm.actions.SplitWayAction.SplitWayResult;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.corrector.UserCancelException;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.tools.Pair;
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

        public boolean mergeSuccessful;
        public boolean hasChanges;
        public boolean hasRelationProblems;

        public List<Multipolygon> polygons;
    }

    public static class Multipolygon {
        public Way outerWay;
        public List<Way> innerWays;

        public Relation relation;

        public Multipolygon(Way way) {
            outerWay = way;
            innerWays = new ArrayList<Way>();
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


    //HelperClass
    //saves a way and the "inside" side
    // insideToTheLeft: if true left side is "in", false -right side is "in". Left and right are determined along the orientation of way.
    private static class WayInPolygon {
        public final Way way;
        public boolean insideToTheRight;

        public WayInPolygon(Way _way, boolean _insideRight) {
            this.way = _way;
            this.insideToTheRight = _insideRight;
        }

        @Override
        public int hashCode() {
            return way.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof WayInPolygon)) return false;
            WayInPolygon otherMember = (WayInPolygon) other;
            return otherMember.way.equals(this.way) && otherMember.insideToTheRight == this.insideToTheRight;
        }
    }

    /**
     * This helper class describes a polygon, assembled from several ways.
     * @author viesturs
     *
     */
    private static class AssembledPolygon {
        public List<WayInPolygon> ways;

        public AssembledPolygon(List<WayInPolygon> boundary) {
            this.ways = boundary;
        }

        public List<Node> getNodes() {
            List<Node> nodes = new ArrayList<Node>();
            for (WayInPolygon way : this.ways) {
                //do not add the last node as it will be repeated in the next way
                if (way.insideToTheRight) {
                    for (int pos = 0; pos < way.way.getNodesCount() - 1; pos++) {
                        nodes.add(way.way.getNode(pos));
                    }
                }
                else {
                    for (int pos = way.way.getNodesCount() - 1; pos > 0; pos--) {
                        nodes.add(way.way.getNode(pos));
                    }
                }
            }

            return nodes;
        }
    }


    public static class AssembledMultipolygon {
        public AssembledPolygon outerWay;
        public List<AssembledPolygon> innerWays;

        public AssembledMultipolygon(AssembledPolygon way) {
            outerWay = way;
            innerWays = new ArrayList<AssembledPolygon>();
        }
    }

    /**
     * This hepler class implements algorithm traversing trough connected ways.
     * Assumes you are going in clockwise orientation.
     * @author viesturs
     *
     */
    private static class WayTraverser {

        private Set<WayInPolygon> availableWays;
        private WayInPolygon lastWay;
        private boolean lastWayReverse;

        public WayTraverser(Collection<WayInPolygon> ways) {

            availableWays = new HashSet<WayInPolygon>(ways);
            lastWay = null;
        }

        public void removeWays(Collection<WayInPolygon> ways) {
            availableWays.removeAll(ways);
        }

        public boolean hasWays() {
            return availableWays.size() > 0;
        }

        public WayInPolygon startNewWay(WayInPolygon way) {
            lastWay = way;
            lastWayReverse = !lastWay.insideToTheRight;

            return lastWay;
        }

        public WayInPolygon startNewWay() {
            if (availableWays.size() == 0) {
                lastWay = null;
            } else {
                lastWay = availableWays.iterator().next();
                lastWayReverse = !lastWay.insideToTheRight;
            }

            return lastWay;
        }


        public  WayInPolygon advanceNextLeftmostWay() {
            return advanceNextWay(false);
        }

        public  WayInPolygon advanceNextRightmostWay() {
            return advanceNextWay(true);
        }

        private WayInPolygon advanceNextWay(boolean rightmost) {

            Node headNode = !lastWayReverse ? lastWay.way.lastNode() : lastWay.way.firstNode();
            Node prevNode = !lastWayReverse ? lastWay.way.getNode(lastWay.way.getNodesCount() - 2) : lastWay.way.getNode(1);

            //find best next way
            WayInPolygon bestWay = null;
            Node bestWayNextNode = null;
            boolean bestWayReverse = false;

            for (WayInPolygon way : availableWays) {
                if (way.way.firstNode().equals(headNode)) {
                    //start adjacent to headNode
                    Node nextNode = way.way.getNode(1);

                    if (nextNode.equals(prevNode))
                    {
                        //this is the path we came from - ignore it.
                    }
                    else if (bestWay == null || (isToTheRightSideOfLine(prevNode, headNode, bestWayNextNode, nextNode) == rightmost)) {
                        //the new way is better
                        bestWay = way;
                        bestWayReverse = false;
                        bestWayNextNode = nextNode;
                    }
                }

                if (way.way.lastNode().equals(headNode)) {
                    //end adjacent to headNode
                    Node nextNode = way.way.getNode(way.way.getNodesCount() - 2);

                    if (nextNode.equals(prevNode)) {
                        //this is the path we came from - ignore it.
                    }
                    else if (bestWay == null || (isToTheRightSideOfLine(prevNode, headNode, bestWayNextNode, nextNode) == rightmost)) {
                        //the new way is better
                        bestWay = way;
                        bestWayReverse = true;
                        bestWayNextNode = nextNode;
                    }
                }
            }

            lastWay = bestWay;
            lastWayReverse = bestWayReverse;

            return lastWay;
        }

        public boolean isLastWayInsideToTheRight() {
            return lastWayReverse != lastWay.insideToTheRight;
        }

        public Node getLastWayStartNode() {
            return lastWayReverse ? lastWay.way.lastNode() : lastWay.way.firstNode();
        }

        public Node getLastWayEndNode() {
            return lastWayReverse ? lastWay.way.firstNode() : lastWay.way.lastNode();
        }
    }

    /**
     * Provides some node order , based on coordinates, nodes with equal coordinates are equal.
     * @author viesturs
     *
     */
    private class NodePositionComparator implements Comparator<Node> {

        @Override
        public int compare(Node n1, Node n2) {

            double dLat = n1.getCoor().lat() - n2.getCoor().lat();
            double dLon = n1.getCoor().lon() - n2.getCoor().lon();

            if (dLat > 0)
                return 1;
            else if (dLat < 0)
                return -1;
            else if (dLon == 0) //dlat is 0 here
                return 0;
            else
                return dLon > 0 ? 1 : -1;
        }
    }


    /**
     * Helper storage class for finding findOuterWays
     * @author viesturs
     */
    static class PolygonLevel {
        public final int level;
        public final AssembledMultipolygon pol;

        public PolygonLevel(AssembledMultipolygon _pol, int _level) {
            pol = _pol;
            level = _level;
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

        List<Node> allNodes = new ArrayList<Node>();
        for (Way way : ways) {
            if (!way.isClosed()) {
                JOptionPane.showMessageDialog(Main.parent, tr("\"{0}\" is not closed and therefore cannot be joined.", way.getName()));
                return;
            }

            allNodes.addAll(way.getNodes());
        }

        // TODO: Only display this warning when nodes outside dataSourceArea are deleted
        Area dataSourceArea = Main.main.getCurrentDataSet().getDataSourceArea();
        if (dataSourceArea != null) {
            for (Node node : allNodes) {
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

        //analyze multipolygon relations and collect all areas
        List<Multipolygon> areas = collectMultipolygons(ways);

        if (areas == null)
            //too complex multipolygon relations found
            return;

        if (!testJoin(areas)) {
            JOptionPane.showMessageDialog(Main.parent, tr("No intersection found. Nothing was changed."));
            return;
        }

        if (!resolveTagConflicts(areas))
            return;
        //user cancelled, do nothing.

        try {
            JoinAreasResult result = joinAreas(areas);

            if (result.hasChanges) {

                Main.map.mapView.repaint();
                DataSet ds = Main.main.getCurrentDataSet();
                ds.fireSelectionChanged();
            } else {
                JOptionPane.showMessageDialog(Main.parent, tr("No intersection found. Nothing was changed."));
            }
        }
        catch (UserCancelException exception) {
            //revert changes
            //FIXME: this is dirty hack
            makeCommitsOneAction(tr("Reverting changes"));
            Main.main.undoRedo.undo();
            Main.main.undoRedo.redoCommands.clear();
        }
    }


    /**
     * Tests if the areas have some intersections to join.
     * @param areas
     * @return
     */
    private boolean testJoin(List<Multipolygon> areas) {
        List<Way> allStartingWays = new ArrayList<Way>();

        for (Multipolygon area : areas) {
            allStartingWays.add(area.outerWay);
            allStartingWays.addAll(area.innerWays);
        }

        //find intersection points
        ArrayList<Node> nodes = addIntersections(allStartingWays, true);
        return nodes.size() > 0;
    }

    /**
     * Will join two or more overlapping areas
     * @param areas - list of areas to join
     * @return new area formed.
     */
    private JoinAreasResult joinAreas(List<Multipolygon> areas) throws UserCancelException {

        JoinAreasResult result = new JoinAreasResult();
        result.hasChanges = false;

        List<Way> allStartingWays = new ArrayList<Way>();
        List<Way> innerStartingWays = new ArrayList<Way>();
        List<Way> outerStartingWays = new ArrayList<Way>();

        for (Multipolygon area : areas) {
            outerStartingWays.add(area.outerWay);
            innerStartingWays.addAll(area.innerWays);
        }

        allStartingWays.addAll(innerStartingWays);
        allStartingWays.addAll(outerStartingWays);

        //first remove nodes in the same coordinate
        boolean removedDuplicates = false;
        removedDuplicates |= removeDuplicateNodes(allStartingWays);

        if (removedDuplicates) {
            result.hasChanges = true;
            commitCommands(marktr("Removed duplicate nodes"));
        }

        //find intersection points
        ArrayList<Node> nodes = addIntersections(allStartingWays, false);

        //no intersections, return.
        if (nodes.size() == 0) return result;
        commitCommands(marktr("Added node on all intersections"));

        ArrayList<RelationRole> relations = new ArrayList<RelationRole>();

        // Remove ways from all relations so ways can be combined/split quietly
        for (Way way : allStartingWays) {
            relations.addAll(removeFromAllRelations(way));
        }

        // Don't warn now, because it will really look corrupted
        boolean warnAboutRelations = relations.size() > 0 && allStartingWays.size() > 1;

        ArrayList<WayInPolygon> preparedWays = new ArrayList<WayInPolygon>();

        for (Way way : outerStartingWays) {
            ArrayList<Way> splitWays = splitWayOnNodes(way, nodes);
            preparedWays.addAll(markWayInsideSide(splitWays, false));
        }

        for (Way way : innerStartingWays) {
            ArrayList<Way> splitWays = splitWayOnNodes(way, nodes);
            preparedWays.addAll(markWayInsideSide(splitWays, true));
        }

        // Find boundary ways
        ArrayList<Way> discardedWays = new ArrayList<Way>();
        List<AssembledPolygon> bounadries = findBoundaryPolygons(preparedWays, discardedWays);

        //find polygons
        List<AssembledMultipolygon> preparedPolygons = findPolygons(bounadries);

        //assemble final ways
        List<Multipolygon> polygons = new ArrayList<Multipolygon>();
        for (AssembledMultipolygon pol : preparedPolygons) {
            polygons.add(joinPolygon(pol));
        }

        // Delete the discarded inner ways
        if (discardedWays.size() > 0) {
            cmds.add(DeleteCommand.delete(Main.map.mapView.getEditLayer(), discardedWays, true));
        }
        commitCommands(marktr("Delete Ways that are not part of an inner multipolygon"));

        // We can attach our new multipolygon relation and pretend it has always been there
        for (Multipolygon pol : polygons) {
            addOwnMultigonRelation(pol.innerWays, pol.outerWay, relations);
            fixRelations(relations, pol.outerWay);
        }

        commitCommands(marktr("Fix relations"));

        for (Multipolygon pol : polygons) {
            stripTags(pol.innerWays);
        }

        makeCommitsOneAction(marktr("Joined overlapping areas"));

        if (warnAboutRelations) {
            JOptionPane.showMessageDialog(Main.parent, tr("Some of the ways were part of relations that have been modified. Please verify no errors have been introduced."));
        }

        result.hasChanges = true;
        result.mergeSuccessful = true;
        result.polygons = polygons;
        return result;
    }

    /**
     * Checks if tags of two given ways differ, and presents the user a dialog to solve conflicts
     * @param Way First way to check
     * @param Way Second Way to check
     * @return boolean True if all conflicts are resolved, False if conflicts remain.
     */
    private boolean resolveTagConflicts(List<Multipolygon> polygons) {

        List<Way> ways = new ArrayList<Way>();

        for (Multipolygon pol : polygons) {
            ways.add(pol.outerWay);
            ways.addAll(pol.innerWays);
        }

        if (ways.size() < 2)
            return true;

        //mostly copied from CombineWayAction.java.
        TagCollection wayTags = TagCollection.unionOfAllPrimitives(ways);
        TagCollection completeWayTags = new TagCollection(wayTags);
        combineTigerTags(completeWayTags);
        normalizeTagCollectionBeforeEditing(completeWayTags, ways);
        TagCollection tagsToEdit = new TagCollection(completeWayTags);
        completeTagCollectionForEditing(tagsToEdit);

        CombinePrimitiveResolverDialog dialog = CombinePrimitiveResolverDialog.getInstance();
        dialog.getTagConflictResolverModel().populate(tagsToEdit, completeWayTags.getKeysWithMultipleValues());
        dialog.setTargetPrimitive(ways.get(0));
        Collection<Relation> parentRelations = CombineWayAction.getParentRelations(ways);
        parentRelations = filterOwnMultipolygonRelations(parentRelations, polygons);
        dialog.getRelationMemberConflictResolverModel().populate(
                parentRelations,
                ways
        );
        dialog.prepareDefaultDecisions();

        // resolve tag conflicts if necessary
        //
        if (!completeWayTags.isApplicableToPrimitive() || !parentRelations.isEmpty()) {
            dialog.setVisible(true);
            if (dialog.isCancelled())
                return false;
        }

        for (Way way : ways) {
            dialog.setTargetPrimitive(way);
            cmds.addAll(dialog.buildResolutionCommands());
        }

        commitCommands(marktr("Fix tag conflicts"));
        return true;
    }


    /**
     * This method removes duplicate points (if any) from the input way.
     * @param way the way to process
     * @return true if any changes where made
     */
    private boolean removeDuplicateNodes(List<Way> ways) {
        //TODO: maybe join nodes with JoinNodesAction, rather than reconnect the ways.

        Map<Node, Node> nodeMap = new TreeMap<Node, Node>(new NodePositionComparator());
        int totalNodesRemoved = 0;

        for (Way way : ways) {
            if (way.getNodes().size() < 2) {
                continue;
            }

            int nodesRemoved = 0;
            List<Node> newNodes = new ArrayList<Node>();
            Node prevNode = null;

            for (Node node : way.getNodes()) {
                if (!nodeMap.containsKey(node)) {
                    //new node
                    nodeMap.put(node, node);

                    //avoid duplicate nodes
                    if (prevNode != node) {
                        newNodes.add(node);
                    } else {
                        nodesRemoved ++;
                    }
                } else {
                    //node with same coordinates already exists, substitute with existing node
                    Node representator = nodeMap.get(node);

                    if (representator != node) {
                        nodesRemoved ++;
                    }

                    //avoid duplicate node
                    if (prevNode != representator) {
                        newNodes.add(representator);
                    }
                }
                prevNode = node;
            }

            if (nodesRemoved > 0) {

                if (newNodes.size() == 1) { //all nodes in the same coordinate - add one more node, to have closed way.
                    newNodes.add(newNodes.get(0));
                }

                Way newWay=new Way(way);
                newWay.setNodes(newNodes);
                cmds.add(new ChangeCommand(way, newWay));
                totalNodesRemoved += nodesRemoved;
            }
        }

        return totalNodesRemoved > 0;
    }



    /**
     * Will find all intersection and add nodes there for list of given ways. Handles self-intersections too.
     * And make commands to add the intersection points to ways.
     * @param List<Way> - a list of ways to test
     * @return ArrayList<Node> List of new nodes
     * Prerequisite: no two nodes have the same coordinates.
     */
    private ArrayList<Node> addIntersections(List<Way> ways, boolean test) {
        //TODO: this is a bit slow - O( (number of nodes)^2 + numberOfIntersections * numberOfNodes )

        //stupid java, cannot instantiate array of generic classes..
        @SuppressWarnings("unchecked")
        ArrayList<Node>[] newNodes = new ArrayList[ways.size()];
        boolean[] changedWays = new boolean[ways.size()];

        Set<Node> intersectionNodes = new LinkedHashSet<Node>();

        for (int pos = 0; pos < ways.size(); pos ++) {
            newNodes[pos] = new ArrayList<Node>(ways.get(pos).getNodes());
            changedWays[pos] = false;
        }

        //iterate over all segment pairs and introduce the intersections

        Comparator<Node> coordsComparator = new NodePositionComparator();

        int seg1Way = 0;
        int seg1Pos = -1;

        while (true) {
            //advance to next segment
            seg1Pos++;
            if (seg1Pos > newNodes[seg1Way].size() - 2) {
                seg1Way++;
                seg1Pos = 0;

                if (seg1Way == ways.size()) { //finished
                    break;
                }
            }


            //iterate over secondary segment

            int seg2Way = seg1Way;
            int seg2Pos = seg1Pos + 1;//skip the adjacent segment

            while (true) {

                //advance to next segment
                seg2Pos++;
                if (seg2Pos > newNodes[seg2Way].size() - 2) {
                    seg2Way++;
                    seg2Pos = 0;

                    if (seg2Way == ways.size()) { //finished
                        break;
                    }
                }

                //need to get them again every time, because other segments may be changed
                Node seg1Node1 = newNodes[seg1Way].get(seg1Pos);
                Node seg1Node2 = newNodes[seg1Way].get(seg1Pos + 1);
                Node seg2Node1 = newNodes[seg2Way].get(seg2Pos);
                Node seg2Node2 = newNodes[seg2Way].get(seg2Pos + 1);

                int commonCount = 0;
                //test if we have common nodes to add.
                if (seg1Node1 == seg2Node1 || seg1Node1 == seg2Node2) {
                    commonCount ++;

                    if (seg1Way == seg2Way &&
                            seg1Pos == 0 &&
                            seg2Pos == newNodes[seg2Way].size() -2) {
                        //do not add - this is first and last segment of the same way.
                    } else {
                        intersectionNodes.add(seg1Node1);
                    }
                }

                if (seg1Node2 == seg2Node1 || seg1Node2 == seg2Node2) {
                    commonCount ++;

                    intersectionNodes.add(seg1Node2);
                }

                //no common nodes - find intersection
                if (commonCount == 0) {
                    LatLon intersection = getLineLineIntersection(
                            seg1Node1.getEastNorth().east(), seg1Node1.getEastNorth().north(),
                            seg1Node2.getEastNorth().east(), seg1Node2.getEastNorth().north(),
                            seg2Node1.getEastNorth().east(), seg2Node1.getEastNorth().north(),
                            seg2Node2.getEastNorth().east(), seg2Node2.getEastNorth().north());

                    if (intersection != null) {
                        if (test) {
                            intersectionNodes.add(seg2Node1);
                            return new ArrayList<Node>(intersectionNodes);
                        }

                        Node newNode = new Node(intersection);
                        Node intNode = newNode;
                        boolean insertInSeg1 = false;
                        boolean insertInSeg2 = false;

                        //find if the intersection point is at end point of one of the segments, if so use that point

                        //segment 1
                        if (coordsComparator.compare(newNode, seg1Node1) == 0) {
                            intNode = seg1Node1;
                        } else if (coordsComparator.compare(newNode, seg1Node2) == 0) {
                            intNode = seg1Node2;
                        } else {
                            insertInSeg1 = true;
                        }

                        //segment 2
                        if (coordsComparator.compare(newNode, seg2Node1) == 0) {
                            intNode = seg2Node1;
                        } else if (coordsComparator.compare(newNode, seg2Node2) == 0) {
                            intNode = seg2Node2;
                        } else {
                            insertInSeg2 = true;
                        }

                        if (insertInSeg1) {
                            newNodes[seg1Way].add(seg1Pos +1, intNode);
                            changedWays[seg1Way] = true;

                            //fix seg2 position, as indexes have changed, seg2Pos is always bigger than seg1Pos on the same segment.
                            if (seg2Way == seg1Way) {
                                seg2Pos ++;
                            }
                        }

                        if (insertInSeg2) {
                            newNodes[seg2Way].add(seg2Pos +1, intNode);
                            changedWays[seg2Way] = true;

                            //Do not need to compare again to already split segment
                            seg2Pos ++;
                        }

                        intersectionNodes.add(intNode);

                        if (intNode == newNode) {
                            cmds.add(new AddCommand(intNode));
                        }
                    }
                }
                else if (test && intersectionNodes.size() > 0)
                    return new ArrayList<Node>(intersectionNodes);
            }
        }

        for (int pos = 0; pos < ways.size(); pos ++) {
            if (changedWays[pos] == false) {
                continue;
            }

            Way way = ways.get(pos);
            Way newWay = new Way(way);
            newWay.setNodes(newNodes[pos]);

            cmds.add(new ChangeCommand(way, newWay));
        }

        return new ArrayList<Node>(intersectionNodes);
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
        if (det == 0) return null; // Lines are parallel

        return Main.proj.eastNorth2latlon(new EastNorth(
                (b1*c2 - b2*c1)/det,
                (a2*c1 -a1*c2)/det
        ));
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
     * This method analyzes the way and assigns each part what direction polygon "inside" is.
     * @param parts the split parts of the way
     * @param isInner - if true, reverts the direction (for multipolygon islands)
     * @return list of parts, marked with the inside orientation.
     */
    private ArrayList<WayInPolygon> markWayInsideSide(List<Way> parts, boolean isInner) {

        ArrayList<WayInPolygon> result = new ArrayList<WayInPolygon>();

        //prepare prev and next maps
        Map<Way, Way> nextWayMap = new HashMap<Way, Way>();
        Map<Way, Way> prevWayMap = new HashMap<Way, Way>();

        for (int pos = 0; pos < parts.size(); pos ++) {

            if (!parts.get(pos).lastNode().equals(parts.get((pos + 1) % parts.size()).firstNode()))
                throw new RuntimeException("Way not circular");

            nextWayMap.put(parts.get(pos), parts.get((pos + 1) % parts.size()));
            prevWayMap.put(parts.get(pos), parts.get((pos + parts.size() - 1) % parts.size()));
        }

        //find the node with minimum y - it's guaranteed to be outer. (What about the south pole?)
        Way topWay = null;
        Node topNode = null;
        int topIndex = 0;
        double minY = Double.POSITIVE_INFINITY;

        for (Way way : parts) {
            for (int pos = 0; pos < way.getNodesCount(); pos ++) {
                Node node = way.getNode(pos);

                if (node.getEastNorth().getY() < minY) {
                    minY = node.getEastNorth().getY();
                    topWay = way;
                    topNode = node;
                    topIndex = pos;
                }
            }
        }

        //get the upper way and it's orientation.

        boolean wayClockwise; // orientation of the top way.

        if (topNode.equals(topWay.firstNode()) || topNode.equals(topWay.lastNode())) {
            Node headNode = null; // the node at junction
            Node prevNode = null; // last node from previous path
            wayClockwise = false;

            //node is in split point - find the outermost way from this point

            headNode = topNode;
            //make a fake node that is downwards from head node (smaller Y). It will be a division point between paths.
            prevNode = new Node(new EastNorth(headNode.getEastNorth().getX(), headNode.getEastNorth().getY() - 1e5));

            topWay = null;
            wayClockwise = false;
            Node bestWayNextNode = null;

            for (Way way : parts) {
                if (way.firstNode().equals(headNode)) {
                    Node nextNode = way.getNode(1);

                    if (topWay == null || !isToTheRightSideOfLine(prevNode, headNode, bestWayNextNode, nextNode)) {
                        //the new way is better
                        topWay = way;
                        wayClockwise = true;
                        bestWayNextNode = nextNode;
                    }
                }

                if (way.lastNode().equals(headNode)) {
                    //end adjacent to headNode
                    Node nextNode = way.getNode(way.getNodesCount() - 2);

                    if (topWay == null || !isToTheRightSideOfLine(prevNode, headNode, bestWayNextNode, nextNode)) {
                        //the new way is better
                        topWay = way;
                        wayClockwise = false;
                        bestWayNextNode = nextNode;
                    }
                }
            }
        } else {
            //node is inside way - pick the clockwise going end.
            Node prev = topWay.getNode(topIndex - 1);
            Node next = topWay.getNode(topIndex + 1);

            //there will be no parallel segments in the middle of way, so all fine.
            wayClockwise = angleIsClockwise(prev, topNode, next);
        }

        Way curWay = topWay;
        boolean curWayInsideToTheRight = wayClockwise ^ isInner;

        //iterate till full circle is reached
        while (true) {

            //add cur way
            WayInPolygon resultWay = new WayInPolygon(curWay, curWayInsideToTheRight);
            result.add(resultWay);

            //process next way
            Way nextWay = nextWayMap.get(curWay);
            Node prevNode = curWay.getNode(curWay.getNodesCount() - 2);
            Node headNode = curWay.lastNode();
            Node nextNode = nextWay.getNode(1);

            if (nextWay == topWay) {
                //full loop traversed - all done.
                break;
            }


            //find intersecting segments
            // the intersections will look like this:
            //
            //                       ^
            //                       |
            //                       X wayBNode
            //                       |
            //                  wayB |
            //                       |
            //             curWay    |       nextWay
            //----X----------------->X----------------------X---->
            //    prevNode           ^headNode              nextNode
            //                       |
            //                       |
            //                  wayA |
            //                       |
            //                       X wayANode
            //                       |

            int intersectionCount = 0;

            for (Way wayA : parts) {

                if (wayA == curWay) {
                    continue;
                }

                if (wayA.lastNode().equals(headNode)) {

                    Way wayB = nextWayMap.get(wayA);

                    //test if wayA is opposite wayB relative to curWay and nextWay

                    Node wayANode = wayA.getNode(wayA.getNodesCount() - 2);
                    Node wayBNode = wayB.getNode(1);

                    boolean wayAToTheRight = isToTheRightSideOfLine(prevNode, headNode, nextNode, wayANode);
                    boolean wayBToTheRight = isToTheRightSideOfLine(prevNode, headNode, nextNode, wayBNode);

                    if (wayAToTheRight != wayBToTheRight) {
                        intersectionCount ++;
                    }
                }
            }

            //if odd number of crossings, invert orientation
            if (intersectionCount % 2 == 1) {
                curWayInsideToTheRight = !curWayInsideToTheRight;
            }

            curWay = nextWay;
        }

        return result;
    }

    /**
     * This is a method splits way into smaller parts, using the prepared nodes list as split points.
     * Uses  SplitWayAction.splitWay for the heavy lifting.
     * @return list of split ways (or original ways if no splitting is done).
     */
    private ArrayList<Way> splitWayOnNodes(Way way, Collection<Node> nodes) {

        ArrayList<Way> result = new ArrayList<Way>();
        List<List<Node>> chunks = buildNodeChunks(way, nodes);

        if (chunks.size() > 1) {
            SplitWayResult split = SplitWayAction.splitWay(Main.map.mapView.getEditLayer(), way, chunks, Collections.<OsmPrimitive>emptyList());

            //execute the command, we need the results
            cmds.add(split.getCommand());
            commitCommands(marktr("Split ways into fragments"));

            result.add(split.getOriginalWay());
            result.addAll(split.getNewWays());
        } else {
            //nothing to split
            result.add(way);
        }

        return result;
    }


    /**
     * Simple chunking version. Does not care about circular ways and result being proper, we will glue it all back together later on.
     * @param way the way to chunk
     * @param splitNodes the places where to cut.
     * @return list of node paths to produce.
     */
    private List<List<Node>> buildNodeChunks(Way way, Collection<Node> splitNodes) {
        List<List<Node>> result = new ArrayList<List<Node>>();
        List<Node> curList = new ArrayList<Node>();

        for (Node node : way.getNodes()) {
            curList.add(node);
            if (curList.size() > 1 && splitNodes.contains(node)) {
                result.add(curList);
                curList = new ArrayList<Node>();
                curList.add(node);
            }
        }

        if (curList.size() > 1) {
            result.add(curList);
        }

        return result;
    }


    /**
     * This method finds witch ways are outer and witch are inner.
     * @param boundaryWays
     * @return
     */
    private List<AssembledMultipolygon> findPolygons(Collection<AssembledPolygon> boundaries) {

        List<PolygonLevel> list = findOuterWaysImpl(0, boundaries);
        List<AssembledMultipolygon> result = new ArrayList<AssembledMultipolygon>();

        //take every other level
        for (PolygonLevel pol : list) {
            if (pol.level % 2 == 0) {
                result.add(pol.pol);
            }
        }

        return result;
    }

    /**
     * Collects outer way and corresponding inner ways from all boundaries.
     * @param boundaryWays
     * @return the outermostWay.
     */
    private List<PolygonLevel> findOuterWaysImpl(int level, Collection<AssembledPolygon> boundaryWays) {

        //TODO: bad performance for deep nestings...
        List<PolygonLevel> result = new ArrayList<PolygonLevel>();

        for (AssembledPolygon outerWay : boundaryWays) {

            boolean outerGood = true;
            List<AssembledPolygon> innerCandidates = new ArrayList<AssembledPolygon>();

            for (AssembledPolygon innerWay : boundaryWays) {
                if (innerWay == outerWay) {
                    continue;
                }

                if (wayInsideWay(outerWay, innerWay)) {
                    outerGood = false;
                    break;
                } else if (wayInsideWay(innerWay, outerWay)) {
                    innerCandidates.add(innerWay);
                }
            }

            if (!outerGood) {
                continue;
            }

            //add new outer polygon
            AssembledMultipolygon pol = new AssembledMultipolygon(outerWay);
            PolygonLevel polLev = new PolygonLevel(pol, level);

            //process inner ways
            if (innerCandidates.size() > 0) {
                List<PolygonLevel> innerList = findOuterWaysImpl(level + 1, innerCandidates);
                result.addAll(innerList);

                for (PolygonLevel pl : innerList) {
                    if (pl.level == level + 1) {
                        pol.innerWays.add(pl.pol.outerWay);
                    }
                }
            }

            result.add(polLev);
        }

        return result;
    }



    /**
     * Finds all ways that form inner or outer boundaries.
     * @param Collection<Way> A list of (splitted) ways that form a multigon and share common end nodes on intersections.
     * @param Collection<Way> this list is filled with ways that are to be discarded
     * @return Collection<Collection<Way>> A list of ways that form the outer and inner boundaries of the multigon.
     */
    public static List<AssembledPolygon> findBoundaryPolygons(Collection<WayInPolygon> multigonWays, List<Way> discardedResult) {
        //first find all discardable ways, by getting outer shells.
        //this will produce incorrect boundaries in some cases, but second pass will fix it.

        List<WayInPolygon> discardedWays = new ArrayList<WayInPolygon>();
        Set<WayInPolygon> processedWays = new HashSet<WayInPolygon>();
        WayTraverser traverser = new WayTraverser(multigonWays);

        for (WayInPolygon startWay : multigonWays) {
            if (processedWays.contains(startWay)) {
                continue;
            }

            traverser.startNewWay(startWay);

            List<WayInPolygon> boundary = new ArrayList<WayInPolygon>();
            WayInPolygon lastWay = startWay;

            while (true) {
                boundary.add(lastWay);

                WayInPolygon bestWay = traverser.advanceNextLeftmostWay();
                boolean wayInsideToTheRight = bestWay == null ? false : traverser.isLastWayInsideToTheRight();

                if (bestWay == null || processedWays.contains(bestWay) || !wayInsideToTheRight) {
                    //bad segment chain - proceed to discard it
                    lastWay = null;
                    break;
                } else if (boundary.contains(bestWay)) {
                    //traversed way found - close the way
                    lastWay = bestWay;
                    break;
                } else {
                    //proceed to next segment
                    lastWay = bestWay;
                }
            }

            if (lastWay != null) {
                //way good
                processedWays.addAll(boundary);

                //remove junk segments at the start
                while (boundary.get(0) != lastWay) {
                    discardedWays.add(boundary.get(0));
                    boundary.remove(0);
                }
            } else {
                //way bad
                discardedWays.addAll(boundary);
                processedWays.addAll(boundary);
            }
        }

        //now we have removed junk segments, collect the real result ways

        traverser.removeWays(discardedWays);

        List<AssembledPolygon> result = new ArrayList<AssembledPolygon>();

        while (traverser.hasWays()) {

            WayInPolygon startWay = traverser.startNewWay();
            List<WayInPolygon> boundary = new ArrayList<WayInPolygon>();
            WayInPolygon curWay = startWay;

            do {
                boundary.add(curWay);
                curWay = traverser.advanceNextRightmostWay();

                //should not happen
                if (curWay == null || !traverser.isLastWayInsideToTheRight())
                    throw new RuntimeException("Join areas internal error.");

            } while (curWay != startWay);

            //build result
            traverser.removeWays(boundary);
            result.add(new AssembledPolygon(boundary));
        }

        for (WayInPolygon way : discardedWays) {
            discardedResult.add(way.way);
        }

        //split inner polygons that have several touching parts.
        result = fixTouchingPolygons(result);

        return result;
    }


    /**
     * This method checks if polygons have several touching parts and splits them in several polygons.
     * @param polygon the polygon to process.
     */
    public static List<AssembledPolygon> fixTouchingPolygons(List<AssembledPolygon> polygons)
    {
        List<AssembledPolygon> newPolygons = new ArrayList<AssembledPolygon>();

        for (AssembledPolygon innerPart : polygons) {
            WayTraverser traverser = new WayTraverser(innerPart.ways);

            while (traverser.hasWays()) {

                WayInPolygon startWay = traverser.startNewWay();
                List<WayInPolygon> boundary = new ArrayList<WayInPolygon>();
                WayInPolygon curWay = startWay;

                Node startNode = traverser.getLastWayStartNode();
                boundary.add(curWay);

                while (startNode != traverser.getLastWayEndNode()) {
                    curWay = traverser.advanceNextLeftmostWay();
                    boundary.add(curWay);

                    //should not happen
                    if (curWay == null || !traverser.isLastWayInsideToTheRight())
                        throw new RuntimeException("Join areas internal error.");
                }

                //build result
                traverser.removeWays(boundary);
                newPolygons.add(new AssembledPolygon(boundary));
            }
        }

        return newPolygons;
    }


    /**
     * Tests if given point is to the right side of path consisting of 3 points.
     * @param lineP1 first point in path
     * @param lineP2 second point in path
     * @param lineP3 third point in path
     * @param testPoint
     * @return true if to the right side, false otherwise
     */
    public static boolean isToTheRightSideOfLine(Node lineP1, Node lineP2, Node lineP3, Node testPoint) {
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

    public static boolean angleIsClockwise(Node commonNode, Node firstNode, Node secondNode) {
        double dy1 = (firstNode.getEastNorth().getY() - commonNode.getEastNorth().getY());
        double dy2 = (secondNode.getEastNorth().getY() - commonNode.getEastNorth().getY());
        double dx1 = (firstNode.getEastNorth().getX() - commonNode.getEastNorth().getX());
        double dx2 = (secondNode.getEastNorth().getX() - commonNode.getEastNorth().getX());

        return dy1 * dx2 - dx1 * dy2 > 0;
    }


    /**
     * Tests if way is inside other way
     * @param outside
     * @param inside
     * @return
     */
    public static boolean wayInsideWay(AssembledPolygon inside, AssembledPolygon outside) {
        Set<Node> outsideNodes = new HashSet<Node>(outside.getNodes());

        for (Node insideNode : inside.getNodes()) {

            if (!outsideNodes.contains(insideNode))
                //simply test the one node
                return nodeInsidePolygon(insideNode, outside.getNodes());
        }

        //all nodes shared.
        return false;
    }

    /**
     * Tests if point is inside a polygon. The polygon can be self-intersecting. In such case the contains function works in xor-like manner.
     * @param polygonNodes list of nodes from polygon path.
     * @param point the point to test
     * @return true if the point is inside polygon.
     * FIXME: this should probably be moved to tools..
     */
    public static boolean nodeInsidePolygon(Node point, List<Node> polygonNodes) {
        if (polygonNodes.size() < 3)
            return false;

        boolean inside = false;
        Node p1, p2;

        //iterate each side of the polygon, start with the last segment
        Node oldPoint = polygonNodes.get(polygonNodes.size() - 1);

        for (Node newPoint : polygonNodes) {
            //skip duplicate points
            if (newPoint.equals(oldPoint)) {
                continue;
            }

            //order points so p1.lat <= p2.lat;
            if (newPoint.getEastNorth().getY() > oldPoint.getEastNorth().getY()) {
                p1 = oldPoint;
                p2 = newPoint;
            } else {
                p1 = newPoint;
                p2 = oldPoint;
            }

            //test if the line is crossed and if so invert the inside flag.
            if ((newPoint.getEastNorth().getY() < point.getEastNorth().getY()) == (point.getEastNorth().getY() <= oldPoint.getEastNorth().getY())
                    && (point.getEastNorth().getX() - p1.getEastNorth().getX()) * (p2.getEastNorth().getY() - p1.getEastNorth().getY())
                    < (p2.getEastNorth().getX() - p1.getEastNorth().getX()) * (point.getEastNorth().getY() - p1.getEastNorth().getY()))
            {
                inside = !inside;
            }

            oldPoint = newPoint;
        }

        return inside;
    }


    /**
     * Joins the lists of ways.
     * @param Collection<Way> The list of outer ways that belong to that multigon.
     * @return Way The newly created outer way
     */
    private Multipolygon  joinPolygon(AssembledMultipolygon polygon) throws UserCancelException {
        Multipolygon result = new Multipolygon(joinWays(polygon.outerWay.ways));

        for (AssembledPolygon pol : polygon.innerWays) {
            result.innerWays.add(joinWays(pol.ways));
        }

        return result;
    }


    /**
     * Joins the outer ways and deletes all short ways that can't be part of a multipolygon anyway.
     * @param Collection<Way> The list of outer ways that belong to that multigon.
     * @return Way The newly created outer way
     */
    private Way joinWays(List<WayInPolygon> ways) throws UserCancelException {

        //leave original orientation, if all paths are reverse.
        boolean allReverse = true;
        for (WayInPolygon way : ways) {
            allReverse &= !way.insideToTheRight;
        }

        if (allReverse) {
            for (WayInPolygon way : ways) {
                way.insideToTheRight = !way.insideToTheRight;
            }
        }

        Way joinedWay = joinOrientedWays(ways);

        //should not happen
        if (joinedWay == null || !joinedWay.isClosed())
            throw new RuntimeException("Join areas internal error.");

        return joinedWay;
    }


    /**
     * Joins a list of ways (using CombineWayAction and ReverseWayAction as specified in WayInPath)
     * @param ArrayList<Way> The list of ways to join and reverse
     * @return Way The newly created way
     */
    private Way joinOrientedWays(List<WayInPolygon> ways) throws UserCancelException{
        if (ways.size() < 2)
            return ways.get(0).way;

        // This will turn ways so all of them point in the same direction and CombineAction won't bug
        // the user about this.

        //TODO: ReverseWay and Combine way are really slow and we use them a lot here. This slows down large joins.
        List<Way> actionWays = new ArrayList<Way>(ways.size());

        for (WayInPolygon way : ways) {
            actionWays.add(way.way);

            if (!way.insideToTheRight) {
                ReverseWayResult res = ReverseWayAction.reverseWay(way.way);
                Main.main.undoRedo.add(res.getReverseCommand());
                cmdsCount++;
            }
        }

        Pair<Way, Command> result = CombineWayAction.combineWaysWorker(actionWays);

        Main.main.undoRedo.add(result.b);
        cmdsCount ++;

        return result.a;
    }


    /**
     * This method analyzes multipolygon relationships of given ways and collects addition inner ways to consider.
     * @param selectedWays the selected ways
     * @return list of polygons, or null if too complex relation encountered.
     */
    private List<Multipolygon> collectMultipolygons(List<Way> selectedWays) {

        List<Multipolygon> result = new ArrayList<Multipolygon>();

        //prepare the lists, to minimize memory allocation.
        List<Way> outerWays = new ArrayList<Way>();
        List<Way> innerWays = new ArrayList<Way>();

        Set<Way> processedOuterWays = new LinkedHashSet<Way>();
        Set<Way> processedInnerWays = new LinkedHashSet<Way>();

        for (Relation r : CombineWayAction.getParentRelations(selectedWays)) {
            if (r.isDeleted() ||
                    r.get("type") == null ||
                    !r.get("type").equalsIgnoreCase("multipolygon")) {
                continue;
            }

            boolean hasKnownOuter = false;
            outerWays.clear();
            innerWays.clear();

            for (RelationMember rm : r.getMembers()) {
                if (rm.getRole().equalsIgnoreCase("outer")) {
                    outerWays.add(rm.getWay());
                    hasKnownOuter |= selectedWays.contains(rm.getWay());
                }
                else if (rm.getRole().equalsIgnoreCase("inner")) {
                    innerWays.add(rm.getWay());
                }
            }

            if (!hasKnownOuter) {
                continue;
            }

            if (outerWays.size() > 1) {
                JOptionPane.showMessageDialog(Main.parent, tr("Sorry. Cannot handle multipolygon relations with multiple outer ways."));
                return null;
            }

            Way outerWay = outerWays.get(0);

            //retain only selected inner ways
            innerWays.retainAll(selectedWays);

            if (processedOuterWays.contains(outerWay)) {
                JOptionPane.showMessageDialog(Main.parent, tr("Sorry. Cannot handle way that is outer in multiple multipolygon relations."));
                return null;
            }

            if (processedInnerWays.contains(outerWay)) {
                JOptionPane.showMessageDialog(Main.parent, tr("Sorry. Cannot handle way that is both inner and outer in multipolygon relations."));
                return null;
            }

            for (Way way :innerWays)
            {
                if (processedOuterWays.contains(way)) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Sorry. Cannot handle way that is both inner and outer in multipolygon relations."));
                    return null;
                }

                if (processedInnerWays.contains(way)) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Sorry. Cannot handle way that is inner in multiple multipolygon relations."));
                    return null;
                }
            }

            processedOuterWays.add(outerWay);
            processedInnerWays.addAll(innerWays);

            Multipolygon pol = new Multipolygon(outerWay);
            pol.innerWays.addAll(innerWays);
            pol.relation = r;

            result.add(pol);
        }

        //add remaining ways, not in relations
        for (Way way : selectedWays) {
            if (processedOuterWays.contains(way) || processedInnerWays.contains(way)) {
                continue;
            }

            result.add(new Multipolygon(way));
        }

        return result;
    }


    /**
     * This method filters the list of relations that form the multipolygons.
     * @param relations
     * @param polygons
     * @return
     */
    private List<Relation> filterOwnMultipolygonRelations(Collection<Relation> relations, List<Multipolygon> polygons) {

        List<Relation> relationsToRemove = new ArrayList<Relation>();

        for (Multipolygon m : polygons) {
            if (m.relation != null) {
                relationsToRemove.add(m.relation);
            }
        }

        List<Relation> result = new ArrayList<Relation>();

        result.addAll(relations);
        result.removeAll(relationsToRemove);
        return result;
    }


    /**
     * Will add own multipolygon relation to the "previously existing" relations. Fixup is done by fixRelations
     * @param Collection<Way> List of already closed inner ways
     * @param Way The outer way
     * @param ArrayList<RelationRole> The list of relation with roles to add own relation to
     */
    private void addOwnMultigonRelation(Collection<Way> inner, Way outer, ArrayList<RelationRole> rels) {
        if (inner.size() == 0) return;
        // Create new multipolygon relation and add all inner ways to it
        Relation newRel = new Relation();
        newRel.put("type", "multipolygon");
        for (Way w : inner) {
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
     * Removes a given OsmPrimitive from all relations
     * @param OsmPrimitive Element to remove from all relations
     * @return ArrayList<RelationRole> List of relations with roles the primitives was part of
     */
    private ArrayList<RelationRole> removeFromAllRelations(OsmPrimitive osm) {
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
                if (!result.contains(saverel)) {
                    result.add(saverel);
                }
                break;
            }
        }

        commitCommands(marktr("Removed Element from Relations"));
        return result;
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
        for (RelationRole r : rels) {
            if (r.rel.get("type") != null &&
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
        switch (multiouters.size()) {
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
            for (RelationRole r : multiouters) {
                // Add members
                for (RelationMember rm : r.rel.getMembers())
                    if (!newRel.getMembers().contains(rm)) {
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
        for (Way w : ways) {
            stripTags(w);
        }
        commitCommands(marktr("Remove tags from inner ways"));
    }

    /**
     * @param Way The Way to remove all tags from
     */
    private void stripTags(Way x) {
        if (x.getKeys() == null)
            return;
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
        for (; i < ur.commands.size(); i++) {
            cmds.add(ur.commands.get(i));
        }

        for (i = 0; i < cmds.size(); i++) {
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
