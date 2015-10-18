// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/**
 * The {@link TableCellRenderer} for a list of tags in {@link HistoryBrowser}
 *
 */
public class TagTableCellRenderer extends JLabel implements TableCellRenderer {
    public static final Color BGCOLOR_DIFFERENCE = new Color(255, 197, 197);

    /**
     * Constructs a new {@code TagTableCellRenderer}.
     */
    public TagTableCellRenderer() {
        setOpaque(true);
    }

    protected void renderName(String key, HistoryBrowserModel.TagTableModel model, boolean isSelected) {
        String text = key;
        Color bgColor = UIManager.getColor("Table.background");
        Color fgColor = UIManager.getColor("Table.foreground");
        if (!model.hasTag(key)) {
            text = "";
            bgColor = BGCOLOR_DIFFERENCE;
        } else if (!model.oppositeHasTag(key)) {
            bgColor = BGCOLOR_DIFFERENCE;
        }
        if (isSelected) {
            bgColor = UIManager.getColor("Table.backgroundSelected");
            fgColor = UIManager.getColor("Table.foregroundSelected");
        }

        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
        setForeground(fgColor);
    }

    protected void renderValue(String key, HistoryBrowserModel.TagTableModel model, boolean isSelected) {
        String text = "";
        Color bgColor = UIManager.getColor("Table.background");
        Color fgColor = UIManager.getColor("Table.foreground");
        if (!model.hasTag(key)) {
            bgColor = BGCOLOR_DIFFERENCE;
        } else {
            text = model.getValue(key);
            if (!model.hasSameValueAsOpposite(key)) {
                bgColor = BGCOLOR_DIFFERENCE;
            }
        }
        if (isSelected) {
            bgColor = UIManager.getColor("Table.backgroundSelected");
            fgColor = UIManager.getColor("Table.foregroundSelected");
        }

        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
        setForeground(fgColor);
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
            renderName(key, model, isSelected);
            break;
        case 1:
            // the value column
            renderValue(key, model, isSelected);
            break;
        }

        return this;
    }

    protected HistoryBrowserModel.TagTableModel getTagTableModel(JTable table) {
        return (HistoryBrowserModel.TagTableModel) table.getModel();
    }
}
