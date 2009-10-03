// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * The {@see TableCellRenderer} for a list of tagsin {@see HistoryBrower}
 *
 */
public class CoordinateTableCellRenderer extends JLabel implements TableCellRenderer {

    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_DIFFERENCE = new Color(255,197,197);

    public CoordinateTableCellRenderer() {
        setOpaque(true);
        setForeground(Color.BLACK);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        String key = (String)value;
        HistoryBrowserModel.CoordinateTableModel model = getCoordinateTableModel(table);

        String text;
        Color bgColor = Color.WHITE;
        if(column == 0) // the name column
            text = row == 0 ? tr("latitude") : tr("longitude");
        else // the value column
        {
            text = (String) model.getValueAt(row, column);
            if (!model.hasSameValueAsOpposite(row))
                bgColor = BGCOLOR_DIFFERENCE;
        }
        setText(text);
        setToolTipText(text);
        setBackground(isSelected ? BGCOLOR_SELECTED : bgColor);

        return this;
    }

    protected HistoryBrowserModel.CoordinateTableModel getCoordinateTableModel(JTable table) {
        return (HistoryBrowserModel.CoordinateTableModel) table.getModel();
    }
}
