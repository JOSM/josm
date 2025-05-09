// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.algorithms.Tarjan;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeGraph;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Pair;

/**
 * Test for detecting <a href="https://en.wikipedia.org/wiki/Cycle_(graph_theory)">cycles</a> in a directed graph,
 * currently used for waterways only. The processed graph consists of ways labeled as waterway.
 *
 * @author gaben
 * @since 19062
 */
public class CycleDetector extends Test {
    protected static final int CYCLE_DETECTED = 4200;

    /** All waterways for cycle detection */
    private final Set<Way> usableWaterways = new HashSet<>();

    /** Already visited primitive unique IDs */
    private final Set<Long> visitedWays = new HashSet<>();

    /** Currently used directional waterways from the OSM wiki */
    private List<String> directionalWaterways;

    protected static final String PREFIX = ValidatorPrefHelper.PREFIX + "." + CycleDetector.class.getSimpleName();

    /**
     * Constructor
     */
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
                Arrays.asList("river", "stream", "tidal_channel", "drain", "ditch", "fish_pass"));
    }

    @Override
    public void endTest() {
        final QuadBuckets<Way> quadBuckets = new QuadBuckets<>();
        quadBuckets.addAll(usableWaterways);

        for (Collection<Way> graph : getGraphs()) {
            NodeGraph nodeGraph = NodeGraph.createDirectedGraphFromWays(graph);
            Tarjan tarjan = new Tarjan(nodeGraph);
            Collection<List<Node>> scc = tarjan.getSCC();

            // for partial selection, we need to manually add the rest of graph members to the lookup object
            if (partialSelection) {
                quadBuckets.addAll(graph);
            }

            for (List<Node> possibleCycle : scc) {
                // there is a cycle in the graph if a strongly connected component has more than one node
                if (possibleCycle.size() > 1) {
                    // build bbox to locate the issue
                    BBox bBox = new BBox();
                    possibleCycle.forEach(node -> bBox.addPrimitive(node, 0));
                    // find ways within this bbox
                    List<Way> waysWithinErrorBbox = quadBuckets.search(bBox);
                    List<Way> toReport = waysWithinErrorBbox.stream()
                            .filter(w -> possibleCycle.stream().filter(w.getNodes()::contains).count() > 1)
                            .collect(Collectors.toList());

                    Map<Node, List<Node>> graphMap = tarjan.getGraphMap();
                    errors.add(
                            TestError.builder(this, Severity.ERROR, CYCLE_DETECTED)
                                    .message(trc("graph theory", "Cycle in directional waterway network"))
                                    .primitives(toReport)
                                    .highlightWaySegments(createSegments(graphMap, possibleCycle))
                                    .build()
                    );
                }
            }
        }

        usableWaterways.clear();
        visitedWays.clear();
        super.endTest();
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
                    if (isConsecutive(w, n, m)) {
                        segments.add(WaySegment.forNodePair(w, n, m));
                    }
                }
            }
        }

        return segments;
    }

    /**
     * Determines if the given nodes are consecutive part of the parent way.
     *
     * @param w parent way
     * @param n the first node to look up in the way direction
     * @param m the second, possibly consecutive node
     * @return {@code true} if the nodes are consecutive order in the way direction
     */
    private static boolean isConsecutive(Way w, Node n, Node m) {
        for (int i = 0; i < w.getNodesCount() - 1; i++) {
            if (w.getNode(i).equals(n) && w.getNode(i + 1).equals(m)) {
                return true;
            }
        }

        return false;
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
            if (visitedWays.contains(waterway.getUniqueId())) {
                continue;
            }
            Collection<Way> graph = buildGraph(waterway);

            if (!graph.isEmpty()) {
                graphs.add(graph);
            }
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

        // case for single, non-connected waterways
        if (graph.isEmpty()) {
            graph.add(way);
        }

        return graph;
    }
}
