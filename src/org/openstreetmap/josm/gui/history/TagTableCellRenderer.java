// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * The {@see TableCellRenderer} for a list of tagsin {@see HistoryBrower}
 * 
 */
public class TagTableCellRenderer extends JLabel implements TableCellRenderer {

    static private Logger logger = Logger.getLogger(TagTableCellRenderer.class.getName());

    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_DIFFERENCE = new Color(255,197,197);

    public TagTableCellRenderer() {
        setOpaque(true);
        setForeground(Color.BLACK);
    }

    protected void renderName(String key, HistoryBrowserModel.TagTableModel model, boolean isSelected) {
        String text = key;
        Color bgColor = Color.WHITE;
        if (! model.hasTag(key)) {
            text = tr("<undefined>");
            bgColor = BGCOLOR_DIFFERENCE;
        } else if (!model.oppositeHasTag(key)) {
            bgColor = BGCOLOR_DIFFERENCE;
        }
        if (isSelected) {
            bgColor = BGCOLOR_SELECTED;
        }
        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
    }

    protected void renderValue(String key, HistoryBrowserModel.TagTableModel model, boolean isSelected) {
        String text = "";
        Color bgColor = Color.WHITE;
        if (! model.hasTag(key)) {
            text = tr("<undefined>");
            bgColor = BGCOLOR_DIFFERENCE;
        } else {
            text = model.getValue(key);
            if (!model.hasSameValueAsOpposite(key)) {
                bgColor = BGCOLOR_DIFFERENCE;
            }
        }
        if (isSelected) {
            bgColor = BGCOLOR_SELECTED;
        }

        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        String key = (String)value;
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
