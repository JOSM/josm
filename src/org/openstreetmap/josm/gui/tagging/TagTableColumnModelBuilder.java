// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Builder class allowing to construct customized tag table column models.
 * All columns are resizable and share the same renderer.
 * @since 9847
 */
public class TagTableColumnModelBuilder {

    private final DefaultTableColumnModel model = new DefaultTableColumnModel();

    /**
     * Construct a new {@code TagTableColumnModelBuilder}.
     * @param renderer rendered used for all columns
     * @param headerValues header values of each column, determining the number of columns
     * @see TableColumn#setHeaderValue
     * @see TableColumn#setCellRenderer
     */
    public TagTableColumnModelBuilder(TableCellRenderer renderer, String... headerValues) {
        CheckParameterUtil.ensureParameterNotNull(headerValues, "headerValues");
        for (int i = 0; i < headerValues.length; i++) {
            TableColumn col = new TableColumn(i);
            col.setHeaderValue(headerValues[i]);
            col.setResizable(true);
            col.setCellRenderer(renderer);
            model.addColumn(col);
        }
    }

    /**
     * Sets width of specified columns.
     * @param width the new width
     * @param indexes indexes of columns to setup
     * @return {@code this}
     * @see TableColumn#setWidth
     */
    public TagTableColumnModelBuilder setWidth(int width, int... indexes) {
        for (int i : indexes) {
            model.getColumn(i).setWidth(width);
        }
        return this;
    }

    /**
     * Sets preferred width of specified columns.
     * @param width the new width
     * @param indexes indexes of columns to setup
     * @return {@code this}
     * @see TableColumn#setPreferredWidth
     */
    public TagTableColumnModelBuilder setPreferredWidth(int width, int... indexes) {
        for (int i : indexes) {
            model.getColumn(i).setPreferredWidth(width);
        }
        return this;
    }

    /**
     * Sets max width of specified columns.
     * @param width the new width
     * @param indexes indexes of columns to setup
     * @return {@code this}
     * @see TableColumn#setMaxWidth
     */
    public TagTableColumnModelBuilder setMaxWidth(int width, int... indexes) {
        for (int i : indexes) {
            model.getColumn(i).setMaxWidth(width);
        }
        return this;
    }

    /**
     * Sets cell editor of specified columns.
     * @param editor the new cell editor
     * @param indexes indexes of columns to setup
     * @return {@code this}
     * @see TableColumn#setCellEditor
     */
    public TagTableColumnModelBuilder setCellEditor(TableCellEditor editor, int... indexes) {
        for (int i : indexes) {
            model.getColumn(i).setCellEditor(editor);
        }
        return this;
    }

    /**
     * Sets selection model.
     * @param selectionModel new selection model
     * @return {@code this}
     * @see DefaultTableColumnModel#setSelectionModel
     */
    public TagTableColumnModelBuilder setSelectionModel(ListSelectionModel selectionModel) {
        model.setSelectionModel(selectionModel);
        return this;
    }

    /**
     * Returns the new tag table column model.
     * @return the new tag table column model
     */
    public DefaultTableColumnModel build() {
        return model;
    }
}
