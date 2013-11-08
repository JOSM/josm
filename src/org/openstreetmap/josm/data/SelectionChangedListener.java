// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This is a listener for selection changes through the dataset's data. Whenever
 * a selection of any data member changes, the dataSet gets informed about this
 * and fires a selectionChanged event.
 *
 * Note that these events are not fired immediately but are inserted in the
 * Swing event queue and packed together. So only one selection changed event
 * is issued within a one message dispatch routine.
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
