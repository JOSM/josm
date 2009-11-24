// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

class SaveLayersTableColumnModel extends DefaultTableColumnModel {
    protected void build() {
        TableColumn col = null;
        TableCellRenderer renderer = new SaveLayerInfoCellRenderer();
        TableCellEditor fileNameEditor = new FilenameCellEditor();
        TableCellEditor saveFlagEditor = new SaveFlagCellEditor();

        // column 0 - Layer
        col = new TableColumn(0);
        col.setHeaderValue(tr("Layer"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setPreferredWidth(100);
        addColumn(col);

        // column 1 - Upload required
        col = new TableColumn(1);
        col.setHeaderValue(tr("Should upload?"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setPreferredWidth(50);
        addColumn(col);

        // column 2 - Save to file required
        col = new TableColumn(2);
        col.setHeaderValue(tr("Should save?"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setPreferredWidth(50);
        addColumn(col);

        // column 3 - filename
        col = new TableColumn(3);
        col.setHeaderValue(tr("Filename"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setCellEditor(fileNameEditor);
        col.setPreferredWidth(200);
        addColumn(col);

        // column 4 - Upload
        col = new TableColumn(4);
        col.setHeaderValue(tr("Upload"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setCellEditor(saveFlagEditor);
        col.setPreferredWidth(30);
        addColumn(col);

        // column 5 - Save
        col = new TableColumn(5);
        col.setHeaderValue(tr("Save"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setCellEditor(saveFlagEditor);
        col.setPreferredWidth(30);

        addColumn(col);
    }

    public SaveLayersTableColumnModel() {
        build();
    }
}
