// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTable;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;

public abstract class PairTable extends OsmPrimitivesTable {

    private final ListMergeModel<? extends PrimitiveId> model;
    
    public PairTable(String name, ListMergeModel<? extends PrimitiveId> model, OsmPrimitivesTableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        this.model = model;
        setName(name);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    @Override
    public OsmPrimitive getPrimitiveInLayer(int row, OsmDataLayer layer) {
        OsmPrimitive result = super.getPrimitiveInLayer(row, layer);
        if (model != null && result != null && layer != null && result.getDataSet() != layer.data) {
            result = model.getMyPrimitiveById(result);
        }
        return result;
    }
}
