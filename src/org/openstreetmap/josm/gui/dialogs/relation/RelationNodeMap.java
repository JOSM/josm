// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.NONE;

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
 * @author Christiaan Welvaart <cjw@time4t.net>
 *
 */
public class RelationNodeMap {
    private class NodesWays{
        public Map<Node, Set<Integer>> nodes = new TreeMap<Node, Set<Integer>>();
        public Map<Integer, Set<Node>> ways = new TreeMap<Integer, Set<Node>>();
        public boolean oneWay;
        public NodesWays(boolean oneWay){
            this.oneWay = oneWay;
        }
    }

    /*
     * the maps. (Need TreeMap for efficiency.)
     */
    private NodesWays map = new NodesWays(false);
    /*
     * Maps for oneways (forward/backward roles)
     */

    private NodesWays onewayMap = new NodesWays(true);
    private NodesWays onewayReverseMap = new NodesWays(true);
    /*
     * Used to keep track of what members are done.
     */
    private Set<Integer> remaining;
    private Map<Integer, Set<Node>> remainingOneway = new TreeMap<Integer, Set<Node>>();;

    /**
     * All members that are incomplete or not a way
     */
    private List<Integer> notSortable = new ArrayList<Integer>();

    public static Node firstOnewayNode(RelationMember m){
        if(!m.isWay()) return null;
        if(m.getRole().equals("backward")) return m.getWay().lastNode();
        return m.getWay().firstNode();
    }

    public static Node lastOnewayNode(RelationMember m){
        if(!m.isWay()) return null;
        if(m.getRole().equals("backward")) return m.getWay().firstNode();
        return m.getWay().lastNode();
    }

    RelationNodeMap(List<RelationMember> members) {
        map.nodes = new TreeMap<Node, Set<Integer>>();
        map.ways = new TreeMap<Integer, Set<Node>>();

        for (int i = 0; i < members.size(); ++i) {
            RelationMember m = members.get(i);
            if (m.getMember().isIncomplete() || !m.isWay()) {
                notSortable.add(i);
                continue;
            }

            Way w = m.getWay();
            if ((MemberTableModel.roundaboutType(w) != NONE)) {
                for (Node nd : w.getNodes()) {
                    addPair(nd, i);
                }
            } else if(MemberTableModel.isOneway(m)) {
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

        remaining = new TreeSet<Integer>();
        remaining.addAll(map.ways.keySet());

        /*
         * Clean up the maps, i.e. remove nodes from roundabouts and dead ends that
         * cannot be used in future. (only for performance)
         */
//        Iterator<Map.Entry<Node,TreeSet<Integer>>> it = map.nodes.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<Node,TreeSet<Integer>> nodeLinks = it.next();
//
//            if (nodeLinks.getValue().size() < 2) {
//                if (nodeLinks.getValue().size() != 1) throw new AssertionError();
//
//                Integer d_way = nodeLinks.getValue().iterator().next();
//                TreeSet<Node> d_way_nodes = map.ways.get(d_way);
//                d_way_nodes.remove(nodeLinks.getKey());
//
//                it.remove();
//                continue;
//            }
//        }
            }

    private void addPair(Node n, int i) {
        Set<Integer> ts = map.nodes.get(n);
        if (ts == null) {
            ts = new TreeSet<Integer>();
            map.nodes.put(n, ts);
        }
        ts.add(i);

        Set<Node> ts2 = map.ways.get(i);
        if (ts2 == null) {
            ts2 = new TreeSet<Node>();
            map.ways.put(i, ts2);
        }
        ts2.add(n);
    }

    private void addNodeWayMap(Node n, int i) {
        Set<Integer> ts = onewayMap.nodes.get(n);
        if (ts == null) {
            ts = new TreeSet<Integer>();
            onewayMap.nodes.put(n, ts);
        }
        ts.add(i);
    }

    private void addWayNodeMap(Node n, int i) {
        Set<Node> ts2 = onewayMap.ways.get(i);
        if (ts2 == null) {
            ts2 = new TreeSet<Node>();
            onewayMap.ways.put(i, ts2);
        }
        ts2.add(n);
    }

    private void addNodeWayMapReverse(Node n, int i) {
        Set<Integer> ts = onewayReverseMap.nodes.get(n);
        if (ts == null) {
            ts = new TreeSet<Integer>();
            onewayReverseMap.nodes.put(n, ts);
        }
        ts.add(i);
    }

    private void addWayNodeMapReverse(Node n, int i) {
        Set<Node> ts2 = onewayReverseMap.ways.get(i);
        if (ts2 == null) {
            ts2 = new TreeSet<Node>();
            onewayReverseMap.ways.put(i, ts2);
        }
        ts2.add(n);
    }

    private void addRemainingForward(Node n, int i) {
        Set<Node> ts2 = remainingOneway.get(i);
        if (ts2 == null) {
            ts2 = new TreeSet<Node>();
            remainingOneway.put(i, ts2);
        }
        ts2.add(n);
    }

    Integer firstOneway = null;
    Node lastOnewayNode = null;
    Node firstCircular = null;

    /**
     * Return a relation member that is linked to the
     * member 'i', but has not been popped yet.
     * Return null if there is no such member left.
     */
    public Integer popAdjacent(Integer way) {
        if (lastOnewayNode != null) return popBackwardOnewayPart(way);
        if (firstOneway != null) return popForwardOnewayPart(way);

        if (map.ways.containsKey(way)){
            for (Node n : map.ways.get(way)) {
                Integer i = deleteAndGetAdjacentNode(map, n);
                if(i != null) return i;

                Integer j = deleteAndGetAdjacentNode(onewayMap, n);
                if(j != null) {
                    firstOneway = j;
                    return j;
                }
            }
        }
        
        firstOneway = way;
        return popForwardOnewayPart(way);
    }

    private Integer popForwardOnewayPart(Integer way) {
        if(onewayMap.ways.containsKey(way)) {
            for (Node n : onewayMap.ways.get(way)) {
                Integer i = findAdjacentWay(onewayMap, n);
                if(i == null) continue;

                lastOnewayNode = processBackwardIfEndOfLoopReached(i);
                if(lastOnewayNode != null){
                    return popBackwardOnewayPart(firstOneway);
                }

                deleteWayNode(onewayMap, i, n);
                return i;
            }
        }
        
        firstOneway = null;
        return null;
    }

    private Node processBackwardIfEndOfLoopReached(Integer way) { //find if we didn't reach end of the loop (and process backward part)
        if (onewayReverseMap.ways.containsKey(way)) {
            for (Node n : onewayReverseMap.ways.get(way)) {
                if((map.nodes.containsKey(n))
                        || (onewayMap.nodes.containsKey(n) && onewayMap.nodes.get(n).size() > 1)) {
                    return n;
                }
                if(firstCircular != null && firstCircular == n) {
                    return firstCircular;
                }
            }
        }
        return null;
    }
    
    private Integer popBackwardOnewayPart(int way){
        if (lastOnewayNode != null) {
            TreeSet<Node> nodes = new TreeSet<Node>();
            if (onewayReverseMap.ways.containsKey(way)) nodes.addAll(onewayReverseMap.ways.get(way));
            if (map.ways.containsKey(way)) nodes.addAll(map.ways.get(way));
            for (Node n : nodes) {
                if(n == lastOnewayNode) { //if oneway part ends
                    firstOneway = null;
                    lastOnewayNode = null;
                    Integer j = deleteAndGetAdjacentNode(map, n);
                    if(j != null) return j;

                    Integer k = deleteAndGetAdjacentNode(onewayMap, n);
                    if(k != null) {
                        firstOneway = k;
                        return k;
                    }
                }

                Integer j = deleteAndGetAdjacentNode(onewayReverseMap, n);
                if(j != null) return j;
            }
        }

        firstOneway = null;
        lastOnewayNode = null;
        
        return null;
    }

    /**
     * find next node in nw NodeWays structure, if the node is found delete and return it
     * @param nw
     * @param n
     * @return node next to n
     */
    private Integer deleteAndGetAdjacentNode(NodesWays nw, Node n){
        Integer j = findAdjacentWay(nw, n);
        if(j == null) return null;
        deleteWayNode(nw, j, n);
        return j;
    }

    private Integer findAdjacentWay(NodesWays nw, Node n) {
        Set<Integer> adj = nw.nodes.get(n);
        if (adj == null || adj.isEmpty()) return null;
        Integer j = adj.iterator().next();
        return j;
    }

    private void deleteWayNode(NodesWays nw, Integer way, Node n){
        if(nw.oneWay)
            doneOneway(way);
        else
            done(way);
        nw.ways.get(way).remove(n);
    }

    /**
     * Returns some remaining member or null if
     * every sortable member has been processed.
     */
    public Integer pop() {
        if (!remaining.isEmpty()){
            Integer i = remaining.iterator().next();
            done(i);
            return i;
        }

        if (remainingOneway.isEmpty()) return null;
        for(Integer i :remainingOneway.keySet()){ //find oneway, whic is connected to more than one way (is between two oneway loops)
            for(Node n : onewayReverseMap.ways.get(i)){
                if(onewayReverseMap.nodes.containsKey(n) && onewayReverseMap.nodes.get(n).size() > 1) {
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
     */
    private void doneOneway(Integer i) {
        Set<Node> nodesForward = remainingOneway.get(i);
        for (Node n : nodesForward) {
            if(onewayMap.nodes.containsKey(n)) onewayMap.nodes.get(n).remove(i);
            if(onewayReverseMap.nodes.containsKey(n)) onewayReverseMap.nodes.get(n).remove(i);
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
