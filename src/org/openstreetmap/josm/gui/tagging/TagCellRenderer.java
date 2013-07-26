// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

/**
 * This is the table cell renderer for cells for the table of tags
 * in the tag editor dialog.
 *
 *
 */
public class TagCellRenderer extends JLabel implements TableCellRenderer  {
    private Font fontStandard = null;
    private Font fontItalic = null;

    public TagCellRenderer() {
        fontStandard = UIManager.getFont("Table.font");
        fontItalic = fontStandard.deriveFont(Font.ITALIC);
        setOpaque(true);
        setBorder(new EmptyBorder(5,5,5,5));
    }

    /**
     * renders the name of a tag in the second column of
     * the table
     *
     * @param tag  the tag
     */
    protected void renderTagName(TagModel tag) {
        setText(tag.getName());
    }

    /**
     * renders the value of a a tag in the third column of
     * the table
     *
     * @param tag  the  tag
     */
    protected void renderTagValue(TagModel tag) {
        if (tag.getValueCount() == 0) {
            setText("");
        } else if (tag.getValueCount() == 1) {
            setText(tag.getValues().get(0));
        } else if (tag.getValueCount() >  1) {
            setText(tr("multiple"));
            setFont(fontItalic);
        }
    }

    /**
     * resets the renderer
     */
    protected void resetRenderer() {
        setText("");
        setIcon(null);
        setFont(fontStandard);
    }

    protected TagEditorModel getModel(JTable table) {
        return (TagEditorModel)table.getModel();
    }

    /**
     * replies the cell renderer component for a specific cell
     *
     * @param table  the table
     * @param value the value to be rendered
     * @param isSelected  true, if the value is selected
     * @param hasFocus true, if the cell has focus
     * @param rowIndex the row index
     * @param vColIndex the column index
     *
     * @return the renderer component
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
        resetRenderer();
        if (value == null)
            return this;

        // set background color
        //
        if (isSelected){
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            setBackground(UIManager.getColor("Table.background")); // standard color
            setForeground(UIManager.getColor("Table.foreground"));
        }

        switch(vColIndex) {
        case 0: renderTagName((TagModel)value); break;
        case 1: renderTagValue((TagModel)value); break;

        default: throw new RuntimeException("unexpected index in switch statement");
        }
        if (hasFocus && isSelected) {
            if (table.getSelectedColumnCount() == 1 && table.getSelectedRowCount() == 1) {
                if (table.getEditorComponent() != null) {
                    table.getEditorComponent().requestFocusInWindow();
                }
            }
        }
        return this;
    }
}
