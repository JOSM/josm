// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.corrector.ReverseWayTagCorrector;
import org.openstreetmap.josm.corrector.UserCancelException;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Combines multiple ways into one.
 * @since 213
 */
public class CombineWayAction extends JosmAction {

    private static final BooleanProperty PROP_REVERSE_WAY = new BooleanProperty("tag-correction.reverse-way", true);

    /**
     * Constructs a new {@code CombineWayAction}.
     */
    public CombineWayAction() {
        super(tr("Combine Way"), "combineway", tr("Combine several ways into one."),
                Shortcut.registerShortcut("tools:combineway", tr("Tool: {0}", tr("Combine Way")), KeyEvent.VK_C, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/CombineWay"));
    }

    protected static boolean confirmChangeDirectionOfWays() {
        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Change directions?"),
                new String[] {tr("Reverse and Combine"), tr("Cancel")});
        ed.setButtonIcons(new String[] {"wayflip.png", "cancel.png"});
        ed.setContent(tr("The ways can not be combined in their current directions.  "
                + "Do you want to reverse some of them?"));
        ed.toggleEnable("combineway-reverse");
        ed.showDialog();
        return ed.getValue() == 1;
    }

    protected static void warnCombiningImpossible() {
        String msg = tr("Could not combine ways<br>"
                + "(They could not be merged into a single string of nodes)");
        new Notification(msg)
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .show();
        return;
    }

    protected static Way getTargetWay(Collection<Way> combinedWays) {
        // init with an arbitrary way
        Way targetWay = combinedWays.iterator().next();

        // look for the first way already existing on
        // the server
        for (Way w : combinedWays) {
            targetWay = w;
            if (!w.isNew()) {
                break;
            }
        }
        return targetWay;
    }

    /**
     * @param ways
     * @return null if ways cannot be combined. Otherwise returns the combined
     *              ways and the commands to combine
     * @throws UserCancelException
     */
    public static Pair<Way, Command> combineWaysWorker(Collection<Way> ways) throws UserCancelException {

        // prepare and clean the list of ways to combine
        //
        if (ways == null || ways.isEmpty())
            return null;
        ways.remove(null); // just in case -  remove all null ways from the collection

        // remove duplicates, preserving order
        ways = new LinkedHashSet<Way>(ways);

        // try to build a new way which includes all the combined
        // ways
        //
        NodeGraph graph = NodeGraph.createUndirectedGraphFromNodeWays(ways);
        List<Node> path = graph.buildSpanningPath();
        if (path == null) {
            warnCombiningImpossible();
            return null;
        }
        // check whether any ways have been reversed in the process
        // and build the collection of tags used by the ways to combine
        //
        TagCollection wayTags = TagCollection.unionOfAllPrimitives(ways);

        List<Way> reversedWays = new LinkedList<Way>();
        List<Way> unreversedWays = new LinkedList<Way>();
        for (Way w: ways) {
            // Treat zero or one-node ways as unreversed as Combine action action is a good way to fix them (see #8971)
            if (w.getNodesCount() < 2 || (path.indexOf(w.getNode(0)) + 1) == path.lastIndexOf(w.getNode(1))) {
                unreversedWays.add(w);
            } else {
                reversedWays.add(w);
            }
        }
        // reverse path if all ways have been reversed
        if (unreversedWays.isEmpty()) {
            Collections.reverse(path);
            unreversedWays = reversedWays;
            reversedWays = null;
        }
        if ((reversedWays != null) && !reversedWays.isEmpty()) {
            if (!confirmChangeDirectionOfWays()) return null;
            // filter out ways that have no direction-dependent tags
            unreversedWays = ReverseWayTagCorrector.irreversibleWays(unreversedWays);
            reversedWays = ReverseWayTagCorrector.irreversibleWays(reversedWays);
            // reverse path if there are more reversed than unreversed ways with direction-dependent tags
            if (reversedWays.size() > unreversedWays.size()) {
                Collections.reverse(path);
                List<Way> tempWays = unreversedWays;
                unreversedWays = reversedWays;
                reversedWays = tempWays;
            }
            // if there are still reversed ways with direction-dependent tags, reverse their tags
            if (!reversedWays.isEmpty() && PROP_REVERSE_WAY.get()) {
                List<Way> unreversedTagWays = new ArrayList<Way>(ways);
                unreversedTagWays.removeAll(reversedWays);
                ReverseWayTagCorrector reverseWayTagCorrector = new ReverseWayTagCorrector();
                List<Way> reversedTagWays = new ArrayList<Way>(reversedWays.size());
                Collection<Command> changePropertyCommands =  null;
                for (Way w : reversedWays) {
                    Way wnew = new Way(w);
                    reversedTagWays.add(wnew);
                    changePropertyCommands = reverseWayTagCorrector.execute(w, wnew);
                }
                if ((changePropertyCommands != null) && !changePropertyCommands.isEmpty()) {
                    for (Command c : changePropertyCommands) {
                        c.executeCommand();
                    }
                }
                wayTags = TagCollection.unionOfAllPrimitives(reversedTagWays);
                wayTags.add(TagCollection.unionOfAllPrimitives(unreversedTagWays));
            }
        }

        // create the new way and apply the new node list
        //
        Way targetWay = getTargetWay(ways);
        Way modifiedTargetWay = new Way(targetWay);
        modifiedTargetWay.setNodes(path);

        List<Command> resolution = CombinePrimitiveResolverDialog.launchIfNecessary(wayTags, ways, Collections.singleton(targetWay));

        LinkedList<Command> cmds = new LinkedList<Command>();
        LinkedList<Way> deletedWays = new LinkedList<Way>(ways);
        deletedWays.remove(targetWay);

        cmds.add(new ChangeCommand(targetWay, modifiedTargetWay));
        cmds.addAll(resolution);
        cmds.add(new DeleteCommand(deletedWays));
        final SequenceCommand sequenceCommand = new SequenceCommand(/* for correct i18n of plural forms - see #9110 */
                trn("Combine {0} way", "Combine {0} ways", ways.size(), ways.size()), cmds);

        return new Pair<Way, Command>(targetWay, sequenceCommand);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (getCurrentDataSet() == null)
            return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        Set<Way> selectedWays = OsmPrimitive.getFilteredSet(selection, Way.class);
        if (selectedWays.size() < 2) {
            new Notification(
                    tr("Please select at least two ways to combine."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }
        // combine and update gui
        Pair<Way, Command> combineResult;
        try {
            combineResult = combineWaysWorker(selectedWays);
        } catch (UserCancelException ex) {
            return;
        }

        if (combineResult == null)
            return;
        final Way selectedWay = combineResult.a;
        Main.main.undoRedo.add(combineResult.b);
        if(selectedWay != null)
        {
            Runnable guiTask = new Runnable() {
                @Override
                public void run() {
                    getCurrentDataSet().setSelected(selectedWay);
                }
            };
            GuiHelper.runInEDT(guiTask);
        }
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
            return;
        }
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        updateEnabledState(selection);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        int numWays = 0;
        for (OsmPrimitive osm : selection)
            if (osm instanceof Way) {
                numWays++;
            }
        setEnabled(numWays >= 2);
    }

    /**
     * A pair of nodes.
     */
    static public class NodePair {
        private final Node a;
        private final Node b;
        
        /**
         * Constructs a new {@code NodePair}.
         * @param a The first node
         * @param b The second node
         */
        public NodePair(Node a, Node b) {
            this.a = a;
            this.b = b;
        }

        /**
         * Constructs a new {@code NodePair}.
         * @param pair An existing {@code Pair} of nodes
         */
        public NodePair(Pair<Node,Node> pair) {
            this(pair.a, pair.b);
        }

        /**
         * Constructs a new {@code NodePair}.
         * @param other An existing {@code NodePair}
         */
        public NodePair(NodePair other) {
            this(other.a, other.b);
        }

        /**
         * Replies the first node.
         * @return The first node
         */
        public Node getA() {
            return a;
        }

        /**
         * Replies the second node
         * @return The second node
         */
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

        /**
         * Determines if this pair contains the given node.
         * @param n The node to look for
         * @return {@code true} if {@code n} is in the pair, {@code false} otherwise
         */
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
            List<NodePair> pairs = new ArrayList<NodePair>();
            for (Pair<Node,Node> pair: way.getNodePairs(false /* don't sort */)) {
                pairs.add(new NodePair(pair));
                if (!directed) {
                    pairs.add(new NodePair(pair).swap());
                }
            }
            return pairs;
        }

        static public List<NodePair> buildNodePairs(List<Way> ways, boolean directed) {
            List<NodePair> pairs = new ArrayList<NodePair>();
            for (Way w: ways) {
                pairs.addAll(buildNodePairs(w, directed));
            }
            return pairs;
        }

        static public List<NodePair> eliminateDuplicateNodePairs(List<NodePair> pairs) {
            List<NodePair> cleaned = new ArrayList<NodePair>();
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

        public static NodeGraph createUndirectedGraphFromNodeWays(Collection<Way> ways) {
            NodeGraph graph = new NodeGraph();
            for (Way w: ways) {
                graph.add(buildNodePairs(w, false /* undirected */));
            }
            return graph;
        }

        private Set<NodePair> edges;
        private int numUndirectedEges = 0;
        private Map<Node, List<NodePair>> successors;
        private Map<Node, List<NodePair>> predecessors;

        protected void rememberSuccessor(NodePair pair) {
            if (successors.containsKey(pair.getA())) {
                if (!successors.get(pair.getA()).contains(pair)) {
                    successors.get(pair.getA()).add(pair);
                }
            } else {
                List<NodePair> l = new ArrayList<NodePair>();
                l.add(pair);
                successors.put(pair.getA(), l);
            }
        }

        protected void rememberPredecessors(NodePair pair) {
            if (predecessors.containsKey(pair.getB())) {
                if (!predecessors.get(pair.getB()).contains(pair)) {
                    predecessors.get(pair.getB()).add(pair);
                }
            } else {
                List<NodePair> l = new ArrayList<NodePair>();
                l.add(pair);
                predecessors.put(pair.getB(), l);
            }
        }

        protected boolean isTerminalNode(Node n) {
            if (successors.get(n) == null) return false;
            if (successors.get(n).size() != 1) return false;
            if (predecessors.get(n) == null) return true;
            if (predecessors.get(n).size() == 1) {
                NodePair p1 = successors.get(n).iterator().next();
                NodePair p2 = predecessors.get(n).iterator().next();
                return p1.equals(p2.swap());
            }
            return false;
        }

        protected void prepare() {
            Set<NodePair> undirectedEdges = new LinkedHashSet<NodePair>();
            successors = new LinkedHashMap<Node, List<NodePair>>();
            predecessors = new LinkedHashMap<Node, List<NodePair>>();

            for (NodePair pair: edges) {
                if (!undirectedEdges.contains(pair) && ! undirectedEdges.contains(pair.swap())) {
                    undirectedEdges.add(pair);
                }
                rememberSuccessor(pair);
                rememberPredecessors(pair);
            }
            numUndirectedEges = undirectedEdges.size();
        }

        /**
         * Constructs a new {@code NodeGraph}.
         */
        public NodeGraph() {
            edges = new LinkedHashSet<NodePair>();
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
            Set<Node> nodes = getNodes();
            for (Node n: nodes) {
                if (successors.get(n) != null && successors.get(n).size() ==1)
                    return n;
            }
            return null;
        }

        protected Set<Node> getTerminalNodes() {
            Set<Node> ret = new LinkedHashSet<Node>();
            for (Node n: getNodes()) {
                if (isTerminalNode(n)) {
                    ret.add(n);
                }
            }
            return ret;
        }

        protected Set<Node> getNodes(Stack<NodePair> pairs) {
            HashSet<Node> nodes = new LinkedHashSet<Node>(2*pairs.size());
            for (NodePair pair: pairs) {
                nodes.add(pair.getA());
                nodes.add(pair.getB());
            }
            return nodes;
        }

        protected List<NodePair> getOutboundPairs(NodePair pair) {
            return getOutboundPairs(pair.getB());
        }

        protected List<NodePair> getOutboundPairs(Node node) {
            List<NodePair> l = successors.get(node);
            if (l == null)
                return Collections.emptyList();
            return l;
        }

        protected Set<Node> getNodes() {
            Set<Node> nodes = new LinkedHashSet<Node>(2 * edges.size());
            for (NodePair pair: edges) {
                nodes.add(pair.getA());
                nodes.add(pair.getB());
            }
            return nodes;
        }

        protected boolean isSpanningWay(Stack<NodePair> way) {
            return numUndirectedEges == way.size();
        }

        protected List<Node> buildPathFromNodePairs(Stack<NodePair> path) {
            LinkedList<Node> ret = new LinkedList<Node>();
            for (NodePair pair: path) {
                ret.add(pair.getA());
            }
            ret.add(path.peek().getB());
            return ret;
        }

        /**
         * Tries to find a spanning path starting from node <code>startNode</code>.
         *
         * Traverses the path in depth-first order.
         *
         * @param startNode the start node
         * @return the spanning path; null, if no path is found
         */
        protected List<Node> buildSpanningPath(Node startNode) {
            if (startNode == null)
                return null;
            Stack<NodePair> path = new Stack<NodePair>();
            Stack<NodePair> nextPairs  = new Stack<NodePair>();
            nextPairs.addAll(getOutboundPairs(startNode));
            while(!nextPairs.isEmpty()) {
                NodePair cur= nextPairs.pop();
                if (! path.contains(cur) && ! path.contains(cur.swap())) {
                    while(!path.isEmpty() && !path.peek().isPredecessorOf(cur)) {
                        path.pop();
                    }
                    path.push(cur);
                    if (isSpanningWay(path)) return buildPathFromNodePairs(path);
                    nextPairs.addAll(getOutboundPairs(path.peek()));
                }
            }
            return null;
        }

        /**
         * Tries to find a path through the graph which visits each edge (i.e.
         * the segment of a way) exactly one.
         *
         * @return the path; null, if no path was found
         */
        public List<Node> buildSpanningPath() {
            prepare();
            // try to find a path from each "terminal node", i.e. from a
            // node which is connected by exactly one undirected edges (or
            // two directed edges in opposite direction) to the graph. A
            // graph built up from way segments is likely to include such
            // nodes, unless all ways are closed.
            // In the worst case this loops over all nodes which is
            // very slow for large ways.
            //
            Set<Node> nodes = getTerminalNodes();
            nodes = nodes.isEmpty() ? getNodes() : nodes;
            for (Node n: nodes) {
                List<Node> path = buildSpanningPath(n);
                if (path != null)
                    return path;
            }
            return null;
        }
    }
}
