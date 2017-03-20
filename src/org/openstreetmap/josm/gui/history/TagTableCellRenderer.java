// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * The {@link TableCellRenderer} for a list of tags in {@link HistoryBrowser}
 *
 */
public class TagTableCellRenderer extends JLabel implements TableCellRenderer {
    /**
     * The background color for a selected row that has the focus.
     */
    public static final Color BGCOLOR_SELECTED_FOCUS = new Color(0xff8faaff);
    /**
     * The background color for a selected row while the table is not focused.
     */
    public static final Color BGCOLOR_SELECTED = new Color(0xffafc2ff);

    /**
     * Constructs a new {@code TagTableCellRenderer}.
     */
    public TagTableCellRenderer() {
        setOpaque(true);
    }

    protected void setBackgroundReadable(String key, TagTableModel model, boolean isSelected, boolean hasFocus, boolean isValue) {
        Color bgColor = UIManager.getColor("Table.background");
        if ((!model.hasTag(key) && model.isCurrentPointInTime())
                || (!model.oppositeHasTag(key) && model.isReferencePointInTime())) {
            bgColor = TwoColumnDiff.Item.DiffItemType.DELETED.getColor();
        } else if ((!model.oppositeHasTag(key) && model.isCurrentPointInTime())
                || (!model.hasTag(key) && model.isReferencePointInTime())) {
            bgColor = TwoColumnDiff.Item.DiffItemType.INSERTED.getColor();
        } else if (isValue && model.hasTag(key) && model.oppositeHasTag(key) && !model.hasSameValueAsOpposite(key)) {
            bgColor = TwoColumnDiff.Item.DiffItemType.CHANGED.getColor();
        }
        if (isSelected) {
            if (hasFocus) {
                bgColor = BGCOLOR_SELECTED_FOCUS;
            } else {
                bgColor = BGCOLOR_SELECTED;
            }
        }

        GuiHelper.setBackgroundReadable(this, bgColor);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        if (value == null)
            return this;

        String key = (String) value;
        TagTableModel model = getTagTableModel(table);

        String text = "";
        if (model.hasTag(key)) {
            switch(column) {
            case TagTableColumnModel.COLUMN_KEY:
                // the name column
                text = key;
                break;
            case TagTableColumnModel.COLUMN_VALUE:
                // the value column
                text = model.getValue(key);
                break;
            default: // Do nothing
            }
        }

        setText(text);
        setToolTipText(text);
        setBackgroundReadable(key, model, isSelected, table.hasFocus(), column == TagTableColumnModel.COLUMN_VALUE);
        return this;
    }

    protected TagTableModel getTagTableModel(JTable table) {
        return (TagTableModel) table.getModel();
    }
}
