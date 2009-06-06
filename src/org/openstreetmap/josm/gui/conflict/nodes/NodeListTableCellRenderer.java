// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.net.URL;
import java.text.DecimalFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Node;

/**
 * This is the {@see TableCellRenderer} used in the node tables of {@see NodeListMerger}.
 * 
 *
 */
public  class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {
    private static DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000");
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    /**
     * Load the image icon for an OSM primitive of type node
     * 
     * @return the icon; null, if not found
     */
    protected ImageIcon loadIcon() {
        URL url = this.getClass().getResource("/images/data/node.png");;
        if (url == null) {
            System.out.println(tr("Failed to load resource /images/data/node.png"));
            return null;
        }
        return new ImageIcon(url);
    }

    /**
     * constructor
     */
    public NodeListTableCellRenderer() {
        setIcon(loadIcon());
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
        if (isSelected) {
            setBackground(BGCOLOR_SELECTED);
        }
        setText(getDisplayName(node));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        Node node = (Node)value;
        reset();
        renderNode(node,isSelected);
        return this;
    }
}
