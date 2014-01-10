// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

public class TagConflictResolverColumnModel extends DefaultTableColumnModel{

    protected void createColumns() {
        TableColumn col = null;
        MultiValueCellRenderer renderer = new MultiValueCellRenderer();
        MultiValueCellEditor editor = new MultiValueCellEditor();

        // column 0 - State
        col = new TableColumn(0);
        col.setHeaderValue("");
        col.setResizable(true);
        col.setWidth(20);
        col.setPreferredWidth(20);
        col.setMaxWidth(30);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - Key
        col = new TableColumn(1);
        col.setHeaderValue(tr("Key"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 2 - Value
        col = new TableColumn(2);
        col.setHeaderValue(tr("Value"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setCellEditor(editor);
        addColumn(col);
    }

    /**
     * Constructs a new {@code TagConflictResolverColumnModel}.
     */
    public TagConflictResolverColumnModel() {
        createColumns();
    }
}
