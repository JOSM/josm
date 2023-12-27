// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeGraph;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.algorithms.Tarjan;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Pair;

/**
 * Test for detecting <a href="https://en.wikipedia.org/wiki/Cycle_(graph_theory)">cycles</a> in a directed graph,
 * currently used for waterways only. The processed graph consists of ways labeled as waterway.
 *
 * @author gaben
 * @since xxx
 */
public class CycleDetector extends Test {
    public static final int CYCLE_DETECTED = 4200;

    /** All waterways for cycle detection */
    private final Set<Way> usableWaterways = new HashSet<>();

    /** Already visited primitive unique IDs */
    private final Set<Long> visitedWays = new HashSet<>();

    /** Currently used directional waterways from the OSM wiki */
    private List<String> directionalWaterways;

    protected static final String PREFIX = ValidatorPrefHelper.PREFIX + "." + CycleDetector.class.getSimpleName();

    public CycleDetector() {
        super(tr("Cycle detector"), tr("Detects cycles in drainage systems."));
    }

    @Override
    public boolean isPrimitiveUsable(OsmPrimitive p) {
        return p.isUsable() && (p instanceof Way) && (((Way) p).getNodesCount() > 1) && p.hasTag("waterway", directionalWaterways);
    }

    @Override
    public void visit(Way w) {
        if (isPrimitiveUsable(w))
            usableWaterways.add(w);
    }

    @Override
    public void startTest(ProgressMonitor progressMonitor) {
        super.startTest(progressMonitor);
        directionalWaterways = Config.getPref().getList(PREFIX + ".directionalWaterways",
            Arrays.asList("river", "stream", "tidal_channel", "drain", "ditch", "fish_pass", "fairway"));
    }

    @Override
    public void endTest() {
        for (Collection<Way> graph : getGraphs()) {
            NodeGraph nodeGraph = NodeGraph.createDirectedGraphFromWays(graph);
            Tarjan tarjan = new Tarjan(nodeGraph);
            Collection<List<Node>> scc = tarjan.getSCC();
            Map<Node, List<Node>> graphMap = tarjan.getGraphMap();

            for (Collection<Node> possibleCycle : scc) {
                // there is a cycle in the graph if a strongly connected component has more than one node
                if (possibleCycle.size() > 1) {
                    errors.add(
                        TestError.builder(this, Severity.ERROR, CYCLE_DETECTED)
                            .message(trc("graph theory", "Cycle in directional waterway network"))
                            .primitives(possibleCycle)
                            .highlightWaySegments(createSegments(graphMap, possibleCycle))
                            .build()
                    );
                }
            }
        }

        super.endTest();
    }

    @Override
    public void clear() {
        super.clear();
        usableWaterways.clear();
        visitedWays.clear();
    }

    /**
     * Creates WaySegments from Nodes for the error highlight function.
     *
     * @param graphMap the complete graph data
     * @param nodes    nodes to build the way segments from
     * @return WaySegments from the Nodes
     */
    private static Collection<WaySegment> createSegments(Map<Node, List<Node>> graphMap, Collection<Node> nodes) {
        List<Pair<Node, Node>> pairs = new ArrayList<>();

        // build new graph exclusively from SCC nodes
        for (Node node : nodes) {
            for (Node successor : graphMap.get(node)) {
                // check for outbound nodes
                if (nodes.contains(successor)) {
                    pairs.add(new Pair<>(node, successor));
                }
            }
        }

        Collection<WaySegment> segments = new ArrayList<>();

        for (Pair<Node, Node> pair : pairs) {
            final Node n = pair.a;
            final Node m = pair.b;

            if (n != null && m != null && !n.equals(m)) {
                List<Way> intersect = new ArrayList<>(n.getParentWays());
                List<Way> mWays = m.getParentWays();
                intersect.retainAll(mWays);

                for (Way w : intersect) {
                    if (w.getNeighbours(n).contains(m) && getNodeIndex(w, n) + 1 == getNodeIndex(w, m)) {
                        segments.add(WaySegment.forNodePair(w, n, m));
                    }
                }
            }
        }

        return segments;
    }

    /**
     * Returns the way index of a node. Only the first occurrence is considered in case it's a closed way.
     *
     * @param w parent way
     * @param n the node to look up
     * @return {@code >=0} if the node is found or<br>{@code -1} if node not part of the way
     */
    private static int getNodeIndex(Way w, Node n) {
        for (int i = 0; i < w.getNodesCount(); i++) {
            if (w.getNode(i).equals(n)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns all directional waterways which connect to at least one other usable way.
     *
     * @return all directional waterways which connect to at least one other usable way
     */
    private Collection<Collection<Way>> getGraphs() {
        // HashSet doesn't make a difference here
        Collection<Collection<Way>> graphs = new ArrayList<>();

        for (Way waterway : usableWaterways) {
            Collection<Way> graph = buildGraph(waterway);

            if (!graph.isEmpty())
                graphs.add(graph);
        }

        return graphs;
    }

    /**
     * Returns a collection of ways, which belongs to the same graph.
     *
     * @param way starting way to extend the graph from
     * @return a collection of ways which belongs to the same graph
     */
    private Collection<Way> buildGraph(Way way) {
        if (visitedWays.contains(way.getUniqueId()))
            return Collections.emptySet();

        final Set<Way> graph = new HashSet<>();
        Queue<Way> queue = new ArrayDeque<>();
        queue.offer(way);

        while (!queue.isEmpty()) {
            Way currentWay = queue.poll();
            visitedWays.add(currentWay.getUniqueId());

            for (Node node : currentWay.getNodes()) {
                Collection<Way> referrers = node.referrers(Way.class)
                    .filter(this::isPrimitiveUsable)
                    .filter(candidate -> candidate != currentWay)
                    .collect(Collectors.toList());

                if (!referrers.isEmpty()) {
                    for (Way referrer : referrers) {
                        if (!visitedWays.contains(referrer.getUniqueId())) {
                            queue.offer(referrer);
                            visitedWays.add(referrer.getUniqueId());
                        }
                    }
                    graph.addAll(referrers);
                }
            }
        }
        return graph;
    }
}
