// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.table.TableModel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * A table model that displays OSM primitives in it's rows
 */
public interface OsmPrimitivesTableModel extends TableModel {

    /**
     * Gets the primitive at a given row index
     * @param idx The row
     * @return The primitive in that row
     */
    OsmPrimitive getReferredPrimitive(int idx);
}
