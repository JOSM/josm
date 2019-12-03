// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.openstreetmap.josm.tools.Pair;

/**
 * A directed or undirected graph of nodes.
 * @since 12463 (extracted from CombineWayAction)
 */
public class NodeGraph {

    /**
     * Builds a list of pair of nodes from the given way.
     * @param way way
     * @param directed if {@code true} each pair of nodes will occur once, in the way nodes order.
     *                 if {@code false} each pair of nodes will occur twice (the pair and its inversed copy)
     * @return a list of pair of nodes from the given way
     */
    public static List<NodePair> buildNodePairs(Way way, boolean directed) {
        List<NodePair> pairs = new ArrayList<>();
        for (Pair<Node, Node> pair: way.getNodePairs(false /* don't sort */)) {
            pairs.add(new NodePair(pair));
            if (!directed) {
                pairs.add(new NodePair(pair).swap());
            }
        }
        return pairs;
    }

    /**
     * Builds a list of pair of nodes from the given ways.
     * @param ways ways
     * @param directed if {@code true} each pair of nodes will occur once, in the way nodes order.
     *                 if {@code false} each pair of nodes will occur twice (the pair and its inversed copy)
     * @return a list of pair of nodes from the given ways
     */
    public static List<NodePair> buildNodePairs(List<Way> ways, boolean directed) {
        List<NodePair> pairs = new ArrayList<>();
        for (Way w: ways) {
            pairs.addAll(buildNodePairs(w, directed));
        }
        return pairs;
    }

    /**
     * Builds a new list of pair nodes without the duplicated pairs (including inversed copies).
     * @param pairs existing list of pairs
     * @return a new list of pair nodes without the duplicated pairs
     */
    public static List<NodePair> eliminateDuplicateNodePairs(List<NodePair> pairs) {
        List<NodePair> cleaned = new ArrayList<>();
        for (NodePair p: pairs) {
            if (!cleaned.contains(p) && !cleaned.contains(p.swap())) {
                cleaned.add(p);
            }
        }
        return cleaned;
    }

    public static NodeGraph createDirectedGraphFromNodePairs(List<NodePair> pairs) {
        NodeGraph graph = new NodeGraph();
        for (NodePair pair: pairs) {
            graph.add(pair);
        }
        return graph;
    }

    public static NodeGraph createDirectedGraphFromWays(Collection<Way> ways) {
        NodeGraph graph = new NodeGraph();
        for (Way w: ways) {
            graph.add(buildNodePairs(w, true /* directed */));
        }
        return graph;
    }

    /**
     * Create an undirected graph from the given node pairs.
     * @param pairs Node pairs to build the graph from
     * @return node graph structure
     */
    public static NodeGraph createUndirectedGraphFromNodeList(List<NodePair> pairs) {
        NodeGraph graph = new NodeGraph();
        for (NodePair pair: pairs) {
            graph.add(pair);
            graph.add(pair.swap());
        }
        return graph;
    }

    /**
     * Create an undirected graph from the given ways, but prevent reversing of all
     * non-new ways by fix one direction.
     * @param ways Ways to build the graph from
     * @return node graph structure
     * @since 8181
     */
    public static NodeGraph createUndirectedGraphFromNodeWays(Collection<Way> ways) {
        NodeGraph graph = new NodeGraph();
        for (Way w: ways) {
            graph.add(buildNodePairs(w, false /* undirected */));
        }
        return graph;
    }

    public static NodeGraph createNearlyUndirectedGraphFromNodeWays(Collection<Way> ways) {
        boolean dir = true;
        NodeGraph graph = new NodeGraph();
        for (Way w: ways) {
            if (!w.isNew()) {
                /* let the first non-new way give the direction (see #5880) */
                graph.add(buildNodePairs(w, dir));
                dir = false;
            } else {
                graph.add(buildNodePairs(w, false /* undirected */));
            }
        }
        return graph;
    }

    private final Set<NodePair> edges;
    private int numUndirectedEges;
    /** counts the number of edges that were added */
    private int addedEdges;
    private final Map<Node, List<NodePair>> successors = new LinkedHashMap<>();
    private final Map<Node, List<NodePair>> predecessors = new LinkedHashMap<>();

    protected void rememberSuccessor(NodePair pair) {
        List<NodePair> l = successors.computeIfAbsent(pair.getA(), k -> new ArrayList<>());
        if (!l.contains(pair)) {
            l.add(pair);
        }
    }

    protected void rememberPredecessors(NodePair pair) {
        List<NodePair> l = predecessors.computeIfAbsent(pair.getB(), k -> new ArrayList<>());
        if (!l.contains(pair)) {
            l.add(pair);
        }
    }

    protected boolean isTerminalNode(Node n) {
        if (successors.get(n) == null) return false;
        if (successors.get(n).size() != 1) return false;
        if (predecessors.get(n) == null) return true;
        if (predecessors.get(n).size() == 1) {
            NodePair p1 = successors.get(n).get(0);
            NodePair p2 = predecessors.get(n).get(0);
            return p1.equals(p2.swap());
        }
        return false;
    }

    protected void prepare() {
        Set<NodePair> undirectedEdges = new LinkedHashSet<>();
        successors.clear();
        predecessors.clear();

        for (NodePair pair: edges) {
            if (!undirectedEdges.contains(pair) && !undirectedEdges.contains(pair.swap())) {
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
        edges = new LinkedHashSet<>();
    }

    /**
     * Add a node pair.
     * @param pair node pair
     */
    public void add(NodePair pair) {
        addedEdges++;
        edges.add(pair);
    }

    /**
     * Add a list of node pairs.
     * @param pairs list of node pairs
     */
    public void add(Collection<NodePair> pairs) {
        for (NodePair pair: pairs) {
            add(pair);
        }
    }

    protected Set<Node> getTerminalNodes() {
        Set<Node> ret = new LinkedHashSet<>();
        for (Node n: getNodes()) {
            if (isTerminalNode(n)) {
                ret.add(n);
            }
        }
        return ret;
    }

    protected List<NodePair> getOutboundPairs(NodePair pair) {
        return getOutboundPairs(pair.getB());
    }

    protected List<NodePair> getOutboundPairs(Node node) {
        return Optional.ofNullable(successors.get(node)).orElseGet(Collections::emptyList);
    }

    protected Set<Node> getNodes() {
        Set<Node> nodes = new LinkedHashSet<>(2 * edges.size());
        for (NodePair pair: edges) {
            nodes.add(pair.getA());
            nodes.add(pair.getB());
        }
        return nodes;
    }

    protected boolean isSpanningWay(Collection<NodePair> way) {
        return numUndirectedEges == way.size();
    }

    protected List<Node> buildPathFromNodePairs(Stack<NodePair> path) {
        List<Node> ret = new LinkedList<>();
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
        if (startNode != null) {
            // do not simply replace `Stack` by `ArrayDeque` because of different iteration behaviour, see #11957
            Stack<NodePair> path = new Stack<>();
            Set<NodePair> dupCheck = new HashSet<>();
            Stack<NodePair> nextPairs = new Stack<>();
            nextPairs.addAll(getOutboundPairs(startNode));
            while (!nextPairs.isEmpty()) {
                NodePair cur = nextPairs.pop();
                if (!dupCheck.contains(cur) && !dupCheck.contains(cur.swap())) {
                    while (!path.isEmpty() && !path.peek().isPredecessorOf(cur)) {
                        dupCheck.remove(path.pop());
                    }
                    path.push(cur);
                    dupCheck.add(cur);
                    if (isSpanningWay(path)) return buildPathFromNodePairs(path);
                    nextPairs.addAll(getOutboundPairs(path.peek()));
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Tries to find a path through the graph which visits each edge (i.e.
     * the segment of a way) exactly once.
     * <p><b>Note that duplicated edges are removed first!</b>
     *
     * @return the path; null, if no path was found
     */
    public List<Node> buildSpanningPath() {
        prepare();
        if (numUndirectedEges > 0 && isConnected()) {
            // try to find a path from each "terminal node", i.e. from a
            // node which is connected by exactly one undirected edges (or
            // two directed edges in opposite direction) to the graph. A
            // graph built up from way segments is likely to include such
            // nodes, unless the edges build one or more closed rings.
            // We order the nodes to start with the best candidates, but
            // it might take very long if there is no valid path as we iterate over all nodes
            // to find out.
            Set<Node> nodes = getTerminalNodes();
            nodes = nodes.isEmpty() ? getMostFrequentVisitedNodesFirst() : nodes;
            for (Node n : nodes) {
                List<Node> path = buildSpanningPath(n);
                if (!path.isEmpty())
                    return path;
            }
        }
        return null;
    }

    /**
     * Tries to find a path through the graph which visits each edge (i.e.
     * the segment of a way) exactly once. If the graph was build from overlapping
     * ways duplicate edges were removed already. This method will return null if
     * any duplicated edge was removed.
     *
     * @return the path; null, if no path was found or duplicated edges were found
     * @since 15555
     */
    public List<Node> buildSpanningPathNoRemove() {
        if (edges.size() != addedEdges)
            return null;
        return buildSpanningPath();
    }

    /**
     * Find out if the graph is connected.
     * @return true if it is connected.
     */
    private boolean isConnected() {
        Set<Node> nodes = getNodes();
        if (nodes.isEmpty())
            return false;
        Deque<Node> toVisit = new ArrayDeque<>();
        HashSet<Node> visited = new HashSet<>();
        toVisit.add(nodes.iterator().next());
        while (!toVisit.isEmpty()) {
            Node n = toVisit.pop();
            if (!visited.contains(n)) {
                List<NodePair> neighbours = getOutboundPairs(n);
                for (NodePair pair : neighbours) {
                    toVisit.addLast(pair.getA());
                    toVisit.addLast(pair.getB());
                }
                visited.add(n);
            }
        }
        return nodes.size() == visited.size();
    }

    /**
     * Sort the nodes by number of appearances in the edges.
     * @return set of nodes which can be start nodes in a spanning way.
     */
    private Set<Node> getMostFrequentVisitedNodesFirst() {
        if (edges.isEmpty())
            return Collections.emptySet();
        // count appearance of nodes in edges
        Map<Node, Integer> counters = new HashMap<>();
        for (NodePair pair : edges) {
            Integer c = counters.get(pair.getA());
            counters.put(pair.getA(), c == null ? 1 : c + 1);
            c = counters.get(pair.getB());
            counters.put(pair.getB(), c == null ? 1 : c + 1);
        }
        // group by counters
        TreeMap<Integer, Set<Node>> sortedMap = new TreeMap<>(Comparator.reverseOrder());
        for (Entry<Node, Integer> e : counters.entrySet()) {
            sortedMap.computeIfAbsent(e.getValue(), LinkedHashSet::new).add(e.getKey());
        }
        LinkedHashSet<Node> result = new LinkedHashSet<>();
        for (Entry<Integer, Set<Node>> e : sortedMap.entrySet()) {
            if (e.getKey() > 4 || result.isEmpty()) {
                result.addAll(e.getValue());
            }
        }
        return result;
    }

}
