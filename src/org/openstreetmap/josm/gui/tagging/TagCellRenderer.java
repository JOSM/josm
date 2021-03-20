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

import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * This is the table cell renderer for cells for the table of tags
 * in the tag editor dialog.
 *
 *
 */
public class TagCellRenderer extends JLabel implements TableCellRenderer {
    private final Font fontStandard;
    private final Font fontItalic;

    /**
     * Constructs a new {@code TagCellRenderer}.
     */
    public TagCellRenderer() {
        fontStandard = UIManager.getFont("Table.font");
        fontItalic = fontStandard.deriveFont(Font.ITALIC);
        setOpaque(true);
        setBorder(new EmptyBorder(5, 5, 5, 5));
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
        if (tag.getValueCount() > 1) {
            setText(tr("multiple"));
            setFont(fontItalic);
        } else {
            setText(tag.getValue());
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
        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            setBackground(UIManager.getColor("Table.background")); // standard color
            setForeground(UIManager.getColor("Table.foreground"));
        }

        switch(vColIndex) {
            case 0: renderTagName((TagModel) value); break;
            case 1: renderTagValue((TagModel) value); break;
            default: throw new JosmRuntimeException("unexpected index in switch statement");
        }
        if (hasFocus && isSelected && table.getSelectedColumnCount() == 1 && table.getSelectedRowCount() == 1
                && table.getEditorComponent() != null) {
            table.getEditorComponent().requestFocusInWindow();
        }
        return this;
    }
}
