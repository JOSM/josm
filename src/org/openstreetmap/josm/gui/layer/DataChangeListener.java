// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer;

import org.openstreetmap.josm.data.osm.event.DataSetListener;

/**
 * 
 * 
 * @deprecated Use {@link DataSetListener} instead
 */
@Deprecated
public interface DataChangeListener {

    @Deprecated
    public void dataChanged(OsmDataLayer l);

}
