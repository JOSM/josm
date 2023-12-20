// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.algorithms;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeGraph;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tarjan's strongly connected components algorithm for JOSM.
 *
 * @author gaben
 * @see <a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">
 * Tarjan's strongly connected components algorithm</a>
 * @since xxx
 */
public final class Tarjan {

    /**
     * Used to remember visited nodes and its metadata. Key is used for storing
     * the unique ID of the nodes instead of the full data to save space.
     */
    private final Map<Long, TarjanHelper> registry;

    /** Used to store the graph data as a map. */
    private final Map<Node, List<Node>> graphMap;

    /** Used to store strongly connected components. NOTE: single nodes are not stored to save memory. */
    private final Collection<List<Node>> scc = new ArrayList<>();

    /** Used on algorithm runtime to keep track discovery progress. */
    private final Deque<Node> stack = new ArrayDeque<>();

    /** Used on algorithm runtime to keep track discovery progress. */
    private int index;

    /**
     * Initialize the Tarjan's algorithm.
     *
     * @param graph graph data in NodeGraph object format
     */
    public Tarjan(NodeGraph graph) {
        graphMap = graph.createMap();

        this.registry = new HashMap<>(Utils.hashMapInitialCapacity(graph.getEdges().size()));
    }

    /**
     * Returns the strongly connected components in the current graph. Single nodes are ignored to save memory.
     *
     * @return the strongly connected components in the current graph
     */
    public Collection<List<Node>> getSCC() {
        for (Node node : graphMap.keySet()) {
            if (!registry.containsKey(node.getUniqueId())) {
                strongConnect(node);
            }
        }
        return scc;
    }

    /**
     * Returns the graph data as a map.
     *
     * @return the graph data as a map
     * @see NodeGraph#createMap()
     */
    public Map<Node, List<Node>> getGraphMap() {
        return graphMap;
    }

    /**
     * Calculates strongly connected components available from the given node, in an iterative fashion.
     *
     * @param u0 the node to generate strongly connected components from
     */
    private void strongConnect(final Node u0) {
        final Deque<Pair<Node, Integer>> work = new ArrayDeque<>();
        work.push(new Pair<>(u0, 0));
        boolean recurse;

        while (!work.isEmpty()) {
            Pair<Node, Integer> popped = work.remove();
            Node u = popped.a;
            int j = popped.b;

            if (j == 0) {
                index++;
                registry.put(u.getUniqueId(), new TarjanHelper(index));
                stack.push(u);
            }

            recurse = false;
            List<Node> successors = getSuccessors(u);

            for (int i = j; i < successors.size(); i++) {
                Node v = successors.get(i);
                if (!registry.containsKey(v.getUniqueId())) {
                    work.push(new Pair<>(u, i + 1));
                    work.push(new Pair<>(v, 0));
                    recurse = true;
                    break;
                } else if (stack.contains(v)) {
                    TarjanHelper uHelper = registry.get(u.getUniqueId());
                    TarjanHelper vHelper = registry.get(v.getUniqueId());
                    uHelper.lowlink = Math.min(uHelper.lowlink, vHelper.index);
                }
            }

            if (!recurse) {
                TarjanHelper uHelper = registry.get(u.getUniqueId());
                if (uHelper.lowlink == uHelper.index) {
                    List<Node> currentSCC = new ArrayList<>();
                    Node v;
                    do {
                        v = stack.remove();
                        currentSCC.add(v);
                    } while (!v.equals(u));

                    // store the component only if it makes a cycle, otherwise it's a waste of memory
                    if (currentSCC.size() > 1) {
                        scc.add(currentSCC);
                    }
                }
                if (!work.isEmpty()) {
                    Node v = u;
                    Pair<Node, Integer> peeked = work.peek();
                    u = peeked.a;
                    TarjanHelper vHelper = registry.get(v.getUniqueId());
                    uHelper = registry.get(u.getUniqueId());
                    uHelper.lowlink = Math.min(uHelper.lowlink, vHelper.lowlink);
                }
            }
        }
    }

    /**
     * Returns the next direct successors from the graph of the given node.
     *
     * @param node a node to start search from
     * @return direct successors of the node or an empty list, if it's a terminal node
     */
    private List<Node> getSuccessors(Node node) {
        return graphMap.getOrDefault(node, Collections.emptyList());
    }

    /**
     * Helper class for storing the Tarjan algorithm runtime metadata.
     */
    private static final class TarjanHelper {
        private final int index;
        private int lowlink;

        private TarjanHelper(int index) {
            this.index = index;
            this.lowlink = index;
        }
    }
}
