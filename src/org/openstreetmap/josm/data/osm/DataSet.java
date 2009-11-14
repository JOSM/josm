// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private static class IdHash implements Hash<PrimitiveId,OsmPrimitive> {

        public int getHashCode(PrimitiveId k) {
            return (int)k.getUniqueId() ^ k.getType().hashCode();
        }

        public boolean equals(PrimitiveId key, OsmPrimitive value) {
            if (key == null || value == null) return false;
            return key.getUniqueId() == value.getUniqueId() && key.getType() == value.getType();
        }
    }

    private Storage<OsmPrimitive> allPrimitives = new Storage<OsmPrimitive>(new IdHash());
    private Map<PrimitiveId, OsmPrimitive> primitivesMap = allPrimitives.foreignKey(new IdHash());
    private List<DataSetListener> listeners = new ArrayList<DataSetListener>();

    /**
     * The API version that created this data set, if any.
     */
    private String version;

    /**
     * Replies the API version this dataset was created from. May be null.
     * 
     * @return the API version this dataset was created from. May be null.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the API version this dataset was created from.
     * 
     * @param version the API version, i.e. "0.5" or "0.6"
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * All nodes goes here, even when included in other data (ways etc). This enables the instant
     * conversion of the whole DataSet by iterating over this data structure.
     */
    private QuadBuckets<Node> nodes = new QuadBuckets<Node>();

    /**
     * Replies an unmodifiable collection of nodes in this dataset
     * 
     * @return an unmodifiable collection of nodes in this dataset
     */
    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodes);
    }

    public List<Node> searchNodes(BBox bbox) {
        return nodes.search(bbox);
    }

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The way nodes are stored only in the way list.
     */
    private QuadBuckets<Way> ways = new QuadBuckets<Way>();

    /**
     * Replies an unmodifiable collection of ways in this dataset
     * 
     * @return an unmodifiable collection of ways in this dataset
     */
    public Collection<Way> getWays() {
        return Collections.unmodifiableCollection(ways);
    }

    public List<Way> searchWays(BBox bbox) {
        return ways.search(bbox);
    }

    /**
     * All relations/relationships
     */
    private Collection<Relation> relations = new LinkedList<Relation>();

    /**
     * Replies an unmodifiable collection of relations in this dataset
     * 
     * @return an unmodifiable collection of relations in this dataset
     */
    public Collection<Relation> getRelations() {
        return Collections.unmodifiableCollection(relations);
    }

    /**
     * All data sources of this DataSet.
     */
    public Collection<DataSource> dataSources = new LinkedList<DataSource>();

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
        if (getPrimitiveById(primitive) != null)
            throw new DataIntegrityProblemException(
                    tr("Unable to add primitive {0} to the dataset because it's already included", primitive.toString()));

        if (primitive instanceof Node) {
            nodes.add((Node) primitive);
        } else if (primitive instanceof Way) {
            ways.add((Way) primitive);
        } else if (primitive instanceof Relation) {
            relations.add((Relation) primitive);
        }
        allPrimitives.add(primitive);
        primitive.setDataset(this);
        firePrimitivesAdded(Collections.singletonList(primitive));
    }

    public OsmPrimitive addPrimitive(PrimitiveData data) {
        OsmPrimitive result;
        if (data instanceof NodeData) {
            result = new Node();
        } else if (data instanceof WayData) {
            result = new Way();
        } else if (data instanceof RelationData) {
            result = new Relation();
        } else
            throw new AssertionError();
        result.setDataset(this);
        result.load(data);
        addPrimitive(result);
        return result;
    }

    /**
     * Removes a primitive from the dataset. This method only removes the
     * primitive form the respective collection of primitives managed
     * by this dataset, i.e. from {@see #nodes}, {@see #ways}, or
     * {@see #relations}. References from other primitives to this
     * primitive are left unchanged.
     *
     * @param primitive the primitive
     */
    public void removePrimitive(PrimitiveId primitiveId) {
        OsmPrimitive primitive = getPrimitiveByIdChecked(primitiveId);
        if (primitive == null)
            return;
        if (primitive instanceof Node) {
            nodes.remove(primitive);
        } else if (primitive instanceof Way) {
            ways.remove(primitive);
        } else if (primitive instanceof Relation) {
            relations.remove(primitive);
        }
        selectedPrimitives.remove(primitive);
        allPrimitives.remove(primitive);
        primitive.setDataset(null);
        firePrimitivesRemoved(Collections.singletonList(primitive));
    }


    /*---------------------------------------------------
     *   SELECTION HANDLING
     *---------------------------------------------------*/

    /**
     * A list of listeners to selection changed events. The list is static, as listeners register
     * themselves for any dataset selection changes that occur, regardless of the current active
     * dataset. (However, the selection does only change in the active layer)
     */
    public static Collection<SelectionChangedListener> selListeners = new LinkedList<SelectionChangedListener>();

    /**
     * notifies all registered selection change listeners about the current selection of
     * primitives
     * 
     * @param sel the current selection
     */
    private static void notifySelectionChangeListeners(Collection<? extends OsmPrimitive> sel) {
        for (SelectionChangedListener l : selListeners) {
            l.selectionChanged(sel);
        }
    }

    /**
     * Notifies all registered {@see SelectionChangedListener} about the current selection in
     * this dataset.
     * 
     */
    public void fireSelectionChanged(){
        notifySelectionChangeListeners(selectedPrimitives);
    }


    LinkedHashSet<OsmPrimitive> selectedPrimitives = new LinkedHashSet<OsmPrimitive>();

    public Collection<OsmPrimitive> getSelectedNodesAndWays() {
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : selectedPrimitives) {
            if (osm instanceof Way ||
                    osm instanceof Node) {
                sel.add(osm);
            }
        }
        return sel;
    }


    /**
     * Return a list of all selected objects. Even keys are returned.
     * @return List of all selected objects.
     */
    public Collection<OsmPrimitive> getSelected() {
        // It would be nice to have this be a copy-on-write list
        // or an Collections.unmodifiableList().  It would be
        // much faster for large selections.  May users just
        // call this, and only check the .size().
        return new ArrayList<OsmPrimitive>(selectedPrimitives);
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

    /**
     * Return all selected items in the collection.
     * @param list The collection from which the selected items are returned.
     */
    private Collection<OsmPrimitive> getSelected(Collection<? extends OsmPrimitive> list) {
        if (list == null)
            return new LinkedList<OsmPrimitive>();
        // getSelected() is called with large lists, so
        // creating the return list from the selection
        // should be faster most of the time.
        Collection<OsmPrimitive> sel = new LinkedHashSet<OsmPrimitive>(selectedPrimitives);
        sel.retainAll(list);
        return sel;
    }

    public boolean isSelected(OsmPrimitive osm) {
        return selectedPrimitives.contains(osm);
    }


    public void toggleSelected(Collection<? extends PrimitiveId> osm) {
        boolean changed = false;
        for (PrimitiveId o : osm) {
            changed = changed | this.__toggleSelected(o);
        }
        if (changed) {
            fireSelectionChanged();
        }
    }
    public void toggleSelected(PrimitiveId... osm) {
        toggleSelected(Arrays.asList(osm));
    }
    private boolean __toggleSelected(PrimitiveId primitiveId) {
        OsmPrimitive primitive = getPrimitiveByIdChecked(primitiveId);
        if (primitive == null)
            return false;
        if (!selectedPrimitives.remove(primitive)) {
            selectedPrimitives.add(primitive);
        }
        return true;
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>.
     * Notifies all {@see SelectionChangedListener} if <code>fireSelectionChangeEvent</code> is true.
     *
     * @param selection the selection
     * @param fireSelectionChangeEvent true, if the selection change listeners are to be notified; false, otherwise
     */
    public void setSelected(Collection<? extends PrimitiveId> selection, boolean fireSelectionChangeEvent) {
        boolean wasEmpty = selectedPrimitives.isEmpty();
        selectedPrimitives = new LinkedHashSet<OsmPrimitive>();
        addSelected(selection, fireSelectionChangeEvent);
        if (!wasEmpty && selectedPrimitives.isEmpty() && fireSelectionChangeEvent) {
            fireSelectionChanged();
        }
    }

    /**
     * Sets the current selection to the primitives in <code>selection</code>
     * and notifies all {@see SelectionChangedListener}.
     *
     * @param selection the selection
     */
    public void setSelected(Collection<? extends PrimitiveId> selection) {
        setSelected(selection, true /* fire selection change event */);
    }

    public void setSelected(PrimitiveId... osm) {
        if (osm.length == 1 && osm[0] == null) {
            setSelected();
            return;
        }
        List<PrimitiveId> list = Arrays.asList(osm);
        setSelected(list);
    }

    /**
     * Adds   the primitives in <code>selection</code> to the current selection
     * and notifies all {@see SelectionChangedListener}.
     *
     * @param selection the selection
     */
    public void addSelected(Collection<? extends PrimitiveId> selection) {
        addSelected(selection, true /* fire selection change event */);
    }

    public void addSelected(PrimitiveId... osm) {
        addSelected(Arrays.asList(osm));
    }

    /**
     * Adds the primitives in <code>selection</code> to the current selection.
     * Notifies all {@see SelectionChangedListener} if <code>fireSelectionChangeEvent</code> is true.
     *
     * @param selection the selection
     * @param fireSelectionChangeEvent true, if the selection change listeners are to be notified; false, otherwise
     */
    public void addSelected(Collection<? extends PrimitiveId> selection, boolean fireSelectionChangeEvent) {
        boolean changed = false;
        for (PrimitiveId id: selection) {
            OsmPrimitive primitive = getPrimitiveByIdChecked(id);
            if (primitive != null) {
                changed = changed | selectedPrimitives.add(primitive);
            }
        }
        if (fireSelectionChangeEvent && changed) {
            fireSelectionChanged();
        }
    }

    /**
     * Remove the selection from every value in the collection.
     * @param list The collection to remove the selection from.
     */
    public void clearSelection(PrimitiveId... osm) {
        clearSelection(Arrays.asList(osm));
    }
    public void clearSelection(Collection<? extends PrimitiveId> list) {
        boolean changed = false;
        for (PrimitiveId id:list) {
            OsmPrimitive primitive = getPrimitiveById(id);
            if (primitive != null) {
                changed = changed | selectedPrimitives.remove(primitive);
            }
        }
        if (changed) {
            fireSelectionChanged();
        }
    }
    public void clearSelection() {
        if (!selectedPrimitives.isEmpty()) {
            selectedPrimitives.clear();
            fireSelectionChanged();
        }
    }


    /*------------------------------------------------------
     * FILTERED / DISABLED HANDLING
     *-----------------------------------------------------*/

    public void setDisabled(OsmPrimitive... osm) {
        if (osm.length == 1 && osm[0] == null) {
            setDisabled();
            return;
        }
        clearDisabled(nodes);
        clearDisabled(ways);
        clearDisabled(relations);
        for (OsmPrimitive o : osm)
            if (o != null) {
                o.setDisabled(true);
            }
    }

    public void setFiltered(Collection<? extends OsmPrimitive> selection) {
        clearFiltered(nodes);
        clearFiltered(ways);
        clearFiltered(relations);
        for (OsmPrimitive osm : selection) {
            osm.setFiltered(true);
        }
    }

    public void setFiltered(OsmPrimitive... osm) {
        if (osm.length == 1 && osm[0] == null) {
            setFiltered();
            return;
        }
        clearFiltered(nodes);
        clearFiltered(ways);
        clearFiltered(relations);
        for (OsmPrimitive o : osm)
            if (o != null) {
                o.setFiltered(true);
            }
    }

    public void setDisabled(Collection<? extends OsmPrimitive> selection) {
        clearDisabled(nodes);
        clearDisabled(ways);
        clearDisabled(relations);
        for (OsmPrimitive osm : selection) {
            osm.setDisabled(true);
        }
    }


    /**
     * Remove the filtered parameter from every value in the collection.
     * @param list The collection to remove the filtered parameter from.
     */
    private void clearFiltered(Collection<? extends OsmPrimitive> list) {
        if (list == null)
            return;
        for (OsmPrimitive osm : list) {
            osm.setFiltered(false);
        }
    }
    /**
     * Remove the disabled parameter from every value in the collection.
     * @param list The collection to remove the disabled parameter from.
     */
    private void clearDisabled(Collection<? extends OsmPrimitive> list) {
        if (list == null)
            return;
        for (OsmPrimitive osm : list) {
            osm.setDisabled(false);
        }
    }


    @Override public DataSet clone() {
        DataSet ds = new DataSet();
        for (Node n : nodes) {
            ds.addPrimitive(new Node(n));
        }
        for (Way w : ways) {
            ds.addPrimitive(new Way(w));
        }
        for (Relation e : relations) {
            ds.addPrimitive(new Relation(e));
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
     * @param id  uniqueId of the primitive. Might be < 0 for newly created primitives
     * @param type the type of  the primitive. Must not be null.
     * @return the primitive
     * @exception NullPointerException thrown, if type is null
     */
    public OsmPrimitive getPrimitiveById(long id, OsmPrimitiveType type) {
        return getPrimitiveById(new SimplePrimitiveId(id, type), false);
    }

    public OsmPrimitive getPrimitiveById(PrimitiveId primitiveId) {
        return getPrimitiveById(primitiveId, false);
    }

    public OsmPrimitive getPrimitiveById(PrimitiveId primitiveId, boolean createNew) {
        OsmPrimitive result = primitivesMap.get(primitiveId);

        if (result == null && createNew) {
            switch (primitiveId.getType()) {
            case NODE: result = new Node(primitiveId.getUniqueId(), true); break;
            case WAY: result = new Way(primitiveId.getUniqueId(), true); break;
            case RELATION: result = new Relation(primitiveId.getUniqueId(), true); break;
            }
            addPrimitive(result);
        }

        return result;
    }

    /**
     * Show message and stack trace in log in case primitive is not found
     * @param primitiveId
     * @return Primitive by id.
     */
    private OsmPrimitive getPrimitiveByIdChecked(PrimitiveId primitiveId) {
        OsmPrimitive result = getPrimitiveById(primitiveId);
        if (result == null) {
            System.out.println(tr("JOSM expected to find primitive [{0} {1}] in dataset but it's not there. Please report this "
                    + " at http://josm.openstreetmap.de . This is not a critical error, it should be safe to continue in your work.",
                    primitiveId.getType(), Long.toString(primitiveId.getUniqueId())));
            new Exception().printStackTrace();
        }

        return result;
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

    /**
     * Reindex all nodes and ways after their coordinates were changed. This is a temporary solution, reindexing should
     * be automatic in the future
     * @deprecated Reindexing should be automatic
     */
    @Deprecated
    public void reindexAll() {
        List<Node> ntmp = new ArrayList<Node>(nodes);
        nodes.clear();
        nodes.addAll(ntmp);
        List<Way> wtmp = new ArrayList<Way>(ways);
        ways.clear();
        ways.addAll(wtmp);
    }

    private void reindexNode(Node node) {
        nodes.remove(node);
        nodes.add(node);
        for (Way way:OsmPrimitive.getFilteredList(node.getReferrers(), Way.class)) {
            ways.remove(way);
            way.updatePosition();
            ways.add(way);
        }
    }

    private void reindexWay(Way way) {
        ways.remove(way);
        way.updatePosition();
        ways.add(way);
    }


    public void addDataSetListener(DataSetListener dsl) {
        listeners.add(dsl);
    }

    public void removeDataSetListener(DataSetListener dsl) {
        listeners.remove(dsl);
    }

    void firePrimitivesAdded(Collection<? extends OsmPrimitive> added) {
        for (DataSetListener dsl : listeners) {
            dsl.primtivesAdded(added);
        }
    }

    void firePrimitivesRemoved(Collection<? extends OsmPrimitive> removed) {
        for (DataSetListener dsl : listeners) {
            dsl.primtivesRemoved(removed);
        }
    }

    void fireTagsChanged(OsmPrimitive prim) {
        for (DataSetListener dsl : listeners) {
            dsl.tagsChanged(prim);
        }
    }

    void fireRelationMembersChanged(Relation r) {
        for (DataSetListener dsl : listeners) {
            dsl.relationMembersChanged(r);
        }
    }

    public void fireNodeMoved(Node node) {
        reindexNode(node);
        for (DataSetListener dsl : listeners) {
            dsl.nodeMoved(node);
        }
    }

    public void fireWayNodesChanged(Way way) {
        reindexWay(way);
        for (DataSetListener dsl : listeners) {
            dsl.wayNodesChanged(way);
        }
    }

    public void clenupDeletedPrimitives() {
        if (cleanupDeleted(nodes.iterator())
                | cleanupDeleted(ways.iterator())
                | cleanupDeleted(relations.iterator())) {
            fireSelectionChanged();
        }
    }

    private boolean cleanupDeleted(Iterator<? extends OsmPrimitive> it) {
        boolean changed = false;
        while (it.hasNext()) {
            OsmPrimitive primitive = it.next();
            if (primitive.isDeleted()) {
                selectedPrimitives.remove(primitive);
                allPrimitives.remove(primitive);
                primitive.setDataset(null);
                changed = true;
                it.remove();
            }
        }
        return changed;
    }

    /**
     * Removes all primitives from the dataset and resets the currently selected primitives
     * to the empty collection. Also notifies selection change listeners if necessary.
     * 
     */
    public void clear() {
        clearSelection();
        for (OsmPrimitive primitive:allPrimitives) {
            primitive.setDataset(null);
        }
        nodes.clear();
        ways.clear();
        relations.clear();
        allPrimitives.clear();
    }
}
