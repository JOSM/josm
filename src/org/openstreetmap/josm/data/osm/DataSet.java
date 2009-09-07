// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.SelectionChangedListener;

/**
 * DataSet is the data behind the application. It can consists of only a few points up to the whole
 * osm database. DataSet's can be merged together, saved, (up/down/disk)loaded etc.
 *
 * Note that DataSet is not an osm-primitive and so has no key association but a few members to
 * store some information.
 *
 * @author imi
 */
public class DataSet implements Cloneable {

    /**
     * The API version that created this data set, if any.
     */
    public String version;

    /**
     * All nodes goes here, even when included in other data (ways etc). This enables the instant
     * conversion of the whole DataSet by iterating over this data structure.
     */
    public Collection<Node> nodes = new LinkedList<Node>();

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The way nodes are stored only in the way list.
     */
    public Collection<Way> ways = new LinkedList<Way>();

    /**
     * All relations/relationships
     */
    public Collection<Relation> relations = new LinkedList<Relation>();

    /**
     * All data sources of this DataSet.
     */
    public Collection<DataSource> dataSources = new LinkedList<DataSource>();

    /**
     * A list of listeners to selection changed events. The list is static, as listeners register
     * themselves for any dataset selection changes that occur, regardless of the current active
     * dataset. (However, the selection does only change in the active layer)
     */
    public static Collection<SelectionChangedListener> selListeners = new LinkedList<SelectionChangedListener>();

    /**
     * @return A collection containing all primitives of the dataset. The data is ordered after:
     * first come nodes, then ways, then relations. Ordering in between the categories is not
     * guaranteed.
     */
    public List<OsmPrimitive> allPrimitives() {
        List<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        o.addAll(nodes);
        o.addAll(ways);
        o.addAll(relations);
        return o;
    }

    /**
     * @return A collection containing all not-deleted primitives (except keys).
     */
    public Collection<OsmPrimitive> allNonDeletedPrimitives() {
        Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : allPrimitives())
            if (osm.isVisible() && !osm.isDeleted()) {
                o.add(osm);
            }
        return o;
    }

    public Collection<OsmPrimitive> allNonDeletedCompletePrimitives() {
        Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : allPrimitives())
            if (osm.isVisible() && !osm.isDeleted() && !osm.incomplete) {
                o.add(osm);
            }
        return o;
    }

    public Collection<OsmPrimitive> allNonDeletedPhysicalPrimitives() {
        Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : allPrimitives())
            if (osm.isVisible() && !osm.isDeleted() && !osm.incomplete && !(osm instanceof Relation)) {
                o.add(osm);
            }
        return o;
    }

    /**
     * Adds a primitive to the dataset
     *
     * @param primitive the primitive. Ignored if null.
     */
    public void addPrimitive(OsmPrimitive primitive) {
        if (primitive == null)
            return;
        if (primitive instanceof Node) {
            nodes.add((Node) primitive);
        } else if (primitive instanceof Way) {
            ways.add((Way) primitive);
        } else if (primitive instanceof Relation) {
            relations.add((Relation) primitive);
        }
    }

    /**
     * Removes a primitive from the dataset. This method only removes the
     * primitive form the respective collection of primitives managed
     * by this dataset, i.e. from {@see #nodes}, {@see #ways}, or
     * {@see #relations}. References from other primitives to this
     * primitive are left unchanged.
     *
     * @param primitive the primitive. Ignored if null.
     */
    public void removePrimitive(OsmPrimitive primitive) {
        if (primitive == null)
            return;
        if (primitive instanceof Node) {
            nodes.remove(primitive);
        } else if (primitive instanceof Way) {
            ways.remove(primitive);
        } else if (primitive instanceof Relation) {
            relations.remove(primitive);
        }
    }

    public Collection<OsmPrimitive> getSelectedNodesAndWays() {
        Collection<OsmPrimitive> sel = getSelected(nodes);
        sel.addAll(getSelected(ways));
        return sel;
    }


    /**
     * Return a list of all selected objects. Even keys are returned.
     * @return List of all selected objects.
     */
    public Collection<OsmPrimitive> getSelected() {
        Collection<OsmPrimitive> sel = getSelected(nodes);
        sel.addAll(getSelected(ways));
        sel.addAll(getSelected(relations));
        return sel;
    }

    /**
     * Return selected nodes.
     */
    public Collection<OsmPrimitive> getSelectedNodes() {
        return getSelected(nodes);
    }

    /**
     * Return selected ways.
     */
    public Collection<OsmPrimitive> getSelectedWays() {
        return getSelected(ways);
    }

    /**
     * Return selected relations.
     */
    public Collection<OsmPrimitive> getSelectedRelations() {
        return getSelected(relations);
    }

    public void setSelected(Collection<? extends OsmPrimitive> selection) {
        clearSelection(nodes);
        clearSelection(ways);
        clearSelection(relations);
        for (OsmPrimitive osm : selection) {
            osm.setSelected(true);
        }
        fireSelectionChanged(selection);
    }

    public void setSelected(OsmPrimitive... osm) {
        if (osm.length == 1 && osm[0] == null) {
            setSelected();
            return;
        }
        clearSelection(nodes);
        clearSelection(ways);
        clearSelection(relations);
        for (OsmPrimitive o : osm)
            if (o != null) {
                o.setSelected(true);
            }
        fireSelectionChanged(Arrays.asList(osm));
    }

    /**
     * Remove the selection from every value in the collection.
     * @param list The collection to remove the selection from.
     */
    private void clearSelection(Collection<? extends OsmPrimitive> list) {
        if (list == null)
            return;
        for (OsmPrimitive osm : list) {
            osm.setSelected(false);
        }
    }

    /**
     * Return all selected items in the collection.
     * @param list The collection from which the selected items are returned.
     */
    private Collection<OsmPrimitive> getSelected(Collection<? extends OsmPrimitive> list) {
        Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
        if (list == null)
            return sel;
        for (OsmPrimitive osm : list)
            if (osm.isSelected() && !osm.isDeleted()) {
                sel.add(osm);
            }
        return sel;
    }

    /**
     * Remember to fire an selection changed event. A call to this will not fire the event
     * immediately. For more,
     * @see SelectionChangedListener
     */
    public static void fireSelectionChanged(Collection<? extends OsmPrimitive> sel) {
        for (SelectionChangedListener l : selListeners) {
            l.selectionChanged(sel);
        }
    }

    @Override public DataSet clone() {
        DataSet ds = new DataSet();
        for (Node n : nodes) {
            ds.nodes.add(new Node(n));
        }
        for (Way w : ways) {
            ds.ways.add(new Way(w));
        }
        for (Relation e : relations) {
            ds.relations.add(new Relation(e));
        }
        for (DataSource source : dataSources) {
            ds.dataSources.add(new DataSource(source.bounds, source.origin));
        }
        ds.version = version;
        return ds;
    }

    /**
     * Returns the total area of downloaded data (the "yellow rectangles").
     * @return Area object encompassing downloaded data.
     */
    public Area getDataSourceArea() {
        if (dataSources.isEmpty()) return null;
        Area a = new Area();
        for (DataSource source : dataSources) {
            // create area from data bounds
            a.add(new Area(source.bounds.asRect()));
        }
        return a;
    }

    // Provide well-defined sorting for collections of OsmPrimitives.
    // FIXME: probably not a good place to put this code.
    public static OsmPrimitive[] sort(Collection<? extends OsmPrimitive> list) {
        OsmPrimitive[] selArr = new OsmPrimitive[list.size()];
        final HashMap<Object, String> h = new HashMap<Object, String>();
        selArr = list.toArray(selArr);
        Arrays.sort(selArr, new Comparator<OsmPrimitive>() {
            public int compare(OsmPrimitive a, OsmPrimitive b) {
                if (a.getClass() == b.getClass()) {
                    String as = h.get(a);
                    if (as == null) {
                        as = a.getName() != null ? a.getName() : Long.toString(a.getId());
                        h.put(a, as);
                    }
                    String bs = h.get(b);
                    if (bs == null) {
                        bs = b.getName() != null ? b.getName() : Long.toString(b.getId());
                        h.put(b, bs);
                    }
                    int res = as.compareTo(bs);
                    if (res != 0)
                        return res;
                }
                return a.compareTo(b);
            }
        });
        return selArr;
    }

    /**
     * returns a  primitive with a given id from the data set. null, if no such primitive
     * exists
     *
     * @param id  the id, > 0 required
     * @param type the type of  the primitive. Must not be null.
     * @return the primitive
     * @exception IllegalArgumentException thrown, if id <= 0
     * @exception IllegalArgumentException thrown, if type is null
     * @exception IllegalArgumentException thrown, if type is neither NODE, or WAY or RELATION
     */
    public OsmPrimitive getPrimitiveById(long id, OsmPrimitiveType type) {
        if (id <= 0)
            throw new IllegalArgumentException(tr("parameter {0} > 0 required. Got {1}.", "id", id));
        if (id <= 0)
            throw new IllegalArgumentException(tr("paramete''{0}'' must not be null", "type"));
        Collection<? extends OsmPrimitive> primitives = null;
        switch(type) {
        case NODE: primitives = nodes; break;
        case WAY: primitives = ways; break;
        case RELATION: primitives = relations; break;
        case CHANGESET: throw new IllegalArgumentException(tr("unsupported value ''{0}'' or parameter ''{1}''", type, "type"));
        }
        for (OsmPrimitive primitive : primitives) {
            if (primitive.getId() == id) return primitive;
        }
        return null;
    }

    public Set<Long> getPrimitiveIds() {
        HashSet<Long> ret = new HashSet<Long>();
        for (OsmPrimitive primitive : nodes) {
            ret.add(primitive.getId());
        }
        for (OsmPrimitive primitive : ways) {
            ret.add(primitive.getId());
        }
        for (OsmPrimitive primitive : relations) {
            ret.add(primitive.getId());
        }
        return ret;
    }

    protected void deleteWay(Way way) {
        way.setNodes(null);
        way.setDeleted(true);
    }

    /**
     * removes all references from ways in this dataset to a particular node
     *
     * @param node the node
     */
    public void unlinkNodeFromWays(Node node) {
        for (Way way: ways) {
            List<Node> nodes = way.getNodes();
            if (nodes.remove(node)) {
                if (nodes.size() < 2) {
                    deleteWay(way);
                } else {
                    way.setNodes(nodes);
                }
            }
        }
    }

    /**
     * removes all references from relations in this dataset  to this primitive
     *
     * @param primitive the primitive
     */
    public void unlinkPrimitiveFromRelations(OsmPrimitive primitive) {
        for (Relation relation : relations) {
            Iterator<RelationMember> it = relation.getMembers().iterator();
            while(it.hasNext()) {
                RelationMember member = it.next();
                if (member.getMember().equals(primitive)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * removes all references from from other primitives  to the
     * referenced primitive
     *
     * @param referencedPrimitive the referenced primitive
     */
    public void unlinkReferencesToPrimitive(OsmPrimitive referencedPrimitive) {
        if (referencedPrimitive instanceof Node) {
            unlinkNodeFromWays((Node)referencedPrimitive);
            unlinkPrimitiveFromRelations(referencedPrimitive);
        } else {
            unlinkPrimitiveFromRelations(referencedPrimitive);
        }
    }

    /**
     * Replies a list of parent relations which refer to the relation
     * <code>child</code>. Replies an empty list if child is null.
     *
     * @param child the child relation
     * @return a list of parent relations which refer to the relation
     * <code>child</code>
     */
    public List<Relation> getParentRelations(Relation child) {
        ArrayList<Relation> parents = new ArrayList<Relation>();
        if (child == null)
            return parents;
        for (Relation parent : relations) {
            if (parent == child) {
                continue;
            }
            for (RelationMember member: parent.getMembers()) {
                if (member.refersTo(child)) {
                    parents.add(parent);
                    break;
                }
            }
        }
        return parents;
    }

    /**
     * Replies true if there is at least one primitive in this dataset with
     * {@see OsmPrimitive#isModified()} == <code>true</code>.
     * 
     * @return true if there is at least one primitive in this dataset with
     * {@see OsmPrimitive#isModified()} == <code>true</code>.
     */
    public boolean isModified() {
        for (Node n: nodes) {
            if (n.isModified()) return true;
        }
        for (Way w: ways) {
            if (w.isModified()) return true;
        }
        for (Relation r: relations) {
            if (r.isModified()) return true;
        }
        return false;
    }

    public Set<Relation> getReferringRelations(Collection<? extends OsmPrimitive> primitives) {
        HashSet<Relation> ret = new HashSet<Relation>();
        if (primitives == null) return ret;
        Set<? extends OsmPrimitive> referred;
        if (primitives instanceof Set<?>) {
            referred = (Set<? extends OsmPrimitive>)primitives;
        } else {
            referred = new HashSet<OsmPrimitive>(primitives);
        }
        referred.remove(null); // just in case - remove null element from primitives
        for (Relation r: relations) {
            if (r.isDeleted() || r.incomplete) {
                continue;
            }
            Set<OsmPrimitive> memberPrimitives = r.getMemberPrimitives();
            memberPrimitives.retainAll(referred);
            if (!memberPrimitives.isEmpty()) {
                ret.add(r);
            }
        }
        return ret;
    }
}
