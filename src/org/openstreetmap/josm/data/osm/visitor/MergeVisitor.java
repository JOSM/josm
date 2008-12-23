// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A visitor that get a data set at construction time and merge every visited object
 * into it.
 *
 * @author imi
 */
public class MergeVisitor implements Visitor {

    /**
     * Map from primitives in the database to visited primitives. (Attention: The other way
     * round than merged)
     */
    public Map<OsmPrimitive, OsmPrimitive> conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();

    private final DataSet ds;
    private final DataSet mergeds;

    private final HashMap<Long, Node> nodeshash = new HashMap<Long, Node>();
    private final HashMap<Long, Way> wayshash = new HashMap<Long, Way>();
    private final HashMap<Long, Relation> relshash = new HashMap<Long, Relation>();

    /**
     * A list of all primitives that got replaced with other primitives.
     * Key is the primitives in the other's dataset and the value is the one that is now
     * in ds.nodes instead.
     */
    private final Map<OsmPrimitive, OsmPrimitive> merged
        = new HashMap<OsmPrimitive, OsmPrimitive>();

    public MergeVisitor(DataSet ds, DataSet mergeds) {
        this.ds = ds;
        this.mergeds = mergeds;

        for (Node n : ds.nodes) if (n.id != 0) nodeshash.put(n.id, n);
        for (Way w : ds.ways) if (w.id != 0) wayshash.put(w.id, w);
        for (Relation r : ds.relations) if (r.id != 0) relshash.put(r.id, r);
    }

    private <P extends OsmPrimitive> void genMerge(P other,
            Collection<P> myprims, Collection<P> mergeprims,
            HashMap<Long, P> primhash) {
        // 1. Try to find an identical prim with the same id.
        if (mergeAfterId(myprims, primhash, other))
            return;

        // 2. Try to find a prim we can merge with the prim from the other ds.
        for (P my : myprims) {
            // LinkedList.contains calls equal, and OsmPrimitive.equal
            // compares just the id.
            if (match(my, other) && !mergeprims.contains(my)) {
                merged.put(other, my);
                mergeCommon(my, other);
                return;
            }
        }

        // 3. No idea how to merge that.  Simply add it unchanged.
        myprims.add(other);
    }

    public void visit(Node other) {
        genMerge(other, ds.nodes, mergeds.nodes, nodeshash);
    }

    public void visit(Way other) {
        fixWay(other);
        genMerge(other, ds.ways, mergeds.ways, wayshash);
    }

    public void visit(Relation other) {
        fixRelation(other);
        genMerge(other, ds.relations, mergeds.relations, relshash);
    }

    /**
     * Postprocess the dataset and fix all merged references to point to the actual
     * data.
     */
    public void fixReferences() {
        for (Way w : ds.ways) fixWay(w);
        for (Relation r : ds.relations) fixRelation(r);
        for (OsmPrimitive osm : conflicts.values())
            if (osm instanceof Way)
                fixWay((Way)osm);
            else if (osm instanceof Relation)
                fixRelation((Relation) osm);
    }

    private void fixWay(Way w) {
        boolean replacedSomething = false;
        LinkedList<Node> newNodes = new LinkedList<Node>();
        for (Node n : w.nodes) {
            Node otherN = (Node) merged.get(n);
            newNodes.add(otherN == null ? n : otherN);
            if (otherN != null)
                replacedSomething = true;
        }
        if (replacedSomething) {
            w.nodes.clear();
            w.nodes.addAll(newNodes);
        }
    }

    private void fixRelation(Relation r) {
        boolean replacedSomething = false;
        LinkedList<RelationMember> newMembers = new LinkedList<RelationMember>();
        for (RelationMember m : r.members) {
            OsmPrimitive otherP = merged.get(m.member);
            if (otherP == null) {
                newMembers.add(m);
            } else {
                RelationMember mnew = new RelationMember(m);
                mnew.member = otherP;
                newMembers.add(mnew);
                replacedSomething = true;
            }
        }
        if (replacedSomething) {
            r.members.clear();
            r.members.addAll(newMembers);
        }
    }

    private static <P extends OsmPrimitive> boolean match(P p1, P p2) {
        if ((p1.id == 0 || p2.id == 0) && !p1.incomplete && !p2.incomplete) {
            return realMatch(p1, p2);
        }
        return p1.id == p2.id;
    }

    /** @return true if the prims have pretty much the same data, i.e. the
     * same position, the same members, ...
     */
    // Java cannot dispatch on generics...
    private static boolean realMatch(OsmPrimitive p1, OsmPrimitive p2) {
        if (p1 instanceof Node && p2 instanceof Node) {
            return realMatch((Node) p1, (Node) p2);
        } else if (p1 instanceof Way && p2 instanceof Way) {
            return realMatch((Way) p1, (Way) p2);
        } else if (p1 instanceof Relation && p2 instanceof Relation) {
            return realMatch((Relation) p1, (Relation) p2);
        } else {
            throw new RuntimeException("arguments have unknown type");
        }
    }

    private static boolean realMatch(Node n1, Node n2) {
        return n1.coor.equalsEpsilon(n2.coor);
    }

    private static boolean realMatch(Way w1, Way w2) {
        if (w1.nodes.size() != w2.nodes.size())
            return false;
        Iterator<Node> it = w1.nodes.iterator();
        for (Node n : w2.nodes)
            if (!match(n, it.next()))
                return false;
        return true;
    }

    private static boolean realMatch(Relation w1, Relation w2) {
        // FIXME this is not perfect yet...
        if (w1.members.size() != w2.members.size())
            return false;
        for (RelationMember em : w1.members) {
            if (!w2.members.contains(em)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Merge the common parts of an osm primitive.
     * @param my The object, the information gets merged into
     * @param other The object, the information gets merged from
     */
    private void mergeCommon(OsmPrimitive my, OsmPrimitive other) {
        if (other.deleted)
            my.delete(true);
        if (my.id == 0 || !my.modified || other.modified) {
            if (my.id == 0 && other.id != 0) {
                my.id = other.id;
                my.modified = other.modified; // match a new node
            } else if (my.id != 0 && other.id != 0 && other.modified)
                my.modified = true;
        }
        if (other.keys == null)
            return;
        if (my.keySet().containsAll(other.keys.keySet()))
            return;
        if (my.keys == null)
            my.keys = other.keys;
        else
            my.keys.putAll(other.keys);

        my.modified = true;
    }

    /**
     * @return <code>true</code>, if no merge is needed or merge is performed already.
     */
    private <P extends OsmPrimitive> boolean mergeAfterId(
            Collection<P> primitives, HashMap<Long, P> hash, P other) {
        // Fast-path merging of identical objects
        if (hash.containsKey(other.id)) {
            P my = hash.get(other.id);
            if (my.realEqual(other, true)) {
                merged.put(other, my);
                return true;
            }
        }

        for (P my : primitives) {
            if (my.realEqual(other, false)) {
                merged.put(other, my);
                return true; // no merge needed.
            }
            if (my.realEqual(other, true)) {
                Date myd = my.timestamp == null ? new Date(0) : my.getTimestamp();
                Date otherd = other.timestamp == null ? new Date(0) : other.getTimestamp();

                // they differ in modified/timestamp combination only. Auto-resolve it.
                merged.put(other, my);
                if (myd.before(otherd)) {
                    my.modified = other.modified;
                    my.timestamp = other.timestamp;
                }
                return true; // merge done.
            }
            if (my.id == other.id && my.id != 0) {
                Date myd = my.timestamp == null ? new Date(0) : my.getTimestamp();
                Date otherd = other.timestamp == null ? new Date(0) : other.getTimestamp();

                if (my.incomplete || other.incomplete) {
                    if (my.incomplete) {
                        my.cloneFrom(other);
                    }
                } else if (my.modified && other.modified) {
                    conflicts.put(my, other);
                } else if (!my.modified && !other.modified) {
                    if (myd.before(otherd)) {
                        my.cloneFrom(other);
                    }
                } else if (other.modified) {
                    if (myd.after(otherd)) {
                        conflicts.put(my, other);
                    } else {
                        my.cloneFrom(other);
                    }
                } else if (my.modified) {
                    if (myd.before(otherd)) {
                        conflicts.put(my, other);
                    }
                }
                merged.put(other, my);
                return true;
            }
        }
        return false;
    }
}
