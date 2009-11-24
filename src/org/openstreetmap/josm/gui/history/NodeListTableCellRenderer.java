// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The {@see TableCellRenderer} for a list of nodes in [@see HistoryBrower}
 *
 *
 */
public class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {

    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);
    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255,197,197);
    public final static Color BGCOLOR_IN_OPPOSITE = new Color(255,234,213);
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    private ImageIcon nodeIcon;

    public NodeListTableCellRenderer(){
        setOpaque(true);
        nodeIcon = ImageProvider.get("data", "node");
        setIcon(nodeIcon);
    }

    protected void renderNode(HistoryBrowserModel.NodeListTableModel model, Long nodeId, int row, boolean isSelected) {
        String text = "";
        Color bgColor = Color.WHITE;
        if (nodeId == null) {
            text = "";
            bgColor = BGCOLOR_EMPTY_ROW;
            setIcon(null);
        } else {
            text = tr("Node {0}", nodeId.toString());
            setIcon(nodeIcon);
            if (model.isSameInOppositeWay(row)) {
                bgColor = Color.WHITE;
            } else if (model.isInOppositeWay(row)) {
                bgColor = BGCOLOR_IN_OPPOSITE;
            } else {
                bgColor = BGCOLOR_NOT_IN_OPPOSITE;
            }
        }
        if (isSelected) {
            bgColor = BGCOLOR_SELECTED;
        }
        setText(text);
        setBackground(bgColor);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        HistoryBrowserModel.NodeListTableModel model = getNodeListTableModel(table);
        Long nodeId = (Long)value;
        renderNode(model, nodeId, row, isSelected);
        return this;
    }

    protected HistoryBrowserModel.NodeListTableModel getNodeListTableModel(JTable table) {
        return (HistoryBrowserModel.NodeListTableModel) table.getModel();
    }
}
