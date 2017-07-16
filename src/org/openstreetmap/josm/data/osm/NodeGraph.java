// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

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
    private final Map<Node, List<NodePair>> successors = new LinkedHashMap<>();
    private final Map<Node, List<NodePair>> predecessors = new LinkedHashMap<>();

    protected void rememberSuccessor(NodePair pair) {
        if (successors.containsKey(pair.getA())) {
            if (!successors.get(pair.getA()).contains(pair)) {
                successors.get(pair.getA()).add(pair);
            }
        } else {
            List<NodePair> l = new ArrayList<>();
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
            List<NodePair> l = new ArrayList<>();
            l.add(pair);
            predecessors.put(pair.getB(), l);
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
        if (!edges.contains(pair)) {
            edges.add(pair);
        }
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

    protected boolean isSpanningWay(Stack<NodePair> way) {
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
            Stack<NodePair> path = new Stack<>();
            Stack<NodePair> nextPairs = new Stack<>();
            nextPairs.addAll(getOutboundPairs(startNode));
            while (!nextPairs.isEmpty()) {
                NodePair cur = nextPairs.pop();
                if (!path.contains(cur) && !path.contains(cur.swap())) {
                    while (!path.isEmpty() && !path.peek().isPredecessorOf(cur)) {
                        path.pop();
                    }
                    path.push(cur);
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
        // In the worst case this loops over all nodes which is very slow for large ways.
        //
        Set<Node> nodes = getTerminalNodes();
        nodes = nodes.isEmpty() ? getNodes() : nodes;
        for (Node n: nodes) {
            List<Node> path = buildSpanningPath(n);
            if (!path.isEmpty())
                return path;
        }
        return null;
    }
}
