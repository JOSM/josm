// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Renderer that renders the objects from an OsmPrimitive as data.
 *
 * Can be used in lists and tables.
 *
 * @author imi
 * @author Frederik Ramm
 */
public class OsmPrimitivRenderer implements ListCellRenderer, TableCellRenderer {
    private DefaultNameFormatter formatter = DefaultNameFormatter.getInstance();

    /**
     * Default list cell renderer - delegate for ListCellRenderer operation
     */
    private DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();

    /**
     * Default table cell renderer - delegate for TableCellRenderer operation
     */
    private DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();

    /**
     * Adapter method supporting the ListCellRenderer interface.
     */
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component def = defaultListCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        return renderer(def, (OsmPrimitive) value);
    }

    /**
     * Adapter method supporting the TableCellRenderer interface.
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component def = defaultTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof OsmPrimitive)
            return renderer(def, (OsmPrimitive) value);
        else if (value instanceof HistoryOsmPrimitive)
            return renderer(def, (HistoryOsmPrimitive) value);
        else
            return def;
    }

    /**
     * Internal method that stuffs information into the rendering component
     * provided that it's a kind of JLabel.
     * @param def the rendering component
     * @param value the OsmPrimtive to render
     * @return the modified rendering component
     */
    private Component renderer(Component def, OsmPrimitive value) {
        if (value != null && def instanceof JLabel) {
            ((JLabel)def).setText(getComponentText(value));
            ImageIcon icon = ImageProvider.get(value.getDisplayType());
            if (icon != null) {
                ((JLabel)def).setIcon(icon);
            } else {
                Main.warn("Null icon for "+value.getDisplayType());
            }
            ((JLabel)def).setToolTipText(getComponentToolTipText(value));
        }
        return def;
    }

    /**
     * Internal method that stuffs information into the rendering component
     * provided that it's a kind of JLabel.
     * @param def the rendering component
     * @param value the HistoryOsmPrimtive to render
     * @return the modified rendering component
     */
    private Component renderer(Component def, HistoryOsmPrimitive value) {
        if (value != null && def instanceof JLabel) {
            ((JLabel)def).setText(value.getDisplayName(DefaultNameFormatter.getInstance()));
            ((JLabel)def).setIcon(ImageProvider.get(value.getType()));
            ((JLabel)def).setToolTipText(formatter.buildDefaultToolTip(value));
        }
        return def;
    }

    /**
     * Can be overridden to customize the Text
     */
    protected String getComponentText(OsmPrimitive value) {
        return value.getDisplayName(DefaultNameFormatter.getInstance());
    }

    /**
     * Can be overridden to customize the ToolTipText
     */
    protected String getComponentToolTipText(OsmPrimitive value) {
        return formatter.buildDefaultToolTip(value);
    }
}
