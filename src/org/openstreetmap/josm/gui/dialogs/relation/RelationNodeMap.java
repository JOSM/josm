// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.*;

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
    /*
     * read only list of all relation members
     */
    private final List<RelationMember> members;
    /*
     * the maps. (Need TreeMap for efficiency.)
     */
    private TreeMap<Node, TreeSet<Integer>> nodesMap;
    private TreeMap<Integer, TreeSet<Node>> waysMap;
    /*
     * Used to keep track of what members are done.
     */
    private TreeSet<Integer> remaining;

    /**
     * All members that are incomplete or not a way
     */
    private List<Integer> notSortable = new ArrayList<Integer>();

    RelationNodeMap(ArrayList<RelationMember> members) {
        this.members = members;

        nodesMap = new TreeMap<Node, TreeSet<Integer>>();
        waysMap = new TreeMap<Integer, TreeSet<Node>>();

        for (int i = 0; i < members.size(); ++i) {
            RelationMember m = members.get(i);
            if (m.getMember().isIncomplete() || !m.isWay())
            {
                notSortable.add(i);
            }
            else {
                Way w = m.getWay();
                if (MemberTableModel.roundaboutType(w) != NONE) {
                    for (Node nd : w.getNodes()) {
                        addPair(nd, i);
                    }
                } else {
                    addPair(w.firstNode(), i);
                    addPair(w.lastNode(), i);
                }
            }
        }

        remaining = new TreeSet<Integer>();
        for (Integer k : waysMap.keySet()) {
            remaining.add(k);
        }

        /*
         * Clean up the maps, i.e. remove nodes from roundabouts and dead ends that
         * cannot be used in future. (only for performance)
         */
        Iterator<Map.Entry<Node,TreeSet<Integer>>> it = nodesMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Node,TreeSet<Integer>> nodeLinks = it.next();

            if (nodeLinks.getValue().size() < 2) {
//                System.err.println("DELETE: "+nodeLinks.getKey()+" "+nodeLinks.getValue());
                if (nodeLinks.getValue().size() != 1) throw new AssertionError();

                Integer d_way = nodeLinks.getValue().iterator().next();
                TreeSet<Node> d_way_nodes = waysMap.get(d_way);
                d_way_nodes.remove(nodeLinks.getKey());

                it.remove();
                continue;
            }
//            System.err.println(nodeLinks.getKey()+" "+nodeLinks.getValue());

        }
//        System.err.println("");
//        System.err.println(remaining);
//        System.err.println("");
//        System.err.println(nodesMap);
//        System.err.println("");
//        System.err.println(waysMap);

    }

    private void addPair(Node n, int i) {
        TreeSet<Integer> ts = nodesMap.get(n);
        if (ts == null) {
            ts = new TreeSet<Integer>();
            nodesMap.put(n, ts);
        }
        ts.add(i);

        TreeSet<Node> ts2 = waysMap.get(i);
        if (ts2 == null) {
            ts2 = new TreeSet<Node>();
            waysMap.put(i, ts2);
        }
        ts2.add(n);
    }

    /**
     * Return a relation member that is linked to the
     * member 'i', but has not been popped jet.
     * Return null if there is no such member left.
     */
    public Integer popAdjacent(Integer i) {
//        System.err.print("Adjacent["+i+"]:");
        TreeSet<Node> nodes = waysMap.get(i);
//        System.err.print(nodes);
        for (Node n : nodes) {
//            System.err.print(" {"+n.getId()+"} ");
            TreeSet<Integer> adj = nodesMap.get(n);
            if (!adj.isEmpty()) {
                Integer j = adj.iterator().next();
                done(j);
                waysMap.get(j).remove(n);
//                System.err.println(j);
                return j;
            }
        }
        return null;
    }

    /**
     * Returns some remaining member or null if
     * every sortable member has been processed.
     */
    public Integer pop() {
        if (remaining.isEmpty()) return null;
        Integer i = remaining.iterator().next();
        done(i);
        return i;
    }

    /**
     * This relation member has been processed.
     * Remove references in the nodesMap.
     */
    private void done(Integer i) {
        remaining.remove(i);
        TreeSet<Node> nodes = waysMap.get(i);
        for (Node n : nodes) {
            boolean result = nodesMap.get(n).remove(i);
            if (!result) throw new AssertionError();
        }
    }

    public List<Integer> getNotSortableMembers() {
        return notSortable;
    }
}
