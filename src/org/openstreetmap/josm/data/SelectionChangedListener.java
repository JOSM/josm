// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This is a listener for selection changes through the dataset's data. Whenever
 * a selection of any data meber changes, the dataSet gets informed about this
 * and fire a selectionChanged event.
 * 
 * Note, that these events get not fired immediately but are inserted in the
 * Swing-event queue and packed together. So only one selection changed event
 * are issued within one message dispatch routine.
 * 
 * @author imi
 */
public interface SelectionChangedListener {

	/**
	 * Informs the listener that the selection in the dataset has changed.
	 * @param newSelection The new selection.
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection);
}
