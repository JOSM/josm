// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@see TableCellRenderer} used in the node tables of {@see NodeListMerger}.
 * 
 */
public  class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {
    private static DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000");
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);

    private ImageIcon icon = null;
    private Border rowNumberBorder = null;

    /**
     * constructor
     */
    public NodeListTableCellRenderer() {
        icon = ImageProvider.get("data", "node");
        rowNumberBorder = BorderFactory.createEmptyBorder(0,4,0,0);
        setOpaque(true);
    }

    /**
     * creates the display name for a node. The name is derived from the nodes id,
     * its name (i.e. the value of the tag with key name) and its coordinates.
     * 
     * @param node  the node
     * @return the display name
     */
    protected String getDisplayName(Node node) {
        StringBuilder sb = new StringBuilder();
        if (node.get("name") != null) {
            sb.append(node.get("name"));
            sb.append("/");
            sb.append(node.id);
        } else {
            sb.append(node.id);
        }
        sb.append(" (");

        if (node.getCoor() != null) {
            sb.append(COORD_FORMATTER.format(node.getCoor().lat()));
            sb.append(",");
            sb.append(COORD_FORMATTER.format(node.getCoor().lon()));
        } else {
            sb.append("?,?");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * reset the renderer
     */
    protected void reset() {
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }

    /**
     * render a node
     * @param node the node
     * @param isSelected
     */
    protected  void renderNode(Node node, boolean isSelected) {
        setIcon(icon);
        setBorder(null);
        if (isSelected) {
            setBackground(BGCOLOR_SELECTED);
        }
        setText(getDisplayName(node));
    }

    protected void renderEmptyRow() {
        setIcon(null);
        setBackground(BGCOLOR_EMPTY_ROW);
        setText("");
    }

    /**
     * render the row id
     * @param row the row index
     * @param isSelected
     */
    protected  void renderRowId(int row, boolean isSelected) {
        setIcon(null);
        setBorder(rowNumberBorder);
        if (isSelected) {
            setBackground(BGCOLOR_SELECTED);
        }
        setText(Integer.toString(row+1));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        Node node = (Node)value;
        reset();
        switch(column) {
        case 0:
            renderRowId(row, isSelected);
            break;
        case 1:
            if (node == null) {
                renderEmptyRow();
            } else {
                renderNode(node,isSelected);
            }
            break;
        default:
            // should not happen
            throw new RuntimeException(tr("unexpected column index. Got {0}", column));
        }
        return this;
    }
}
