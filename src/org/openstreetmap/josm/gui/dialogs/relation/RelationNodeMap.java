package org.openstreetmap.josm.gui.dialogs.relation;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A mapping from Node positions to elements in a Relation
 * (currently Nodes and Ways only)
 * 
 * @author Christiaan Welvaart <cjw@time4t.net>
 *
 */
public class RelationNodeMap {
    private java.util.HashMap<Node, java.util.TreeSet<Integer>>   points;
    private java.util.HashMap<Node, Integer>   nodes;
    private java.util.Vector<Integer>          remaining;
    private Relation                           relation;

    RelationNodeMap(Relation relation)
    {
        int     i;

        this.relation = relation;
        points = new java.util.HashMap<Node, java.util.TreeSet<Integer>>();
        nodes = new java.util.HashMap<Node, Integer>();
        remaining = new java.util.Vector<Integer>();

        for (i = 0; i < relation.members.size(); ++i)
        {
            RelationMember  m = relation.members.get(i);
            if (m.member.incomplete)
            {
                // throw an exception?
                return;
            }
            add(i, m);
        }
    }

    Integer find(Node node, int current)
    {
        Integer result = null;

        try {
            result = nodes.get(node);
            if (result == null)
            {
                result = points.get(node).first();
                if (relation.members.get(current).member == relation.members.get(result).member)
                {
                    result = points.get(node).last();
                }
            }
        } catch(NullPointerException f) {}
        catch(java.util.NoSuchElementException e) {}

        return result;
    }

    void add(int n, RelationMember m)
    {
        try
        {
            Way w = (Way)m.member;
            if (!points.containsKey(w.firstNode()))
            {
                points.put(w.firstNode(), new java.util.TreeSet<Integer>());
            }
            points.get(w.firstNode()).add(Integer.valueOf(n));

            if (!points.containsKey(w.lastNode()))
            {
                points.put(w.lastNode(), new java.util.TreeSet<Integer>());
            }
            points.get(w.lastNode()).add(Integer.valueOf(n));
        }
        catch(ClassCastException e1)
        {
            try
            {
                Node        node = (Node)m.member;
                nodes.put(node, Integer.valueOf(n));
            }
            catch(ClassCastException e2)
            {
                remaining.add(Integer.valueOf(n));
            }
        }
    }

    boolean remove(int n, RelationMember a)
    {
        boolean result;

        try
        {
            result = points.get(((Way)a.member).firstNode()).remove(n);
            result &= points.get(((Way)a.member).lastNode()).remove(n);
        }
        catch(ClassCastException e1)
        {
            result = (nodes.remove(a.member) != null);
        }

        return result;
    }

    void move(int from, int to)
    {
        if (from != to)
        {
            RelationMember  b = relation.members.get(from);
            RelationMember  a = relation.members.get(to);

            remove(to, b);
            add(to, a);
        }
    }

    // no node-mapped entries left
    boolean isEmpty()
    {
        return points.isEmpty() && nodes.isEmpty();
    }

    java.util.Vector<Integer> getRemaining()
    {
        return remaining;
    }

    Integer pop()
    {
        Integer result = null;

        if (!nodes.isEmpty())
        {
            result = nodes.values().iterator().next();
            nodes.remove(result);
        }
        else if (!points.isEmpty())
        {
            for (java.util.TreeSet<Integer> set : points.values())
            {
                if (!set.isEmpty())
                {
                    result = set.first();
                    break;
                }
            }
        }

        return result;
    }

}
