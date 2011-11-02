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

public class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {

    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);
    public final static Color BGCOLOR_DELETED = new Color(255,197,197);
    public final static Color BGCOLOR_INSERTED = new Color(0xDD, 0xFF, 0xDD);
    public final static Color BGCOLOR_CHANGED = new Color(255,234,213);
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    private ImageIcon nodeIcon;

    public NodeListTableCellRenderer(){
        setOpaque(true);
        nodeIcon = ImageProvider.get("data", "node");
        setIcon(nodeIcon);
    }

    protected void renderNode(TwoColumnDiff.Item item, boolean isSelected) {
        String text = "";
        Color bgColor = Color.WHITE;
        setIcon(nodeIcon);
        if (item.value != null) {
            text = tr("Node {0}", item.value.toString());
        }
        switch(item.state) {
        case TwoColumnDiff.Item.EMPTY:
            text = "";
            bgColor = BGCOLOR_EMPTY_ROW;
            setIcon(null);
            break;
        case TwoColumnDiff.Item.CHANGED:
            bgColor = BGCOLOR_CHANGED;
            break;
        case TwoColumnDiff.Item.INSERTED:
            bgColor = BGCOLOR_INSERTED;
            break;
        case TwoColumnDiff.Item.DELETED:
            bgColor = BGCOLOR_DELETED;
            break;
        default:
            bgColor = BGCOLOR_EMPTY_ROW;
        }
        if (isSelected) {
            bgColor = BGCOLOR_SELECTED;
        }
        setText(text);
        setBackground(bgColor);
    }

    // Warning: The model pads with null-rows to match the size of the opposite table. 'value' could be null
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        renderNode((TwoColumnDiff.Item)value, isSelected);
        return this;
    }
}