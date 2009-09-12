// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Combines multiple ways into one.
 *
 */
public class CombineWayAction extends JosmAction {

    public CombineWayAction() {
        super(tr("Combine Way"), "combineway", tr("Combine several ways into one."),
                Shortcut.registerShortcut("tools:combineway", tr("Tool: {0}", tr("Combine Way")), KeyEvent.VK_C, Shortcut.GROUP_EDIT), true);
    }

    protected Set<OsmPrimitive> intersect(Set<? extends OsmPrimitive> s1, Set<? extends OsmPrimitive> s2) {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>(s1);
        ret.retainAll(s2);
        return ret;
    }

    protected boolean confirmCombiningWithConflictsInRelationMemberships() {
        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Combine ways with different memberships?"),
                new String[] {tr("Combine Anyway"), tr("Cancel")});
        ed.setButtonIcons(new String[] {"combineway.png", "cancel.png"});
        ed.setContent(tr("The selected ways have differing relation memberships.  "
                + "Do you still want to combine them?"));
        ed.showDialog();

        return ed.getValue() == 1;
    }

    protected boolean confirmChangeDirectionOfWays() {
        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Change directions?"),
                new String[] {tr("Reverse and Combine"), tr("Cancel")});
        ed.setButtonIcons(new String[] {"wayflip.png", "cancel.png"});
        ed.setContent(tr("The ways can not be combined in their current directions.  "
                + "Do you want to reverse some of them?"));
        ed.showDialog();
        return ed.getValue() == 1;
    }

    protected void warnCombiningImpossible() {
        String msg = tr("Could not combine ways "
                + "(They could not be merged into a single string of nodes)");
        JOptionPane.showMessageDialog(
                Main.parent,
                msg,  //FIXME: not sure whether this fits in a dialog
                tr("Information"),
                JOptionPane.INFORMATION_MESSAGE
        );
        return;
    }

    protected Way getTargetWay(Collection<Way> combinedWays) {
        // init with an arbitrary way
        Way targetWay = combinedWays.iterator().next();

        // look for the first way already existing on
        // the server
        for (Way w : combinedWays) {
            targetWay = w;
            if (w.getId() != 0) {
                break;
            }
        }
        return targetWay;
    }

    protected void completeTagCollectionWithMissingTags(TagCollection tc, Collection<Way> combinedWays) {
        for (String key: tc.getKeys()) {
            // make sure the empty value is in the tag set such that we can delete the tag
            // in the conflict dialog if necessary
            //
            tc.add(new Tag(key,""));
            for (Way w: combinedWays) {
                if (w.get(key) == null) {
                    tc.add(new Tag(key)); // add a tag with key and empty value
                }
            }
        }
        // remove irrelevant tags
        //
        tc.removeByKey("created_by");
    }

    public void combineWays(Collection<Way> ways) {

        // prepare and clean the list of ways to combine
        //
        if (ways == null || ways.isEmpty())
            return;
        ways.remove(null); // just in case -  remove all null ways from the collection
        ways = new HashSet<Way>(ways); // remove duplicates

        // build the list of relations referring to the ways to combine
        //
        WayReferringRelations referringRelations = new WayReferringRelations(ways);
        referringRelations.build(getCurrentDataSet());

        // build the collection of tags used by the ways to combine
        //
        TagCollection wayTags = TagCollection.unionOfAllPrimitives(ways);


        // try to build a new way out of the combination of ways
        // which are combined
        //
        NodeGraph graph = NodeGraph.createDirectedGraphFromWays(ways);
        List<Node> path = graph.buildSpanningPath();
        if (path == null) {
            graph = NodeGraph.createUndirectedGraphFromNodeWays(ways);
            path = graph.buildSpanningPath();
            if (path != null) {
                if (!confirmChangeDirectionOfWays())
                    return;
            } else {
                warnCombiningImpossible();
                return;
            }
        }

        // create the new way and apply the new node list
        //
        Way targetWay = getTargetWay(ways);
        Way modifiedTargetWay = new Way(targetWay);
        modifiedTargetWay.setNodes(path);

        TagCollection completeWayTags = new TagCollection(wayTags);
        completeTagCollectionWithMissingTags(completeWayTags, ways);

        CombinePrimitiveResolverDialog dialog = CombinePrimitiveResolverDialog.getInstance();
        dialog.getTagConflictResolverModel().populate(completeWayTags);
        dialog.setTargetPrimitive(targetWay);
        dialog.getRelationMemberConflictResolverModel().populate(
                referringRelations.getRelations(),
                referringRelations.getWays()
        );
        dialog.prepareDefaultDecisions();

        // resolve tag conflicts if necessary
        //
        if (!wayTags.isApplicableToPrimitive() || !referringRelations.getRelations().isEmpty()) {
            dialog.setVisible(true);
            if (dialog.isCancelled())
                return;
        }



        LinkedList<Command> cmds = new LinkedList<Command>();
        LinkedList<Way> deletedWays = new LinkedList<Way>(ways);
        deletedWays.remove(targetWay);

        cmds.add(new DeleteCommand(deletedWays));
        cmds.add(new ChangeCommand(targetWay, modifiedTargetWay));
        cmds.addAll(dialog.buildResolutionCommands());
        final SequenceCommand sequenceCommand = new SequenceCommand(tr("Combine {0} ways", ways.size()), cmds);

        // update gui
        final Way selectedWay = targetWay;
        Runnable guiTask = new Runnable() {
            public void run() {
                Main.main.undoRedo.add(sequenceCommand);
                getCurrentDataSet().setSelected(selectedWay);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            guiTask.run();
        } else {
            SwingUtilities.invokeLater(guiTask);
        }
    }


    public void actionPerformed(ActionEvent event) {
        if (getCurrentDataSet() == null)
            return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        Set<Way> selectedWays = OsmPrimitive.getFilteredSet(selection, Way.class);
        if (selectedWays.size() < 2) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least two ways to combine."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        combineWays(selectedWays);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
            return;
        }
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        int numWays = 0;

        for (OsmPrimitive osm : selection)
            if (osm instanceof Way) {
                numWays++;
            }
        setEnabled(numWays >= 2);
    }

    /**
     * This is a collection of relations referring to at least one out of a set of
     * ways.
     * 
     *
     */
    static private class WayReferringRelations {
        /**
         * the map references between relations and ways. The key is a ways, the value is a
         * set of relations referring to that way.
         */
        private Map<Way, Set<Relation>> wayRelationMap;

        /**
         * 
         * @param ways  a collection of ways
         */
        public WayReferringRelations(Collection<Way> ways) {
            wayRelationMap = new HashMap<Way, Set<Relation>>();
            if (ways == null) return;
            ways.remove(null); // just in case - remove null values
            for (Way way: ways) {
                if (!wayRelationMap.containsKey(way)) {
                    wayRelationMap.put(way, new HashSet<Relation>());
                }
            }
        }

        /**
         * build the sets of referring relations from the relations in the dataset <code>ds</code>
         * 
         * @param ds the data set
         */
        public void build(DataSet ds) {
            for (Relation r: ds.relations) {
                if (r.isDeleted() || r.incomplete) {
                    continue;
                }
                Set<Way> referringWays = OsmPrimitive.getFilteredSet(r.getMemberPrimitives(), Way.class);
                for (Way w : wayRelationMap.keySet()) {
                    if (referringWays.contains(w)) {
                        wayRelationMap.get(w).add(r);
                    }
                }
            }
        }

        /**
         * Replies the ways
         * @return the ways
         */
        public Set<Way> getWays() {
            return wayRelationMap.keySet();
        }

        /**
         * Replies the set of referring relations
         * 
         * @return the set of referring relations
         */
        public Set<Relation> getRelations() {
            HashSet<Relation> ret = new HashSet<Relation>();
            for (Way w: wayRelationMap.keySet()) {
                ret.addAll(wayRelationMap.get(w));
            }
            return ret;
        }

        /**
         * Replies the set of referring relations for a specific way
         * 
         * @return the set of referring relations
         */
        public Set<Relation> getRelations(Way way) {
            return wayRelationMap.get(way);
        }

        protected Command buildRelationUpdateCommand(Relation relation, Collection<Way> ways, Way targetWay) {
            List<RelationMember> newMembers = new ArrayList<RelationMember>();
            for (RelationMember rm : relation.getMembers()) {
                if (ways.contains(rm.getMember())) {
                    RelationMember newMember = new RelationMember(rm.getRole(),targetWay);
                    newMembers.add(newMember);
                } else {
                    newMembers.add(rm);
                }
            }
            Relation newRelation = new Relation(relation);
            newRelation.setMembers(newMembers);
            return new ChangeCommand(relation, newRelation);
        }

        public List<Command> buildRelationUpdateCommands(Way targetWay) {
            Collection<Way> toRemove = getWays();
            toRemove.remove(targetWay);
            ArrayList<Command> cmds = new ArrayList<Command>();
            for (Relation r : getRelations()) {
                Command cmd = buildRelationUpdateCommand(r, toRemove, targetWay);
                cmds.add(cmd);
            }
            return cmds;
        }
    }

    static public class NodePair {
        private Node a;
        private Node b;
        public NodePair(Node a, Node b) {
            this.a =a;
            this.b = b;
        }

        public NodePair(Pair<Node,Node> pair) {
            this.a = pair.a;
            this.b = pair.b;
        }

        public NodePair(NodePair other) {
            this.a = other.a;
            this.b = other.b;
        }

        public Node getA() {
            return a;
        }

        public Node getB() {
            return b;
        }

        public boolean isAdjacentToA(NodePair other) {
            return other.getA() == a || other.getB() == a;
        }

        public boolean isAdjacentToB(NodePair other) {
            return other.getA() == b || other.getB() == b;
        }

        public boolean isSuccessorOf(NodePair other) {
            return other.getB() == a;
        }

        public boolean isPredecessorOf(NodePair other) {
            return b == other.getA();
        }

        public NodePair swap() {
            return new NodePair(b,a);
        }

        @Override
        public String toString() {
            return new StringBuilder()
            .append("[")
            .append(a.getId())
            .append(",")
            .append(b.getId())
            .append("]")
            .toString();
        }

        public boolean contains(Node n) {
            return a == n || b == n;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((a == null) ? 0 : a.hashCode());
            result = prime * result + ((b == null) ? 0 : b.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            NodePair other = (NodePair) obj;
            if (a == null) {
                if (other.a != null)
                    return false;
            } else if (!a.equals(other.a))
                return false;
            if (b == null) {
                if (other.b != null)
                    return false;
            } else if (!b.equals(other.b))
                return false;
            return true;
        }
    }


    static public class NodeGraph {
        static public List<NodePair> buildNodePairs(Way way, boolean directed) {
            ArrayList<NodePair> pairs = new ArrayList<NodePair>();
            for (Pair<Node,Node> pair: way.getNodePairs(false /* don't sort */)) {
                pairs.add(new NodePair(pair));
                if (!directed) {
                    pairs.add(new NodePair(pair).swap());
                }
            }
            return pairs;
        }

        static public List<NodePair> buildNodePairs(List<Way> ways, boolean directed) {
            ArrayList<NodePair> pairs = new ArrayList<NodePair>();
            for (Way w: ways) {
                pairs.addAll(buildNodePairs(w, directed));
            }
            return pairs;
        }

        static public List<NodePair> eliminateDuplicateNodePairs(List<NodePair> pairs) {
            ArrayList<NodePair> cleaned = new ArrayList<NodePair>();
            for(NodePair p: pairs) {
                if (!cleaned.contains(p) && !cleaned.contains(p.swap())) {
                    cleaned.add(p);
                }
            }
            return cleaned;
        }

        static public NodeGraph createDirectedGraphFromNodePairs(List<NodePair> pairs) {
            NodeGraph graph = new NodeGraph();
            for (NodePair pair: pairs) {
                graph.add(pair);
            }
            return graph;
        }

        static public NodeGraph createDirectedGraphFromWays(Collection<Way> ways) {
            NodeGraph graph = new NodeGraph();
            for (Way w: ways) {
                graph.add(buildNodePairs(w, true /* directed */));
            }
            return graph;
        }

        static public NodeGraph createUndirectedGraphFromNodeList(List<NodePair> pairs) {
            NodeGraph graph = new NodeGraph();
            for (NodePair pair: pairs) {
                graph.add(pair);
                graph.add(pair.swap());
            }
            return graph;
        }

        static public NodeGraph createUndirectedGraphFromNodeWays(Collection<Way> ways) {
            NodeGraph graph = new NodeGraph();
            for (Way w: ways) {
                graph.add(buildNodePairs(w, false /* undirected */));
            }
            return graph;
        }

        private Set<NodePair> edges;
        private int numUndirectedEges = 0;

        protected void computeNumEdges() {
            Set<NodePair> undirectedEdges = new HashSet<NodePair>();
            for (NodePair pair: edges) {
                if (!undirectedEdges.contains(pair) && ! undirectedEdges.contains(pair.swap())) {
                    undirectedEdges.add(pair);
                }
            }
            numUndirectedEges = undirectedEdges.size();
        }

        public NodeGraph() {
            edges = new HashSet<NodePair>();
        }

        public void add(NodePair pair) {
            if (!edges.contains(pair)) {
                edges.add(pair);
            }
        }

        public void add(List<NodePair> pairs) {
            for (NodePair pair: pairs) {
                add(pair);
            }
        }

        protected Node getStartNode() {
            return edges.iterator().next().getA();
        }

        protected Set<Node> getNodes(Stack<NodePair> pairs) {
            HashSet<Node> nodes = new HashSet<Node>();
            for (NodePair pair: pairs) {
                nodes.add(pair.getA());
                nodes.add(pair.getB());
            }
            return nodes;
        }

        protected List<NodePair> getOutboundPairs(NodePair pair) {
            LinkedList<NodePair> outbound = new LinkedList<NodePair>();
            for (NodePair candidate:edges) {
                if (candidate.equals(pair)) {
                    continue;
                }
                if (candidate.isSuccessorOf(pair)) {
                    outbound.add(candidate);
                }
            }
            return outbound;
        }

        protected List<NodePair> getOutboundPairs(Node node) {
            LinkedList<NodePair> outbound = new LinkedList<NodePair>();
            for (NodePair candidate:edges) {
                if (candidate.getA() == node) {
                    outbound.add(candidate);
                }
            }
            return outbound;
        }

        protected Set<Node> getNodes() {
            Set<Node> nodes = new HashSet<Node>();
            for (NodePair pair: edges) {
                nodes.add(pair.getA());
                nodes.add(pair.getB());
            }
            return nodes;
        }

        protected boolean isSpanningWay(Stack<NodePair> way) {
            return numUndirectedEges == way.size();
        }


        protected boolean advance(Stack<NodePair> path) {
            // found a spanning path ?
            //
            if (isSpanningWay(path))
                return true;

            // advance with one of the possible follow up nodes
            //
            Stack<NodePair> nextPairs = new Stack<NodePair>();
            nextPairs.addAll(getOutboundPairs(path.peek()));
            while(!nextPairs.isEmpty()) {
                NodePair next = nextPairs.pop();
                if (path.contains(next) || path.contains(next.swap())) {
                    continue;
                }
                path.push(next);
                if (advance(path)) return true;
                path.pop();
            }
            return false;
        }

        protected List<Node> buildPathFromNodePairs(Stack<NodePair> path) {
            LinkedList<Node> ret = new LinkedList<Node>();
            for (NodePair pair: path) {
                ret.add(pair.getA());
            }
            ret.add(path.peek().getB());
            return ret;
        }

        protected List<Node> buildSpanningPath(Node startNode) {
            if (startNode == null)
                return null;
            Stack<NodePair> path = new Stack<NodePair>();
            // advance with one of the possible follow up nodes
            //
            Stack<NodePair> nextPairs  = new Stack<NodePair>();
            nextPairs.addAll(getOutboundPairs(startNode));
            while(!nextPairs.isEmpty()) {
                path.push(nextPairs.pop());
                if (advance(path))
                    return buildPathFromNodePairs(path);
                path.pop();
            }
            return null;
        }

        public List<Node> buildSpanningPath() {
            computeNumEdges();
            for (Node n : getNodes()) {
                List<Node> path = buildSpanningPath(n);
                if (path != null)
                    return path;
            }
            return null;
        }
    }
}
