// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmData;

/**
 * This interface indicates that the class can fire {@link IDataSelectionListener}.
 * @author Taylor Smock, Michael Zangl (original code)
 * @param <O> the base type of OSM primitives
 * @param <N> type representing OSM nodes
 * @param <W> type representing OSM ways
 * @param <R> type representing OSM relations
 * @param <D> The dataset type
 * @since xxx
 */
public interface IDataSelectionEventSource<O extends IPrimitive, N extends INode, W extends IWay<N>, R extends IRelation<?>,
       D extends OsmData<O, N, W, R>> {
    /**
     * Add a listener
     * @param listener The listener to add
     * @return {@code true} if the listener was added
     */
    boolean addSelectionListener(IDataSelectionListener<O, N, W, R, D> listener);

    /**
     * Remove a listener
     * @param listener The listener to remove
     * @return {@code true} if the listener was removed
     */
    boolean removeSelectionListener(IDataSelectionListener<O, N, W, R, D> listener);
}
