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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * A directed or undirected graph of nodes. Nodes are connected via edges represented by NodePair instances.
 *
 * @since 12463 (extracted from CombineWayAction)
 */
public class NodeGraph {

    /**
     * Builds a list of pair of nodes from the given way.
     * @param way way
     * @param directed if {@code true} each pair of nodes will occur once, in the way nodes order.
     *                 if {@code false} each pair of nodes will occur twice (the pair and its inverse copy)
     * @return a list of pair of nodes from the given way
     */
    public static List<NodePair> buildNodePairs(Way way, boolean directed) {
        List<NodePair> pairs = new ArrayList<>();
        for (Pair<Node, Node> pair : way.getNodePairs(false)) {
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
     * @param directed if {@code true} each pair of nodes will occur once, in the way nodes order.<br>
     *                 if {@code false} each pair of nodes will occur twice (the pair and its inverse copy)
     * @return a list of pair of nodes from the given ways
     */
    public static List<NodePair> buildNodePairs(List<Way> ways, boolean directed) {
        List<NodePair> pairs = new ArrayList<>();
        for (Way w : ways) {
            pairs.addAll(buildNodePairs(w, directed));
        }
        return pairs;
    }

    /**
     * Builds a new list of pair nodes without the duplicated pairs (including inverse copies).
     * @param pairs existing list of pairs
     * @return a new list of pair nodes without the duplicated pairs
     */
    public static List<NodePair> eliminateDuplicateNodePairs(List<NodePair> pairs) {
        List<NodePair> cleaned = new ArrayList<>();
        for (NodePair p : pairs) {
            if (!cleaned.contains(p) && !cleaned.contains(p.swap())) {
                cleaned.add(p);
            }
        }
        return cleaned;
    }

    /**
     * Create a directed graph from the given node pairs.
     * @param pairs Node pairs to build the graph from
     * @return node graph structure
     */
    public static NodeGraph createDirectedGraphFromNodePairs(List<NodePair> pairs) {
        NodeGraph graph = new NodeGraph();
        for (NodePair pair : pairs) {
            graph.add(pair);
        }
        return graph;
    }

    /**
     * Create a directed graph from the given ways.
     * @param ways ways to build the graph from
     * @return node graph structure
     */
    public static NodeGraph createDirectedGraphFromWays(Collection<Way> ways) {
        NodeGraph graph = new NodeGraph();
        for (Way w : ways) {
            graph.add(buildNodePairs(w, true));
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
        for (NodePair pair : pairs) {
            graph.add(pair);
            graph.add(pair.swap());
        }
        return graph;
    }

    /**
     * Create an undirected graph from the given ways, but prevent reversing of all
     * non-new ways by fixing one direction.
     * @param ways Ways to build the graph from
     * @return node graph structure
     * @since 8181
     */
    public static NodeGraph createUndirectedGraphFromNodeWays(Collection<Way> ways) {
        NodeGraph graph = new NodeGraph();
        for (Way w : ways) {
            graph.add(buildNodePairs(w, false));
        }
        return graph;
    }

    /**
     * Create a nearly undirected graph from the given ways, but prevent reversing of all
     * non-new ways by fixing one direction.
     * The first new way gives the direction of the graph.
     * @param ways Ways to build the graph from
     * @return node graph structure
     */
    public static NodeGraph createNearlyUndirectedGraphFromNodeWays(Collection<Way> ways) {
        boolean dir = true;
        NodeGraph graph = new NodeGraph();
        for (Way w : ways) {
            if (!w.isNew()) {
                /* let the first non-new way give the direction (see #5880) */
                graph.add(buildNodePairs(w, dir));
                dir = false;
            } else {
                graph.add(buildNodePairs(w, false));
            }
        }
        return graph;
    }

    private final Set<NodePair> edges;
    private int numUndirectedEdges;
    /** The number of edges that were added. */
    private int addedEdges;
    private final Map<Node, List<NodePair>> successors = new LinkedHashMap<>();
    private final Map<Node, List<NodePair>> predecessors = new LinkedHashMap<>();

    /**
     * Constructs a lookup table from the existing edges in the graph to enable efficient querying.
     * This method creates a map where each node is associated with a list of nodes that are directly connected to it.
     *
     * @return A map representing the graph structure, where nodes are keys, and values are their direct successors.
     * @since 19062
     */
    public Map<Node, List<Node>> createMap() {
        final Map<Node, List<Node>> result = new HashMap<>(Utils.hashMapInitialCapacity(edges.size()));

        for (NodePair edge : edges) {
            result.computeIfAbsent(edge.getA(), k -> new ArrayList<>()).add(edge.getB());
        }

        return result;
    }

    /**
     * See {@link #prepare()}
     */
    protected void rememberSuccessor(NodePair pair) {
        List<NodePair> l = successors.computeIfAbsent(pair.getA(), k -> new ArrayList<>());
        if (!l.contains(pair)) {
            l.add(pair);
        }
    }

    /**
     * See {@link #prepare()}
     */
    protected void rememberPredecessors(NodePair pair) {
        List<NodePair> l = predecessors.computeIfAbsent(pair.getB(), k -> new ArrayList<>());
        if (!l.contains(pair)) {
            l.add(pair);
        }
    }

    /**
     * Replies true if {@code n} is a terminal node of the graph. Internal variables should be initialized first.
     * @param n Node to check
     * @return {@code true} if it is a terminal node
     * @see #prepare()
     */
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

        for (NodePair pair : edges) {
            if (!undirectedEdges.contains(pair) && !undirectedEdges.contains(pair.swap())) {
                undirectedEdges.add(pair);
            }
            rememberSuccessor(pair);
            rememberPredecessors(pair);
        }
        numUndirectedEdges = undirectedEdges.size();
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
     * @param pairs collection of node pairs
     */
    public void add(Iterable<NodePair> pairs) {
        for (NodePair pair : pairs) {
            add(pair);
        }
    }

    /**
     * Return the edges containing the node pairs of the graph.
     * @return the edges containing the node pairs of the graph
     */
    public Collection<NodePair> getEdges() {
        return Collections.unmodifiableSet(edges);
    }

    /**
     * Return the terminal nodes of the graph.
     * @return the terminal nodes of the graph
     */
    protected Set<Node> getTerminalNodes() {
        return getNodes().stream().filter(this::isTerminalNode).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<NodePair> getConnectedPairs(Node node) {
        List<NodePair> connected = new ArrayList<>();
        connected.addAll(Optional.ofNullable(successors.get(node)).orElseGet(Collections::emptyList));
        connected.addAll(Optional.ofNullable(predecessors.get(node)).orElseGet(Collections::emptyList));
        return connected;
    }

    protected List<NodePair> getOutboundPairs(NodePair pair) {
        return getOutboundPairs(pair.getB());
    }

    protected List<NodePair> getOutboundPairs(Node node) {
        return Optional.ofNullable(successors.get(node)).orElseGet(Collections::emptyList);
    }

    /**
     * Return the graph's nodes.
     * @return the graph's nodes
     */
    public Collection<Node> getNodes() {
        Set<Node> nodes = new LinkedHashSet<>(2 * edges.size());
        for (NodePair pair : edges) {
            nodes.add(pair.getA());
            nodes.add(pair.getB());
        }
        return nodes;
    }

    protected boolean isSpanningWay(Collection<NodePair> way) {
        return numUndirectedEdges == way.size();
    }

    protected List<Node> buildPathFromNodePairs(Deque<NodePair> path) {
        return Stream.concat(path.stream().map(NodePair::getA), Stream.of(path.peekLast().getB()))
                .collect(Collectors.toList());
    }

    /**
     * Tries to find a spanning path starting from node {@code startNode}.
     * <p>
     * Traverses the path in depth-first order.
     *
     * @param startNode the start node
     * @return the spanning path; empty list if no path is found
     */
    protected List<Node> buildSpanningPath(Node startNode) {
        if (startNode != null) {
            Deque<NodePair> path = new ArrayDeque<>();
            Set<NodePair> dupCheck = new HashSet<>();
            Deque<NodePair> nextPairs = new ArrayDeque<>(getOutboundPairs(startNode));
            while (!nextPairs.isEmpty()) {
                NodePair cur = nextPairs.removeLast();
                if (!dupCheck.contains(cur) && !dupCheck.contains(cur.swap())) {
                    while (!path.isEmpty() && !path.peekLast().isPredecessorOf(cur)) {
                        dupCheck.remove(path.removeLast());
                    }
                    path.addLast(cur);
                    dupCheck.add(cur);
                    if (isSpanningWay(path))
                        return buildPathFromNodePairs(path);
                    nextPairs.addAll(getOutboundPairs(path.peekLast()));
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Tries to find a path through the graph which visits each edge (i.e.
     * the segment of a way) exactly once.<p>
     * <b>Note that duplicated edges are removed first!</b>
     *
     * @return the path; {@code null}, if no path was found
     */
    public List<Node> buildSpanningPath() {
        prepare();
        if (numUndirectedEdges > 0 && isConnected()) {
            // Try to find a path from each "terminal node", i.e. from a
            // node which is connected by exactly one undirected edge (or
            // two directed edges in the opposite direction) to the graph. A
            // graph built up from way segments is likely to include such
            // nodes, unless the edges build one or more closed rings.
            // We order the nodes to start with the best candidates, but
            // it might take very long if there is no valid path as we iterate over all nodes
            // to find out.
            Set<Node> nodes = getTerminalNodes();
            nodes = nodes.isEmpty() ? getMostFrequentVisitedNodesFirst() : nodes;
            return nodes.stream()
                    .map(this::buildSpanningPath)
                    .filter(path -> !path.isEmpty())
                    .findFirst().orElse(null);
        }
        return null;
    }

    /**
     * Tries to find a path through the graph which visits each edge (i.e.
     * the segment of a way) exactly once. If the graph was build from overlapping
     * ways duplicate edges were removed already. This method will return null if
     * any duplicated edge was removed.
     *
     * @return List of nodes that build the path; an empty list if no path or duplicated edges were found
     * @since 15573 (return value not null)
     */
    public List<Node> buildSpanningPathNoRemove() {
        List<Node> path = null;
        if (edges.size() == addedEdges)
            path = buildSpanningPath();
        return path == null ? Collections.emptyList() : path;
    }

    /**
     * Find out if the graph is connected.
     * @return {@code true} if it is connected
     */
    private boolean isConnected() {
        Collection<Node> nodes = getNodes();
        if (nodes.isEmpty())
            return false;
        Deque<Node> toVisit = new ArrayDeque<>();
        HashSet<Node> visited = new HashSet<>();
        toVisit.add(nodes.iterator().next());
        while (!toVisit.isEmpty()) {
            Node n = toVisit.pop();
            if (!visited.contains(n)) {
                for (NodePair pair : getConnectedPairs(n)) {
                    if (n != pair.getA())
                        toVisit.addLast(pair.getA());
                    if (n != pair.getB())
                        toVisit.addLast(pair.getB());
                }
                visited.add(n);
            }
        }
        return nodes.size() == visited.size();
    }

    /**
     * Sort the nodes by number of appearances in the edges.
     * @return set of nodes which can be start nodes in a spanning way
     */
    private Set<Node> getMostFrequentVisitedNodesFirst() {
        if (edges.isEmpty())
            return Collections.emptySet();
        // count the appearance of nodes in edges
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
            sortedMap.computeIfAbsent(e.getValue(), x -> new LinkedHashSet<>()).add(e.getKey());
        }
        LinkedHashSet<Node> result = new LinkedHashSet<>();
        for (Entry<Integer, Set<Node>> e : sortedMap.entrySet()) {
            if (e.getKey() > 4 || result.isEmpty()) {
                result.addAll(e.getValue());
            }
        }
        return Collections.unmodifiableSet(result);
    }

}
