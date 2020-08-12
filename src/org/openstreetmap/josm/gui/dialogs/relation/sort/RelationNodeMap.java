// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import static org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction.NONE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Auxiliary class for relation sorting.
 *
 * Constructs two mappings: One that maps each way to its nodes and the inverse mapping that
 * maps each node to all ways that have this node.
 * After construction both maps are consistent, but later on objects that are no longer needed
 * are removed from the value sets.
 * However the corresponding keys are not deleted even if they map to an empty set.
 * Note that normal ways have 2 nodes (beginning and end) but roundabouts can have less or more
 * (that are shared by other members).
 *
 * @author Christiaan Welvaart &lt;cjw@time4t.net&gt;
 * @since 1785
 */
public class RelationNodeMap {

    private static final String ROLE_BACKWARD = "backward";

    private static class NodesWays {
        public final Map<Node, Set<Integer>> nodes = new TreeMap<>();
        public final Map<Integer, Set<Node>> ways = new TreeMap<>();
        public final boolean oneWay;

        NodesWays(boolean oneWay) {
            this.oneWay = oneWay;
        }
    }

    /*
     * the maps. (Need TreeMap for efficiency.)
     */
    private final NodesWays map = new NodesWays(false);
    /*
     * Maps for oneways (forward/backward roles)
     */

    private final NodesWays onewayMap = new NodesWays(true);
    private final NodesWays onewayReverseMap = new NodesWays(true);
    /*
     * Used to keep track of what members are done.
     */
    private final Set<Integer> remaining = new TreeSet<>();
    private final Map<Integer, Set<Node>> remainingOneway = new TreeMap<>();

    /**
     * All members that are incomplete or not a way
     */
    private final List<Integer> notSortable = new ArrayList<>();

    /**
     * Gets the start node of the member, respecting the direction role.
     * @param m The relation member.
     * @return <code>null</code> if the member is no way, the node otherwise.
     */
    public static Node firstOnewayNode(RelationMember m) {
        if (!m.isWay()) return null;
        if (ROLE_BACKWARD.equals(m.getRole())) {
            return m.getWay().lastNode();
        }
        return m.getWay().firstNode();
    }

    /**
     * Gets the end node of the member, respecting the direction role.
     * @param m The relation member.
     * @return <code>null</code> if the member is no way, the node otherwise.
     */
    public static Node lastOnewayNode(RelationMember m) {
        if (!m.isWay()) return null;
        if (ROLE_BACKWARD.equals(m.getRole())) {
            return m.getWay().firstNode();
        }
        return m.getWay().lastNode();
    }

    RelationNodeMap(List<RelationMember> members) {
        for (int i = 0; i < members.size(); ++i) {
            RelationMember m = members.get(i);
            if (m.getMember().isIncomplete() || !m.isWay() || m.getWay().getNodesCount() < 2) {
                notSortable.add(i);
                continue;
            }

            Way w = m.getWay();
            if (RelationSortUtils.roundaboutType(w) != NONE) {
                for (Node nd : w.getNodes()) {
                    addPair(nd, i);
                }
            } else if (RelationSortUtils.isOneway(m)) {
                addNodeWayMap(firstOnewayNode(m), i);
                addWayNodeMap(lastOnewayNode(m), i);
                addNodeWayMapReverse(lastOnewayNode(m), i);
                addWayNodeMapReverse(firstOnewayNode(m), i);
                addRemainingForward(firstOnewayNode(m), i);
                addRemainingForward(lastOnewayNode(m), i);
            } else {
                addPair(w.firstNode(), i);
                addPair(w.lastNode(), i);
            }
        }

        remaining.addAll(map.ways.keySet());
    }

    private void addPair(Node n, int i) {
        map.nodes.computeIfAbsent(n, k -> new TreeSet<>()).add(i);
        map.ways.computeIfAbsent(i, k -> new TreeSet<>()).add(n);
    }

    private void addNodeWayMap(Node n, int i) {
        onewayMap.nodes.computeIfAbsent(n, k -> new TreeSet<>()).add(i);
    }

    private void addWayNodeMap(Node n, int i) {
        onewayMap.ways.computeIfAbsent(i, k -> new TreeSet<>()).add(n);
    }

    private void addNodeWayMapReverse(Node n, int i) {
        onewayReverseMap.nodes.computeIfAbsent(n, k -> new TreeSet<>()).add(i);
    }

    private void addWayNodeMapReverse(Node n, int i) {
        onewayReverseMap.ways.computeIfAbsent(i, k -> new TreeSet<>()).add(n);
    }

    private void addRemainingForward(Node n, int i) {
        remainingOneway.computeIfAbsent(i, k -> new TreeSet<>()).add(n);
    }

    private Integer firstOneway;
    private Node lastOnewayNode;
    private Node firstCircular;

    /**
     * Return a relation member that is linked to the member 'i', but has not been popped yet.
     * Return null if there is no such member left.
     * @param way way key
     * @return a relation member that is linked to the member 'i', but has not been popped yet
     */
    public Integer popAdjacent(Integer way) {
        if (lastOnewayNode != null) return popBackwardOnewayPart(way);
        if (firstOneway != null) return popForwardOnewayPart(way);

        if (map.ways.containsKey(way)) {
            for (Node n : map.ways.get(way)) {
                Integer i = deleteAndGetAdjacentNode(map, n);
                if (i != null) return i;

                Integer j = deleteAndGetAdjacentNode(onewayMap, n);
                if (j != null) {
                    firstOneway = j;
                    return j;
                }
            }
        }

        firstOneway = way;
        return popForwardOnewayPart(way);
    }

    private Integer popForwardOnewayPart(Integer way) {
        if (onewayMap.ways.containsKey(way)) {
            Node exitNode = onewayMap.ways.get(way).iterator().next();

            if (checkIfEndOfLoopReached(exitNode)) {
                lastOnewayNode = exitNode;
                return popBackwardOnewayPart(firstOneway);
            }

            Integer i = deleteAndGetAdjacentNode(onewayMap, exitNode);
            if (i != null) return i;

            // When our forward route ends in a dead end try to start
            // the backward route anyway from the split point
            // (firstOneWay), to support routes with split a split start
            // or end.
            lastOnewayNode = exitNode;
            return popBackwardOnewayPart(firstOneway);
        }

        firstOneway = null;
        return null;
    }

    // Check if the given node can be the end of the loop (i.e. it has
    // an outgoing bidirectional or multiple outgoing oneways, or we
    // looped back to our first circular node)
    private boolean checkIfEndOfLoopReached(Node n) {
        if (map.nodes.containsKey(n)
                || (onewayMap.nodes.containsKey(n) && onewayMap.nodes.get(n).size() > 1))
            return true;
        if (firstCircular != null && firstCircular == n)
            return true;
        return false;
    }

    private Integer popBackwardOnewayPart(int way) {
        if (lastOnewayNode != null) {
            Set<Node> nodes = new TreeSet<>();
            if (onewayReverseMap.ways.containsKey(way)) {
                nodes.addAll(onewayReverseMap.ways.get(way));
            }
            if (map.ways.containsKey(way)) {
                nodes.addAll(map.ways.get(way));
            }
            for (Node n : nodes) {
                if (n == lastOnewayNode) { //if oneway part ends
                    firstOneway = null;
                    lastOnewayNode = null;
                    Integer j = deleteAndGetAdjacentNode(map, n);
                    if (j != null) return j;

                    Integer k = deleteAndGetAdjacentNode(onewayMap, n);
                    if (k != null) {
                        firstOneway = k;
                        return k;
                    }
                }

                Integer j = deleteAndGetAdjacentNode(onewayReverseMap, n);
                if (j != null) return j;
            }
        }

        firstOneway = null;
        lastOnewayNode = null;

        return null;
    }

    /**
     * find next node in nw NodeWays structure, if the node is found delete and return it
     * @param nw nodes and ways
     * @param n node
     * @return node next to n
     */
    private Integer deleteAndGetAdjacentNode(NodesWays nw, Node n) {
        Integer j = findAdjacentWay(nw, n);
        if (j == null) return null;
        deleteWayNode(nw, j, n);
        return j;
    }

    private static Integer findAdjacentWay(NodesWays nw, Node n) {
        Set<Integer> adj = nw.nodes.get(n);
        if (adj == null || adj.isEmpty()) return null;
        return adj.iterator().next();
    }

    private void deleteWayNode(NodesWays nw, Integer way, Node n) {
        if (nw.oneWay) {
            doneOneway(way);
        } else {
            done(way);
            // For bidirectional ways, remove the entry node, so
            // subsequent lookups will only return the other node(s) as
            // valid exit nodes.
            nw.ways.get(way).remove(n);
        }
    }

    /**
     * Returns some remaining member or null if every sortable member has been processed.
     * @return member key
     */
    public Integer pop() {
        if (!remaining.isEmpty()) {
            Integer i = remaining.iterator().next();
            done(i);
            return i;
        }

        if (remainingOneway.isEmpty()) return null;
        for (Integer i : remainingOneway.keySet()) { //find oneway, which is connected to more than one way (is between two oneway loops)
            for (Node n : onewayReverseMap.ways.get(i)) {
                if (onewayReverseMap.nodes.containsKey(n) && onewayReverseMap.nodes.get(n).size() > 1) {
                    doneOneway(i);
                    firstCircular = n;
                    return i;
                }
            }
        }

        Integer i = remainingOneway.keySet().iterator().next();
        doneOneway(i);
        return i;
    }

    /**
     * This relation member has been processed.
     * Remove references in the map.nodes.
     * @param i member key
     */
    private void doneOneway(Integer i) {
        Set<Node> nodesForward = remainingOneway.get(i);
        for (Node n : nodesForward) {
            if (onewayMap.nodes.containsKey(n)) {
                onewayMap.nodes.get(n).remove(i);
            }
            if (onewayReverseMap.nodes.containsKey(n)) {
                onewayReverseMap.nodes.get(n).remove(i);
            }
        }
        remainingOneway.remove(i);
    }

    private void done(Integer i) {
        remaining.remove(i);
        Set<Node> nodes = map.ways.get(i);
        for (Node n : nodes) {
            boolean result = map.nodes.get(n).remove(i);
            if (!result) throw new AssertionError();
        }
    }

    public List<Integer> getNotSortableMembers() {
        return notSortable;
    }
}
