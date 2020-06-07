// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.command.conflict.ConflictResolveCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTable;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;

/**
 * This is the base class for all tables that display an {@link AbstractListMergeModel}.
 */
public abstract class PairTable extends OsmPrimitivesTable {

    private final transient AbstractListMergeModel<? extends PrimitiveId, ? extends ConflictResolveCommand> model;

    /**
     * Constructs a new {@code PairTable}.
     * @param name table name
     * @param model merge model
     * @param dm table model
     * @param cm column model
     * @param sm selection model
     */
    protected PairTable(String name, AbstractListMergeModel<? extends PrimitiveId, ? extends ConflictResolveCommand> model,
            OsmPrimitivesTableModel dm, TableColumnModel cm, ListSelectionModel sm) {
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
