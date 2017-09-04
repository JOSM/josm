// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeGraph;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Helper for {@link ParallelWayAction}.
 *
 * @author Ole Jørgen Brønner (olejorgenb)
 */
public class ParallelWays {
    private final List<Way> ways;
    private final List<Node> sortedNodes;

    private final int nodeCount;

    private final EastNorth[] pts;
    private final EastNorth[] normals;

    /**
     * Constructs a new {@code ParallelWays}.
     * @param sourceWays source ways
     * @param copyTags whether tags should be copied
     * @param refWayIndex Need a reference way to determine the direction of the offset when we manage multiple ways
     */
    public ParallelWays(Collection<Way> sourceWays, boolean copyTags, int refWayIndex) {
        // Possible/sensible to use PrimetiveDeepCopy here?

        // Make a deep copy of the ways, keeping the copied ways connected
        // TODO: This assumes the first/last nodes of the ways are the only possible shared nodes.
        Map<Node, Node> splitNodeMap = new HashMap<>(sourceWays.size());
        for (Way w : sourceWays) {
            if (!splitNodeMap.containsKey(w.firstNode())) {
                splitNodeMap.put(w.firstNode(), copyNode(w.firstNode(), copyTags));
            }
            if (!splitNodeMap.containsKey(w.lastNode())) {
                splitNodeMap.put(w.lastNode(), copyNode(w.lastNode(), copyTags));
            }
        }
        ways = new ArrayList<>(sourceWays.size());
        for (Way w : sourceWays) {
            Way wCopy = new Way();
            wCopy.addNode(splitNodeMap.get(w.firstNode()));
            for (int i = 1; i < w.getNodesCount() - 1; i++) {
                wCopy.addNode(copyNode(w.getNode(i), copyTags));
            }
            wCopy.addNode(splitNodeMap.get(w.lastNode()));
            if (copyTags) {
                wCopy.setKeys(w.getKeys());
            }
            ways.add(wCopy);
        }

        // Find a linear ordering of the nodes. Fail if there isn't one.
        NodeGraph nodeGraph = NodeGraph.createUndirectedGraphFromNodeWays(ways);
        List<Node> sortedNodesPath = nodeGraph.buildSpanningPath();
        if (sortedNodesPath == null)
            throw new IllegalArgumentException("Ways must have spanning path"); // Create a dedicated exception?

        // Fix #8631 - Remove duplicated nodes from graph to be robust with self-intersecting ways
        Set<Node> removedNodes = new HashSet<>();
        sortedNodes = new ArrayList<>();
        for (int i = 0; i < sortedNodesPath.size(); i++) {
            Node n = sortedNodesPath.get(i);
            if (i < sortedNodesPath.size()-1 && sortedNodesPath.get(i+1).getCoor().equals(n.getCoor())) {
                removedNodes.add(n);
                for (Way w : ways) {
                    w.removeNode(n);
                }
                continue;
            }
            if (!removedNodes.contains(n)) {
                sortedNodes.add(n);
            }
        }

        // Ugly method of ensuring that the offset isn't inverted. I'm sure there is a better and more elegant way
        Way refWay = ways.get(refWayIndex);
        boolean refWayReversed = true;
        for (int i = 0; i < sortedNodes.size() - 1; i++) {
            if (sortedNodes.get(i) == refWay.firstNode() && sortedNodes.get(i + 1) == refWay.getNode(1)) {
                refWayReversed = false;
                break;
            }
        }
        if (refWayReversed) {
            Collections.reverse(sortedNodes); // need to keep the orientation of the reference way.
        }

        // Initialize the required parameters. (segment normals, etc.)
        nodeCount = sortedNodes.size();
        pts = new EastNorth[nodeCount];
        normals = new EastNorth[nodeCount - 1];
        int i = 0;
        for (Node n : sortedNodes) {
            EastNorth t = n.getEastNorth();
            pts[i] = t;
            i++;
        }
        for (i = 0; i < nodeCount - 1; i++) {
            double dx = pts[i + 1].getX() - pts[i].getX();
            double dy = pts[i + 1].getY() - pts[i].getY();
            double len = Math.sqrt(dx * dx + dy * dy);
            normals[i] = new EastNorth(-dy / len, dx / len);
        }
    }

    /**
     * Determines if the nodes graph form a closed path
     * @return {@code true} if the nodes graph form a closed path
     */
    public boolean isClosedPath() {
        return sortedNodes.get(0) == sortedNodes.get(sortedNodes.size() - 1);
    }

    /**
     * Offsets the way(s) d units. Positive d means to the left (relative to the reference way)
     * @param d offset
     */
    public void changeOffset(double d) {
        // This is the core algorithm:
        /* 1. Calculate a parallel line, offset by 'd', to each segment in the path
         * 2. Find the intersection of lines belonging to neighboring segments. These become the new node positions
         * 3. Do some special casing for closed paths
         *
         * Simple and probably not even close to optimal performance wise
         */

        EastNorth[] ppts = new EastNorth[nodeCount];

        EastNorth prevA = pts[0].add(normals[0].scale(d));
        EastNorth prevB = pts[1].add(normals[0].scale(d));
        for (int i = 1; i < nodeCount - 1; i++) {
            EastNorth a = pts[i].add(normals[i].scale(d));
            EastNorth b = pts[i + 1].add(normals[i].scale(d));
            if (Geometry.segmentsParallel(a, b, prevA, prevB)) {
                ppts[i] = a;
            } else {
                ppts[i] = Geometry.getLineLineIntersection(a, b, prevA, prevB);
            }
            prevA = a;
            prevB = b;
        }
        if (isClosedPath()) {
            EastNorth a = pts[0].add(normals[0].scale(d));
            EastNorth b = pts[1].add(normals[0].scale(d));
            if (Geometry.segmentsParallel(a, b, prevA, prevB)) {
                ppts[0] = a;
            } else {
                ppts[0] = Geometry.getLineLineIntersection(a, b, prevA, prevB);
            }
            ppts[nodeCount - 1] = ppts[0];
        } else {
            ppts[0] = pts[0].add(normals[0].scale(d));
            ppts[nodeCount - 1] = pts[nodeCount - 1].add(normals[nodeCount - 2].scale(d));
        }

        for (int i = 0; i < nodeCount; i++) {
            sortedNodes.get(i).setEastNorth(ppts[i]);
        }
    }

    /**
     * Performs the action by adding a new sequence command to the undo/redo queue.
     */
    public void commit() {
        MainApplication.undoRedo.add(new SequenceCommand("Make parallel way(s)", makeAddWayAndNodesCommandList()));
    }

    private List<Command> makeAddWayAndNodesCommandList() {
        DataSet ds = Main.main.getEditDataSet();
        List<Command> commands = new ArrayList<>(sortedNodes.size() + ways.size());
        for (int i = 0; i < sortedNodes.size() - (isClosedPath() ? 1 : 0); i++) {
            commands.add(new AddCommand(ds, sortedNodes.get(i)));
        }
        for (Way w : ways) {
            commands.add(new AddCommand(ds, w));
        }
        return commands;
    }

    private static Node copyNode(Node source, boolean copyTags) {
        if (copyTags)
            return new Node(source, true);
        else {
            Node n = new Node();
            n.setCoor(source.getCoor());
            return n;
        }
    }

    /**
     * Returns the resulting parallel ways.
     * @return the resulting parallel ways
     */
    public final List<Way> getWays() {
        return ways;
    }
}
