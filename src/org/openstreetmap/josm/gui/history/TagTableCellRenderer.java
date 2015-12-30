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
    public static final Color BGCOLOR_SELECTED = new Color(143, 170, 255);

    /**
     * Constructs a new {@code TagTableCellRenderer}.
     */
    public TagTableCellRenderer() {
        setOpaque(true);
    }

    protected void setBackgroundReadable(String key, HistoryBrowserModel.TagTableModel model, boolean isSelected) {
        Color bgColor = UIManager.getColor("Table.background");
        if (!model.hasTag(key) && model.isCurrentPointInTime()
                || !model.oppositeHasTag(key) && model.isReferencePointInTime()) {
            bgColor = TwoColumnDiff.Item.DiffItemType.DELETED.getColor();
        } else if (!model.oppositeHasTag(key) && model.isCurrentPointInTime()
                || !model.hasTag(key) && model.isReferencePointInTime()) {
            bgColor = TwoColumnDiff.Item.DiffItemType.INSERTED.getColor();
        } else if (model.hasTag(key) && model.oppositeHasTag(key) && !model.hasSameValueAsOpposite(key)) {
            bgColor = TwoColumnDiff.Item.DiffItemType.CHANGED.getColor();
        }
        if (isSelected) {
            bgColor = BGCOLOR_SELECTED;
        }

        GuiHelper.setBackgroundReadable(this, bgColor);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        if (value == null)
            return this;

        String key = (String) value;
        HistoryBrowserModel.TagTableModel model = getTagTableModel(table);

        switch(column) {
        case 0:
            // the name column
            setText(model.hasTag(key) ? key : "");
            setToolTipText(getText());
            setBackgroundReadable(key, model, isSelected);
            break;
        case 1:
            // the value column
            setText(model.hasTag(key) ? model.getValue(key) : "");
            setToolTipText(getText());
            setBackgroundReadable(key, model, isSelected);
            break;
        }

        return this;
    }

    protected HistoryBrowserModel.TagTableModel getTagTableModel(JTable table) {
        return (HistoryBrowserModel.TagTableModel) table.getModel();
    }
}
