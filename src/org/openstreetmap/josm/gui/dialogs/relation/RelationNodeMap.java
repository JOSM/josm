package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A mapping from Node positions to elements in a Relation (currently Nodes and Ways only)
 * 
 * @author Christiaan Welvaart <cjw@time4t.net>
 * 
 */
public class RelationNodeMap {
    private java.util.HashMap<Node, java.util.TreeSet<Integer>> points;
    private java.util.HashMap<Node, Integer> nodes;
    private java.util.Vector<Integer> remaining;
    private ArrayList<RelationMember> members;

    RelationNodeMap(ArrayList<RelationMember> members) {
        int i;

        this.members = members;
        points = new java.util.HashMap<Node, java.util.TreeSet<Integer>>();
        nodes = new java.util.HashMap<Node, Integer>();
        remaining = new java.util.Vector<Integer>();

        for (i = 0; i < members.size(); ++i) {
            RelationMember m = members.get(i);
            if (m.member.incomplete)
                // throw an exception?
                return;
            add(i, m);
        }
    }

    Integer find(Node node, int current) {
        Integer result = null;

        try {
            result = nodes.get(node);
            if (result == null) {
                result = points.get(node).first();
                if (members.get(current).member == members.get(result).member) {
                    result = points.get(node).last();
                }
            }
        } catch (NullPointerException f) {
        } catch (java.util.NoSuchElementException e) {
        }

        return result;
    }

    void add(int n, RelationMember m) {
        if (m.member instanceof Way) {
            Way w = (Way) m.member;
            if (!points.containsKey(w.firstNode())) {
                points.put(w.firstNode(), new java.util.TreeSet<Integer>());
            }
            points.get(w.firstNode()).add(Integer.valueOf(n));

            if (!points.containsKey(w.lastNode())) {
                points.put(w.lastNode(), new java.util.TreeSet<Integer>());
            }
            points.get(w.lastNode()).add(Integer.valueOf(n));
        } else if (m.member instanceof Node) {
            Node node = (Node) m.member;
            nodes.put(node, Integer.valueOf(n));
        } else {
            remaining.add(Integer.valueOf(n));
        }
    }

    boolean remove(int n, RelationMember a) {
        boolean result;
        if (a.member instanceof Way) {
            result = points.get(((Way) a.member).firstNode()).remove(n);
            result &= points.get(((Way) a.member).lastNode()).remove(n);
        } else {
            result = (nodes.remove(a.member) != null);
        }
        return result;
    }

    void move(int from, int to) {
        if (from != to) {
            RelationMember b = members.get(from);
            RelationMember a = members.get(to);

            remove(to, b);
            add(to, a);
        }
    }

    // no node-mapped entries left
    boolean isEmpty() {
        return points.isEmpty() && nodes.isEmpty();
    }

    java.util.Vector<Integer> getRemaining() {
        return remaining;
    }

    Integer pop() {
        Node node = null;
        Integer result = null;

        if (!nodes.isEmpty()) {
            node = nodes.keySet().iterator().next();
            result = nodes.get(node);
            nodes.remove(node);
        } else if (!points.isEmpty()) {
            for (java.util.TreeSet<Integer> set : points.values()) {
                if (!set.isEmpty()) {
                    result = set.first();
                    Way w = (Way) members.get(result).member;
                    points.get(w.firstNode()).remove(result);
                    points.get(w.lastNode()).remove(result);
                    break;
                }
            }
        }

        return result;
    }
}
