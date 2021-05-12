// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * The {@link TableColumnModel} for the table with the list of versions
 * @since 1709
 */
public class VersionTableColumnModel extends DefaultTableColumnModel {

    /** Column index for version */
    public static final int COL_VERSION = 0;
    /** Column index for reference */
    public static final int COL_REFERENCE = 1;
    /** Column index for current */
    public static final int COL_CURRENT = 2;
    /** Column index for date */
    public static final int COL_DATE = 3;
    /** Column index for user */
    public static final int COL_USER = 4;
    /** Column index for editor */
    public static final int COL_EDITOR = 5;

    /**
     * Creates a new {@code VersionTableColumnModel}.
     */
    public VersionTableColumnModel() {
        createColumns();
    }

    protected void createColumns() {
        VersionTable.RadioButtonRenderer bRenderer = new VersionTable.RadioButtonRenderer();

        // column 0 - Version
        TableColumn col = new TableColumn(COL_VERSION);
        /* translation note: 3 letter abbr. for "Version" */
        col.setHeaderValue(tr("Ver"));
        col.setCellRenderer(new VersionTableCellRenderer());
        col.setResizable(false);
        addColumn(col);
        // column 1 - Reference
        col = new TableColumn(COL_REFERENCE);
        col.setHeaderValue(tr("A"));
        col.setCellRenderer(bRenderer);
        col.setCellEditor(new VersionTable.RadioButtonEditor());
        col.setResizable(false);
        addColumn(col);
        // column 2 - Current
        col = new TableColumn(COL_CURRENT);
        col.setHeaderValue(tr("B"));
        col.setCellRenderer(bRenderer);
        col.setCellEditor(new VersionTable.RadioButtonEditor());
        col.setResizable(false);
        addColumn(col);
        // column 3 - Date
        col = new TableColumn(COL_DATE);
        col.setHeaderValue(tr("Date"));
        col.setResizable(false);
        addColumn(col);
        // column 4 - User
        col = new TableColumn(COL_USER);
        col.setHeaderValue(tr("User"));
        col.setResizable(false);
        addColumn(col);
        // column 5 - Editor
        col = new TableColumn(COL_EDITOR);
        col.setHeaderValue(tr("Editor"));
        col.setResizable(false);
        addColumn(col);
    }
}
