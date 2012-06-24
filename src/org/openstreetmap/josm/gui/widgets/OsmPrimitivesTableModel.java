// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.table.TableModel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public interface OsmPrimitivesTableModel extends TableModel {
    
    public abstract OsmPrimitive getReferredPrimitive(int idx);
}
