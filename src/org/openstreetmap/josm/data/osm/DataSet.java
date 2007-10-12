// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.SelectionChangedListener;

/**
 * DataSet is the data behind the application. It can consists of only a few
 * points up to the whole osm database. DataSet's can be merged together,
 * saved, (up/down/disk)loaded etc.
 *
 * Note, that DataSet is not an osm-primitive and so has no key association
 * but a few members to store some information.
 *
 * @author imi
 */
public class DataSet implements Cloneable {

	/**
	 * All nodes goes here, even when included in other data (ways etc).
	 * This enables the instant conversion of the whole DataSet by iterating over
	 * this data structure.
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
	 * A list of listeners to selection changed events. The list is static,
	 * as listeners register themself for any dataset selection changes that 
	 * occour, regardless of the current active dataset. (However, the
	 * selection does only change in the active layer)
	 */
	public static Collection<SelectionChangedListener> selListeners = new LinkedList<SelectionChangedListener>();

	/**
	 * @return A collection containing all primitives of the dataset. The
	 * data is ordered after: first come nodes, then ways, then relations.
	 * Ordering in between the categories is not guaranteed.
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
			if (!osm.deleted)
				o.add(osm);
		return o;
	}

	public void addPrimitive(OsmPrimitive osm) {
		if (osm instanceof Node) {
			nodes.add((Node) osm);
		} else if (osm instanceof Way) {
			ways.add((Way) osm);
		} else if (osm instanceof Relation) { 
			relations.add((Relation) osm);
		}
	}

	/**
	 * Remove the selection of the whole dataset.
	 * @deprecated Use setSelected() instead.
	 */
	@Deprecated
	public void clearSelection() {
		clearSelection(nodes);
		clearSelection(ways);
		clearSelection(relations);
		Collection<OsmPrimitive> sel = Collections.emptyList();
		fireSelectionChanged(sel);
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

	public void setSelected(Collection<? extends OsmPrimitive> selection) {
		clearSelection(nodes);
		clearSelection(ways);
		clearSelection(relations);
		for (OsmPrimitive osm : selection)
			osm.selected = true;
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
			if (o != null)
				o.selected = true;
		fireSelectionChanged(Arrays.asList(osm));
	}

	/**
	 * Remove the selection from every value in the collection.
	 * @param list The collection to remove the selection from.
	 */
	private void clearSelection(Collection<? extends OsmPrimitive> list) {
		if (list == null)
			return;
		for (OsmPrimitive osm : list)
			osm.selected = false;
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
			if (osm.selected && !osm.deleted)
				sel.add(osm);
		return sel;
	}

	/**
	 * Remember to fire an selection changed event. A call to this will not fire
	 * the event immediately. For more, @see SelectionChangedListener
	 */
	public static void fireSelectionChanged(Collection<? extends OsmPrimitive> sel) {
		for (SelectionChangedListener l : selListeners)
			l.selectionChanged(sel);
	}
	
	@Override public DataSet clone() {
		DataSet ds = new DataSet();
		for (Node n : nodes)
			ds.nodes.add(new Node(n));
		for (Way w : ways)
			ds.ways.add(new Way(w));
		for (Relation e : relations)
			ds.relations.add(new Relation(e));
		for (DataSource source : dataSources)
			ds.dataSources.add(new DataSource(source.bounds, source.origin));
	    return ds;
    }
}
